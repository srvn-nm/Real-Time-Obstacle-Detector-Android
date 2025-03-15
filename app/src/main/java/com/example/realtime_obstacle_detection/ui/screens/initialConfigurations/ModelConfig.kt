package com.example.realtime_obstacle_detection.ui.screens.initialConfigurations

data class ModelConfig(
    val selectedModel: Models,
    val configThreshold: Float,
    val iouThreshold: Float,
    val threadCount: Int,
    val useNNAPI: Boolean
) {
    val labelFileName: String
        get() = selectedModel.labelFileName
}