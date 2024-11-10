package com.example.realtime_obstacle_detection.utis.compose.boundingBox

// Real-world sizes in meters for each object type, converted to millimeters
private val REAL_WORLD_SIZES_MM = mapOf(
    "bicycle" to 600F,
    "bus" to 2500F,
    "car" to 1800F,
    "dog" to 500F,
    "electric pole" to 300F,
    "motorcycle" to 800F,
    "person" to 500F,
    "traffic sign" to 700F,
    "tree" to 2000F,
    "uncovered manhole" to 700F
)

/**
 * Calculates the distance to an object using the detailed camera and object metrics.
 *
 * @param className the name of the class of the detected object.
 * @param objectHeightInPixels the height of the object in the image, in pixels.
 * @param focalLengthInMM the focal length of the camera, in millimeters.
 * @param imageHeightInPixels the height of the image in pixels.
 * @param sensorHeightInMM the height of the camera sensor, in millimeters.
 * @return the calculated distance to the object in millimeters, or null if the class name is not recognized.
 */
fun calculateDistance(className: String, objectHeightInPixels: Float, focalLengthInMM: Float?, imageHeightInPixels: Float, sensorHeightInMM: Float?): Float? {
    val realHeightOfObjectInMM = REAL_WORLD_SIZES_MM[className.lowercase()] ?: return null

    // Implement the provided formula to calculate the distance to the object.
    if (focalLengthInMM != null &&  sensorHeightInMM != null)
       return (focalLengthInMM * realHeightOfObjectInMM * imageHeightInPixels) / (objectHeightInPixels * sensorHeightInMM)

    return null
}