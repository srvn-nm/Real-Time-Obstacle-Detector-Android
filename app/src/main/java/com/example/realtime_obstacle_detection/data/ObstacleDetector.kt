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
import com.example.realtime_obstacle_detection.utis.camera.getCameraSensorInfo
import com.example.realtime_obstacle_detection.utis.distance.calculateDistance
import com.example.realtime_obstacle_detection.utis.camera.getFocalLength
import androidx.core.graphics.scale

/**
 * ObstacleDetector is responsible for performing object detection using a pre-trained YOLO model.
 * It handles model loading, preprocessing input images, running inference, and post-processing
 * detection results including confidence filtering, bounding box extraction, distance calculation,
 * and non-maximum suppression(NMS).
 *
 * @param context The Android context used for accessing assets and system information.
 * @param obstacleClassifier A callback interface to deliver detection results or handle empty results.
 * @param modelPath Path to the TFLite model file within the assets directory.
 * @param labelPath Path to the label file listing object class names.
 * @param threadsCount Number of threads used for inference.
 * @param useNNAPI Flag to enable NNAPI acceleration (if supported by edge device).
 * @param confidenceThreshold Minimum confidence score to keep a detection result.
 * @param iouThreshold Threshold for Intersection over Union (IoU) used in NMS to filter overlapping boxes.
 */
