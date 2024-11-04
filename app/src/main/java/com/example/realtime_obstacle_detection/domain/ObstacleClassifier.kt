package com.example.realtime_obstacle_detection.domain

import android.graphics.Bitmap
import android.view.Surface
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions

interface ObstacleClassifier {

    fun onEmptyDetect()

    /**
     * Classifies the given bitmap image to identify obstacle.
     *
     * @param detectedScene The bitmap image to be processed from the camera view.
     * @param objectDetectionResults A list of classification results, each containing a label and a confidence score.
     */
    fun onDetect(objectDetectionResults: List<ObjectDetectionResult>, detectedScene: Bitmap)

}