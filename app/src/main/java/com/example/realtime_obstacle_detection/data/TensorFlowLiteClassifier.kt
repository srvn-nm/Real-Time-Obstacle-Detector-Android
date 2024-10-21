package com.example.realtime_obstacle_detection.data

import android.content.Context
import android.graphics.Bitmap
import android.view.Surface
import com.example.realtime_obstacle_detection.domain.ClassificationResults
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.domain.ObstacleClassifier
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class TensorFlowLiteClassifier(
    private val context: Context,
    private val conf: Float = 0.5f,
    private val count: Int = 1,
    private val threads: Int = 2,
) : ObstacleClassifier {

    private val labelPath = "labels.text"


    //labels and models
    private var labels = mutableListOf<String>()
    private var classifier: ImageClassifier? = null

    companion object {
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.25F
        private const val IOU_THRESHOLD = 0.4F
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

    private fun setupClassifier() {

        //basic options like threads counts or gpu usage
        val baseOptions = BaseOptions.builder()
            .setNumThreads(threads)
            .useGpu()
            .useNnapi()
            .build()

        //other options of an image classifier like threads setMaxResults or setScoreThreshold (we have to set baseOptions here too)
        val options = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(count)
            .setScoreThreshold(conf)
            .build()

        val modelPath = "best_float32.tflite"
        try {

            val model = FileUtil.loadMappedFile(context, modelPath)

            classifier = ImageClassifier.createFromFileAndOptions(
                context,
                modelPath,
                options
            )
        } catch (error: IllegalStateException) {
            error.printStackTrace()
        }
    }

     fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation {
        return when(rotation) {
            Surface.ROTATION_270 -> ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_90 -> ImageProcessingOptions.Orientation.TOP_LEFT
            Surface.ROTATION_180 -> ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            else -> ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }

     fun classify(bitmap: Bitmap, rotation: Int): List<ClassificationResults> {

        //initialization of classifier
        if(classifier == null) {
            setupClassifier()
        }

        //we need to process our bitmap as TensorImage for preparation to model input
        val imageProcessor = ImageProcessor.Builder().build()
        val image = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setOrientation(getOrientationFromRotation(rotation))
            .build()

        val results = classifier?.classify(image, imageProcessingOptions)

        //now we have to map our return type to our returned data class
        return results?.flatMap { classifications ->
            classifications.categories.map { category ->
                ClassificationResults(
                    label = category.displayName,
                    score = category.score
                )
            }
        }?.distinctBy { it.label } ?: emptyList() //no result for picture
    }

    override fun onEmptyDetect() {
        TODO("Not yet implemented")
    }

    override fun onDetect(
        objectDetectionResults: List<ObjectDetectionResult>,
        detectedScene: Bitmap
    ) {
        TODO("Not yet implemented")
    }

}