package com.example.realtime_obstacle_detection.ui.screens.initialConfigurations

enum class Models(
    val displayName: String,
    val modelFileName: String,
    val labelFileName: String
) {
    OBSTACLE_16BIT(
        "14 Obstacle 16-bit",
        "14_obstacle_16bit.tflite",
        "14_obstacles_labels.txt"
    ),
    OBSTACLE_32BIT(
        "14 Obstacle 32-bit",
        "14_obstacle_32bit.tflite",
        "14_obstacles_labels.txt"
    ),
    OBSTACLE_BEST_INT8(
        "14 Obstacle Best INT8",
        "14_obstacle_best_int8.tflite",
        "14_obstacles_labels.txt"
    ),
    OBJECT_16BIT(
        "20 Object 16-bit",
        "20_object_16bit.tflite",
        "20_object_labels.txt"
    ),
    OBJECT_32BIT(
        "20 Object 32-bit",
        "20_object_32bit.tflite",
        "20_object_labels.txt"
    ),
    BEST_FLOAT16(
        "Best Float16",
        "best_float16.tflite",
        "best_labels.txt"
    ),
    BEST_FLOAT32(
        "Best Float32",
        "best_float32.tflite",
        "best_labels.txt"
    ),
    BEST_FULL_INTEGER(
        "Best Full Integer Quant",
        "best_full_integer_quant.tflite",
        "best_labels.txt"
    ),
    BEST_INT8(
        "Best INT8",
        "best_int8.tflite",
        "best_labels.txt"
    ),
    BEST_INTEGER_QUANT(
        "Best Integer Quant",
        "best_integer_quant.tflite",
        "best_labels.txt"
    ),
    OBSTACLE_DETECTOR_FLOAT16(
        "Obstacle Detector Float16",
        "obstacle_detector_float16.tflite",
        "obstacle_detector_labels.txt"
    ),
    OBSTACLE_DETECTOR_FLOAT32(
        "Obstacle Detector Float32",
        "obstacle_detector_float32.tflite",
        "obstacle_detector_labels.txt"
    ),
    YOLOV8_FLOAT32(
        "YOLOv8 Float32",
        "yoloV8_float32.tflite",
        "yoloV8_labels.txt"
    );

    companion object {
        val DEFAULT = OBSTACLE_32BIT
    }
}
