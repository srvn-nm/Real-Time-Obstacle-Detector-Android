package com.example.realtime_obstacle_detection.domain

/**
 * Data class to represent the results of an object detection operation.
 * This class holds the bounding box coordinates, the centroid of the detected object,
 * the dimensions of the bounding box, the confidence rate of the detection, and the
 * class information of the detected object.
 *
 * @property x1 The x-coordinate of the top-left corner of the bounding box.
 * @property y1 The y-coordinate of the top-left corner of the bounding box.
 * @property x2 The x-coordinate of the bottom-right corner of the bounding box.
 * @property y2 The y-coordinate of the bottom-right corner of the bounding box.
 * @property width The width of the bounding box.
 * @property height The height of the bounding box.
 * @property confidenceRate The confidence rate of the detection, usually between 0.0 and 1.0, where a higher value indicates greater confidence in the detection.
 * @property className The name of the class to which the detected object is predicted to belong.
 * @property distance the distance of object from the camera
 */
data class ObjectDetectionResult(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val width: Float,
    val height: Float,
    val confidenceRate: Float,
    val className: String,
    val distance : Float?
)