class ObstacleDetector(
    private val context: Context,
    private val obstacleClassifier: ObstacleClassifier,
    private val modelPath: String = "15Obstacles_yolov8_float32.tflite",
    private val labelPath: String = "15Obstacles_labels.txt",
    private val threadsCount: Int = 3,
    private val useNNAPI: Boolean = true,
    private val confidenceThreshold: Float = 0.35F,
    private val iouThreshold: Float = 0.3F
) {

    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()

    //    private var imageProcessor : ImageProcessor? = null
    private var focalLength: Float? = null
    private var sensorHeight: Float? = null

    private var tensorWidth = 0
    private var tensorHeight = 0
    private var channelsCount = 0
    private var elementsCount = 0

    // CRITICAL: Variable to store the required input type
    private var inputDataType: DataType = DataType.FLOAT32

    /**
     * Initializes the object detection model by:
     * - Reading camera parameters (focal length and sensor height),
     * - Creating an image preprocessing pipeline (normalization and casting),
     * - Configuring the TFLite interpreter with thread count and NNAPI,
     * - Loading the model and extracting tensor dimensions,
     * - Loading class labels from asset files.
     *
     * Note: This function should be called once before any detection is performed.
     */
    fun setup() {

        Log.d("model setup and configuration", "starting the process ...")

        // Camera parameters setup
        focalLength = getFocalLength(context)
        sensorHeight = getCameraSensorInfo(context)?.height
        Log.d(
            "model setup and configuration",
            "camera information: focalLength,sensorHeight -> $focalLength,$sensorHeight"
        )

        // Image processing pipeline
//        imageProcessor = ImageProcessor.Builder()
//            .add(NormalizeOp( 0f, 255f))
//            .add(CastOp(DataType.FLOAT32))
//            .build()

        Log.d("model setup and configuration", "image processor ...")


        // Interpreter configuration
        val options = Interpreter.Options()

        options.numThreads = threadsCount
        options.useNNAPI = useNNAPI

        Log.d("model setup and configuration", "GPU, threadsCount and NNAPI CONFIGS are added")

        // Model initialization
        val model = FileUtil.loadMappedFile(context, modelPath)

        interpreter = Interpreter(model, options)

        // Tensor dimensions
        val inputTensor = interpreter?.getInputTensor(0) ?: return
        val inputShape = interpreter?.getInputTensor(0)?.shape() ?: return
        val outputShape = interpreter?.getOutputTensor(0)?.shape() ?: return

        tensorWidth = inputShape[1]
        tensorHeight = inputShape[2]
        channelsCount = outputShape[1]
        elementsCount = outputShape[2]

        // CRITICAL: Extract and store the model's required input data type
        inputDataType = inputTensor.dataType()
        Log.d("model setup and configuration", "interpreter and its configurations ...")

        try {
            labelReader()
            Log.d("model setup and configuration", "labels are added")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Loads class labels from the label file in the assets folder.
     * Each line in the file represents a class name used in the model output.
     * Populates the `labels` list used for assigning names to detection results.
     */
    private fun labelReader() {
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

    /**
     * Runs object detection on the given bitmap image.
     * Steps:
     * - Resizes and preprocesses the input image,
     * - Runs the image through the model using the interpreter,
     * - Extracts the best bounding boxes using the model output,
     * - Passes the result to the `ObstacleClassifier` callback.
     *
     * Logs processing time for each phase for performance tracking.
     *
     * @param image The bitmap image to analyze.
     */

    fun detect(image: Bitmap) {

        var startTime = System.currentTimeMillis()

        interpreter ?: return

        if (tensorWidth == 0 || tensorHeight == 0 || channelsCount == 0 || elementsCount == 0) return

        // 1. Resize the input Bitmap
        //we should preprocess the bitmap before detection
        val resizedBitmap = image.scale(tensorWidth, tensorHeight, false)


        // 2. Prepare TensorImage using the model's required input type (FLOAT32 or UINT8/INT8)
        val tensorImage = TensorImage(inputDataType)
        tensorImage.load(resizedBitmap)

//        val processedImage = imageProcessor!!
//            .process(tensorImage)
//        val imageBuffer = processedImage.buffer
        val processorBuilder = ImageProcessor.Builder()

        when (inputDataType) {
            DataType.FLOAT32 -> {
                Log.d("ObstacleDetector", "Processing as FLOAT32 (Normalized).")
                processorBuilder
                    .add(NormalizeOp(0f, 255f)) // Normalize 0-255 to 0.0-1.0
                    .add(CastOp(DataType.FLOAT32))
            }
            DataType.UINT8, DataType.INT8 -> {
                Log.d("ObstacleDetector", "Processing as 8-bit (No Normalization).")
                // CRITICAL FIX: Explicitly cast to the target integer type.
                // This forces the TFLite library to structure the final ByteBuffer correctly.
                processorBuilder.add(CastOp(inputDataType))
            }
            else -> {
                Log.e("ObstacleDetector", "Unsupported TFLite input data type: $inputDataType. Cannot run inference.")
                return
            }
        }

        val imageProcessor = processorBuilder.build()
        val imageBuffer = imageProcessor.process(tensorImage).buffer


        val output = TensorBuffer.createFixedSize(
            intArrayOf(1, channelsCount, elementsCount),
            DataType.FLOAT32
        )

        var endTime = System.currentTimeMillis()

        var duration = (endTime - startTime) / 1000.0

        Log.d(
            "processing time",
            "preparation of imageprocessor and fundamental variables took $duration seconds"
        )

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
        Log.d(
            "processing time",
            "bounding box and distance calculation and saving operations took $duration seconds"
        )

    }

    /**
     * Processes raw model output into a list of high-confidence detection results.
     * Steps:
     * - Iterates over each predicted bounding box and finds the class with the highest confidence,
     * - Applies the confidence threshold filter,
     * - Converts center-based bounding boxes to corner-based format (x1, y1, x2, y2),
     * - Calculates the physical distance to the object based on camera parameters,
     * - Filters out invalid coordinates and returns the final list after applying NMS.
     *
     * @param array The flattened float array output of the TFLite model.
     * @return A list of `ObjectDetectionResult`, or null if no valid detections.
     */
    private fun bestBox(array: FloatArray): List<ObjectDetectionResult>? {

        val startTime = System.currentTimeMillis()
        val objectDetectionResults = mutableListOf<ObjectDetectionResult>()

        for (classIndex in 0 until elementsCount) {
            var maxConf = -1.0f
            var maxIdx = -1
            var j = 4
            var arrayIdx = classIndex + elementsCount * j
            while (j < channelsCount) {
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += elementsCount
            }

            if (maxConf > confidenceThreshold) {
                val clsName = labels[maxIdx]
                val cx = array[classIndex]
                val cy = array[classIndex + elementsCount]
                val width = array[classIndex + elementsCount * 2]
                val height = array[classIndex + elementsCount * 3]
                val x1 = cx - (width / 2F)
                val y1 = cy - (height / 2F)
                val x2 = cx + (width / 2F)
                val y2 = cy + (height / 2F)

                val distance = calculateDistance(
                    className = clsName,
                    objectHeightInPixels = height * tensorHeight,
                    focalLengthInMM = focalLength,
                    imageHeightInPixels = tensorHeight.toFloat(),
                    sensorHeightInMM = sensorHeight
                )

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
                        x1 = x1,
                        y1 = y1,
                        x2 = x2,
                        y2 = y2,
                        width = width,
                        height = height,
                        confidenceRate = maxConf,
                        className = clsName,
                        distance = distance
                    )
                )
            }
        }

        val endTime = System.currentTimeMillis()

        val duration = (endTime - startTime) / 1000.0
        Log.d(
            "processing time",
            "finding the bounding box and saving operations took $duration seconds"
        )

        if (objectDetectionResults.isEmpty())
            return null

        return applyNMS(objectDetectionResults)
    }

    /**
     * Applies Non-Maximum Suppression (NMS) to eliminate overlapping bounding boxes.
     * Retains only the highest-confidence box when multiple detections overlap significantly.
     *
     * @param boxes List of all raw detection results.
     * @return A filtered list with non-overlapping detection boxes.
     */
    private fun applyNMS(boxes: List<ObjectDetectionResult>): MutableList<ObjectDetectionResult> {

        val selectedBoxes = mutableListOf<ObjectDetectionResult>()

        val sortedBoxes = boxes.sortedByDescending {
            it.confidenceRate
        }.toMutableList()

        while (sortedBoxes.isNotEmpty()) {
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

    /**
     * Calculates the Intersection over Union (IoU) score between two bounding boxes.
     * Used to determine overlap for NMS.
     *
     * @param box1 First bounding box.
     * @param box2 Second bounding box.
     * @return IoU value between 0.0 and 1.0.
     */
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
