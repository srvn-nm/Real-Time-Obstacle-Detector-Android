package com.example.realtime_obstacle_detection.presentation.tensorflow

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.realtime_obstacle_detection.data.ObstacleDetector

class TensorFlowLiteFrameAnalyzer (
    private val obstacleDetector: ObstacleDetector,
//    private var screenImage : Bitmap?
): ImageAnalysis.Analyzer {

    private var frameSkipCounter = 0

    override fun analyze(image: ImageProxy) {
        if(frameSkipCounter % 5 == 0) {
            val bitmap = image
                .toBitmap()
            obstacleDetector.detect(image = bitmap)
        }
        frameSkipCounter++

        image.close()
    }
}
