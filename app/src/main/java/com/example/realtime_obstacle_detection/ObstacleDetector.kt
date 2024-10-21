package com.example.realtime_obstacle_detection

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
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.domain.ObstacleClassifier

class ObstacleDetector(
    private val context: Context,
    private val obstacleClassifier: ObstacleClassifier,
    private val modelPath :String = "best_float32.tflite",
    private val labelPath :String = "labels.txt",
    private val threadsCount :Int = 4,
    private val useNNAPI : Boolean= true,
    private val confidenceThreshold : Float = 0.25F,
    private val iouThreshold : Float = 0.4F,


    ){

    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()
    private var imageProcessor : ImageProcessor? = null

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var numChannel = 0
    private var numElements = 0

    fun setup() {

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
        numChannel = outputShape[1]
        numElements = outputShape[2]

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

        interpreter ?: return
        imageProcessor?: return
        if (tensorWidth == 0 || tensorHeight == 0 || numChannel == 0 || numElements == 0) return

        //we should preprocess the bitmap before detection
        val resizedBitmap = Bitmap
            .createScaledBitmap(image, tensorWidth, tensorHeight, false)


        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)

        val processedImage = imageProcessor!!.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val output = TensorBuffer.createFixedSize(intArrayOf(1 , numChannel, numElements), DataType.FLOAT32)
        interpreter?.run(imageBuffer, output.buffer)

        val bestBoxes = bestBox(output.floatArray)
        if (bestBoxes == null) {
            obstacleClassifier.onEmptyDetect()
            return
        }

        obstacleClassifier.onDetect(
            objectDetectionResults = bestBoxes,
            detectedScene = image
        )
    }


    private fun bestBox(array: FloatArray) : List<ObjectDetectionResult>? {

        val objectDetectionResults = mutableListOf<ObjectDetectionResult>()

        for (classIndex in 0 until numElements) {
            var maxConf = -1.0f
            var maxIdx = -1
            var j = 4
            var arrayIdx = classIndex + numElements * j
            while (j < numChannel){
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            if (maxConf > confidenceThreshold) {
                val clsName = labels[maxIdx]
                val cx = array[classIndex] // 0
                val cy = array[classIndex + numElements] // 1
                val w = array[classIndex + numElements * 2]
                val h = array[classIndex + numElements * 3]
                val x1 = cx - (w/2F)
                val y1 = cy - (h/2F)
                val x2 = cx + (w/2F)
                val y2 = cy + (h/2F)
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
                        confidenceRate = maxConf, classIndex = maxIdx, className = clsName
                    )
                )
            }
        }

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
