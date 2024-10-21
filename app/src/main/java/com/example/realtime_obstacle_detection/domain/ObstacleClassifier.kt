package com.example.realtime_obstacle_detection.domain

import android.graphics.Bitmap

interface ObstacleClassifier {

    fun onEmptyDetect()

    /**
     * Classifies the given bitmap image to identify obstacle.
     *
     * @param detectedScene The bitmap image to be processed from the camera view.
     * @param objectDetectionResults A list of classification results, each containing a label and a confidence score.
     */
    fun onDetect(objectDetectionResults: List<ObjectDetectionResult>, detectedScene: Bitmap)

    //fun getOrientationFromRotation(rotation: Int): ImageProcessingOptions.Orientation
}