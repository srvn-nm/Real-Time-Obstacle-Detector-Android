package com.example.realtime_obstacle_detection.data

import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.domain.ObstacleClassifier
import com.example.realtime_obstacle_detection.utis.compose.boundingBox.calculateDistance
import com.example.realtime_obstacle_detection.utis.camera.getFocalLength

class ObstacleDetector(
    private val context: Context,
    private val obstacleClassifier: ObstacleClassifier,
    private val modelPath :String = "obstacle_detector_float16.tflite",
    private val labelPath :String = "obstacle_detector_labels.txt",
    private val threadsCount :Int = 4,
    private val useNNAPI : Boolean= true,
    private val confidenceThreshold : Float = 0.5F,
    private val iouThreshold : Float = 0.4F
){

    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()
    private var imageProcessor : ImageProcessor? = null
    private var  focalLength : Float?= null

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var channelsCount = 0
    private var elementsCount = 0

    fun setup() {

        focalLength = getFocalLength(context)

        imageProcessor = ImageProcessor.Builder()
            .add(NormalizeOp( 0f, 255f))
            .add(CastOp(DataType.FLOAT32))
            .build()

        val options = Interpreter.Options()
        options.numThreads = threadsCount
        options.useNNAPI = useNNAPI

        val model = FileUtil.loadMappedFile(context, modelPath)

        interpreter = Interpreter(model, options)

        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return

        tensorWidth = inputShape[1]
        tensorHeight = inputShape[2]
        channelsCount = outputShape[1]
        elementsCount = outputShape[2]

        try {
            labelReader()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun labelReader(){
        val inputStream: InputStream = context.assets.open(labelPath)
        val reader = BufferedReader(InputStreamReader(inputStream))

        var line: String? = reader.readLine()
        while (line != null && line != "") {
            labels.add(line)
            line = reader.readLine()
        }

        reader.close()
        inputStream.close()
    }

    fun detect(image: Bitmap){

        var startTime = System.currentTimeMillis()

        interpreter ?: return

        if (tensorWidth == 0 || tensorHeight == 0 || channelsCount == 0 || elementsCount == 0) return

        //we should preprocess the bitmap before detection
        val resizedBitmap = Bitmap
            .createScaledBitmap(image, tensorWidth, tensorHeight, false)


        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)

        val processedImage = imageProcessor!!
            .process(tensorImage)
        val imageBuffer = processedImage.buffer


        val output = TensorBuffer.createFixedSize(intArrayOf(1 , channelsCount, elementsCount), DataType.FLOAT32)

        var endTime = System.currentTimeMillis()

        var duration = (endTime - startTime) / 1000.0

        Log.d("processing time", "preparation of imageprocessor and fundamental variables took $duration seconds")

        startTime = System.currentTimeMillis()
        interpreter?.run(imageBuffer, output.buffer)
        endTime = System.currentTimeMillis()

        duration = (endTime - startTime) / 1000.0
        Log.d("model inference time", "model($modelPath) inference took $duration seconds")

        startTime = System.currentTimeMillis()
        val bestBoxes = bestBox(output.floatArray)
        if (bestBoxes == null) {
            obstacleClassifier.onEmptyDetect()
            return
        }

        obstacleClassifier.onDetect(
            objectDetectionResults = bestBoxes,
            detectedScene = image
        )

        endTime = System.currentTimeMillis()

        duration = (endTime - startTime) / 1000.0
        Log.d("processing time", "bounding box and distance calculation and saving operations took $duration seconds")

    }

    private fun bestBox(array: FloatArray) : List<ObjectDetectionResult>? {

        var startTime = System.currentTimeMillis()
        val objectDetectionResults = mutableListOf<ObjectDetectionResult>()

        for (classIndex in 0 until elementsCount) {
            var maxConf = -1.0f
            var maxIdx = -1
            var j = 4
            var arrayIdx = classIndex + elementsCount * j
            while (j < channelsCount){
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += elementsCount
            }

            if (maxConf > confidenceThreshold) {
                val clsName = labels[maxIdx]
                val cx = array[classIndex] // 0
                val cy = array[classIndex + elementsCount] // 1
                val w = array[classIndex + elementsCount * 2]
                val h = array[classIndex + elementsCount * 3]
                val x1 = cx - (w/2F)
                val y1 = cy - (w/2F)
                val x2 = cx + (w/2F)
                val y2 = cy + (h/2F)



                val distance = calculateDistance(className = clsName ,  width = w,focalLength = focalLength!!)

                if (x1 < 0F || x1 > 1F)
                    continue
                if (y1 < 0F || y1 > 1F)
                    continue
                if (x2 < 0F || x2 > 1F)
                    continue
                if (y2 < 0F || y2 > 1F)
                    continue

                objectDetectionResults.add(
                    ObjectDetectionResult(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2, cx = cx, cy = cy, width = w, height = h,
                        confidenceRate = maxConf, classIndex = maxIdx, className = clsName, distance = distance
                    )
                )
            }
        }

        var endTime = System.currentTimeMillis()

        var duration = (endTime - startTime) / 1000.0
        Log.d("processing time", "finding the bounding box and saving operations took $duration seconds")

        if (objectDetectionResults.isEmpty())
            return null

        return applyNMS(objectDetectionResults)
    }

    private fun applyNMS(boxes: List<ObjectDetectionResult>) : MutableList<ObjectDetectionResult> {

        val selectedBoxes = mutableListOf<ObjectDetectionResult>()

        val sortedBoxes = boxes.sortedByDescending {
            it.confidenceRate
        }.toMutableList()

        while(sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= iouThreshold) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: ObjectDetectionResult, box2: ObjectDetectionResult): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.width * box1.height
        val box2Area = box2.width * box2.height
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

}
