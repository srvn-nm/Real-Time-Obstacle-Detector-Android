package com.example.realtime_obstacle_detection.domain

/** Data class to represent the results of a classification operation.
 *
 */
data class ObjectDetectionResult(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val cx: Float,
    val cy: Float,
    val width: Float,
    val height: Float,
    val confidenceRate: Float,
    val classIndex: Int,
    val className: String
)
