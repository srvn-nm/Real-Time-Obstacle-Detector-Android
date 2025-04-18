package com.example.realtime_obstacle_detection.ui.screens.initialConfigurations

/**
 * Enum class representing different versions of YOLO object detection models
 * used within the application.
 *
 * Each entry corresponds to a unique model variant with the following properties:
 * @param displayName A user-friendly name for displaying in the UI or logs.
 * @param modelFileName The name of the TFLite model file used for inference.
 * @param labelFileName The associated label file containing object class names.
 *
 * Groupings:
 * - YOLOv8 / YOLOv12: Refers to different versions of the YOLO model architecture.
 * - 15 vs. 18 Obstacles: Refers to the number of object classes in the detection dataset.
 * - FP16, FP32, INT8, Integer Quant: Represents different quantization and precision formats
 *   to balance model size, speed, and accuracy.
 *
 * The `DEFAULT` model is set to `YOLO8_18_OBSTACLE_FP32BIT`, but can be customized as needed.
 */
enum class Models(
    val displayName: String,
    val modelFileName: String,
    val labelFileName: String
) {
    //YoloV8 - 18 Obstacles ------------------------------------------------------------------------
    YOLO8_18_OBSTACLE_FP16BIT(
        "18 Obstacles YOLOv FP16",
        "18Obstacles_yolov8_float16.tflite",
        "18Obstacles_labels.txt"
    ),
    YOLO8_18_OBSTACLE_FP32BIT(
        "18 Obstacles YOLOv FP32",
        "18Obstacles_yolov8_float32.tflite",
        "18Obstacles_labels.txt"
    ),
    YOLO8_18_OBSTACLE_FULL_INTEGER(
        "18 Obstacles YOLOv8 Full Integer Quant",
        "18Obstacles_yolov8_full_integer_quant.tflite",
        "18Obstacles_labels.txt"
    ),
    YOLO8_18_OBSTACLE_INT8(
        "18 Obstacles YOLOv8 INT8",
        "18Obstacles_yolov8_int8.tflite",
        "18Obstacles_labels.txt"
    ),
    YOLO8_18_OBSTACLE_INTEGER_QUANT(
        "18 Obstacles YOLOv8 Integer Quant",
        "18Obstacles_yolov8_integer_quant.tflite",
        "18Obstacles_labels.txt"
    ),

    //YoloV12 - 18 Obstacles -----------------------------------------------------------------------
    YOLO12_18_OBSTACLE_FP16BIT(
        "18 Obstacles YOLOv12 FP16",
        "18Obstacles_yolov12_float16.tflite",
        "18Obstacles_labels.txt"
    ),
    YOLO12_18_OBSTACLE_FP32BIT(
        "18 Obstacles YOLOv12 FP32",
        "18Obstacles_yolov12_float32.tflite",
        "18Obstacles_labels.txt"
    ),
    
    //15 Obstacles - YoloV8  -----------------------------------------------------------------------
    
    BEST_FLOAT16(
        "Best Float16",
        "15Obstacles_yolov8_float16.tflite",
        "15Obstacles_labels.txt"
    ),
    BEST_FLOAT32(
        "Best Float32",
        "15Obstacles_yolov8_float32.tflite",
        "15Obstacles_labels.txt"
    ),
    BEST_FULL_INTEGER(
        "Best Full Integer Quant",
        "15Obstacles_yolov8_full_integer_quant.tflite",
        "15Obstacles_labels.txt"
    ),
    BEST_INT8(
        "Best INT8",
        "15Obstacles_yolov8_int8.tflite",
        "15Obstacles_labels.txt"
    ),
    BEST_INTEGER_QUANT(
        "Best Integer Quant",
        "15Obstacles_yolov8_integer_quant.tflite",
        "15Obstacles_labels.txt"
    ),

    //YoloV8 (Original Model)  ---------------------------------------------------------------------

    YOLOV8_FLOAT32(
        "YOLOv8 Float32",
        "yoloV8_float32.tflite",
        "yoloV8_labels.txt"
    );

    /**Default model used across the application when no specific model is selected. */
    companion object {
        val DEFAULT = YOLO8_18_OBSTACLE_FP32BIT
    }
}
