package com.example.realtime_obstacle_detection.presentation.tensorflow

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.realtime_obstacle_detection.data.ObstacleDetector

class TensorFlowLiteFrameAnalyzer (
    private val obstacleDetector: ObstacleDetector,
    private val onFpsCalculated: ((Int) -> Unit)? = null,
    private val onInferenceTime: ((Long) -> Unit)? = null
): ImageAnalysis.Analyzer {

    private var frameSkipCounter = 0
    private var lastTimestamp = 0L
    private var frameCount = 0

    override fun analyze(image: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (lastTimestamp == 0L) {
            lastTimestamp = currentTimestamp
        }
        frameCount++
        // Update FPS every second.
        if (currentTimestamp - lastTimestamp >= 1000) {
            val fps = frameCount
            onFpsCalculated?.invoke(fps)
            Log.d("FPS", "FPS: $fps")
            frameCount = 0
            lastTimestamp = currentTimestamp
        }

        if(frameSkipCounter % 6 == 0) {
            val rotationDegrees = image.imageInfo.rotationDegrees

            val startTime = System.currentTimeMillis()
            val bitmap = image.toBitmap()

            val endTime = System.currentTimeMillis()

            val duration = (endTime - startTime) / 1000.0

            Log.d("processing time", "Preprocessing and rotation took $duration seconds")

            // 1) Measure inference:
            val t0 = System.currentTimeMillis()
            obstacleDetector.detect(bitmap)
            val inferenceMs = System.currentTimeMillis() - t0
            onInferenceTime?.invoke(inferenceMs)

        }
        frameSkipCounter++

        image.close()

    }
}