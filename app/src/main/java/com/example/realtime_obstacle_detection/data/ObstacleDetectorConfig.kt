package com.example.realtime_obstacle_detection.data

import android.content.Context

/*** This class will save all of our essential Models interpreter configs in settings
 *
 */
class ObstacleDetectorConfig(
    var modelPath: String = "15Obstacles_yolov8_float32.tflite",
    var labelPath: String = "15Obstacles_labels.txt",
    var threadsCount: Int = 3,
    var useNNAPI: Boolean = true,
    var confidenceThreshold: Float = 0.35F,
    var iouThreshold: Float = 0.3F
) {
    companion object {
        private const val PREFERENCES_NAME = "ObstacleDetectorPrefs"

        fun load(context: Context): ObstacleDetectorConfig {
            val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            return ObstacleDetectorConfig(
                modelPath = prefs.getString("modelPath", "15Obstacles_yolov8_float32.tflite")!!,
                labelPath = prefs.getString("labelPath", "15Obstacles_labels.txt")!!,
                threadsCount = prefs.getInt("threadsCount", 3),
                useNNAPI = prefs.getBoolean("useNNAPI", true),
                confidenceThreshold = prefs.getFloat("confidenceThreshold", 0.35F),
                iouThreshold = prefs.getFloat("iouThreshold", 0.3F)
            )
        }

        fun save(context: Context, config: ObstacleDetectorConfig) {
            val prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("modelPath", config.modelPath)
                putString("labelPath", config.labelPath)
                putInt("threadsCount", config.threadsCount)
                putBoolean("useNNAPI", config.useNNAPI)
                putFloat("confidenceThreshold", config.confidenceThreshold)
                putFloat("iouThreshold", config.iouThreshold)
                apply()
            }
        }
    }
}