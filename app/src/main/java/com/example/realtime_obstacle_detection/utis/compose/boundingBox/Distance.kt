package com.example.realtime_obstacle_detection.utis.compose.boundingBox


// Real-world sizes in meters for each object type
private val REAL_WORLD_SIZES = mapOf(
    "bicycle" to 0.6F,
    "bus" to 2.5F,
    "car" to 1.8F,
    "dog" to 0.5F,
    "electric pole" to 0.3F,
    "motorcycle" to 0.8F,
    "person" to 0.5F,
    "traffic sign" to 0.7F,
    "tree" to 1.5F,
    "uncovered manhole" to 0.7F
)


// Add distance calculation within ObjectDetectionResult
fun calculateDistance(className :String, width : Float, focalLength: Float): Float? {
    val realWorldSize = REAL_WORLD_SIZES[className.lowercase()] ?: return null
    // Calculate distance based on bounding box width (could also use height if needed)
    return (realWorldSize * focalLength) / width
}
