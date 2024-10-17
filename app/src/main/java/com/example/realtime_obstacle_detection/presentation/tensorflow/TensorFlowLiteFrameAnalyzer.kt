package com.example.realtime_obstacle_detection.presentation.tensorflow

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.realtime_obstacle_detection.ObstacleDetector

class TensorFlowLiteFrameAnalyzer (
    private val obstacleDetector: ObstacleDetector,
    //private var bitmapImage : Bitmap
): ImageAnalysis.Analyzer {

    private var frameSkipCounter = 0

    override fun analyze(image: ImageProxy) {
        if(frameSkipCounter % 2 == 0) {
            val bitmap = image
                .toBitmap()
            obstacleDetector.detect(image = bitmap)
        }
        frameSkipCounter++

        image.close()
    }
}
