package com.example.realtime_obstacle_detection.presentation.tensorflow

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.realtime_obstacle_detection.data.ObstacleDetector

class TensorFlowLiteFrameAnalyzer (
    private val obstacleDetector: ObstacleDetector,
): ImageAnalysis.Analyzer {

    private var frameSkipCounter = 0

    override fun analyze(image: ImageProxy) {

        if(frameSkipCounter % 6 == 0) {
            val rotationDegrees = image.imageInfo.rotationDegrees

            val startTime = System.currentTimeMillis()
            val bitmap = image.toBitmap()

            val endTime = System.currentTimeMillis()

            val duration = (endTime - startTime) / 1000.0

            Log.d("processing time", "Preprocessing and rotation took $duration seconds")

            obstacleDetector.detect(image = bitmap)

        }
        frameSkipCounter++

        image.close()

    }
}