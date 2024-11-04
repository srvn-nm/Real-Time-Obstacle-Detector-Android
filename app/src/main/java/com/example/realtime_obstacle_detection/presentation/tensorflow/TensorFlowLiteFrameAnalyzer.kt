package com.example.realtime_obstacle_detection.presentation.tensorflow

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.Surface
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.MatrixExt.postRotate
import com.example.realtime_obstacle_detection.data.ObstacleDetector

class TensorFlowLiteFrameAnalyzer (
    private val obstacleDetector: ObstacleDetector,
): ImageAnalysis.Analyzer {

    private fun getOrientationFromRotation(rotation: Int): Int {
        return when(rotation) {
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            else -> 0
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun analyze(image: ImageProxy) {

            val startTime = System.currentTimeMillis()

//            val orientation = getOrientationFromRotation(rotation = image.imageInfo.rotationDegrees)
            val bitmap = image.toBitmap()
//            val rotatedBitmap = rotateBitmap(bitmap, orientation)

            val endTime = System.currentTimeMillis()

            val duration = (endTime - startTime) / 1000.0

            Log.d("processing time", "Preprocessing and rotation took $duration seconds")

            obstacleDetector.detect(image = bitmap)

            image.close()
    }
}