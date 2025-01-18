import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.realtime_obstacle_detection.data.ObstacleDetectorConfig


@Composable
fun SettingsScreen(context: Context, config: ObstacleDetectorConfig) {
    val modelOptions = listOf(
        "best_float32.tflite",
        "best_float16.tflite",
        "best_full_integer_quant.tflite",
        "best_int8.tflite",
        "best_integer_quant.tflite",
        "yoloV8_float32.tflite"
    )

    var selectedModelPath by remember { mutableStateOf(config.modelPath) }
    var labelPath by remember { mutableStateOf(config.labelPath) }
    var threadsCount by remember { mutableStateOf(config.threadsCount) }
    var useNNAPI by remember { mutableStateOf(config.useNNAPI) }
    var confidenceThreshold by remember { mutableStateOf(config.confidenceThreshold) }
    var iouThreshold by remember { mutableStateOf(config.iouThreshold) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.h4)

        // Model Path Dropdown
        Text(text = "Select Model")
        DropdownMenu(
            expanded = true,
            onDismissRequest = {},
        ) {
            modelOptions.forEach { model ->
                DropdownMenuItem(onClick = {
                    selectedModelPath = model
                    labelPath = if (model == "yoloV8_float32.tflite") "yoloV8_labels.txt" else "best_labels.txt"
                }) {
                    Text(text = model)
                }
            }
        }
        Text(text = "Selected Model: $selectedModelPath")

        // Threads Count Slider
        Text(text = "Threads Count: $threadsCount")
        Slider(
            value = threadsCount.toFloat(),
            valueRange = 1f..5f,
            steps = 4,
            onValueChange = { threadsCount = it.toInt() }
        )

        // Use NNAPI Toggle
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Use NNAPI")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = useNNAPI, onCheckedChange = { useNNAPI = it })
        }

        // Confidence Threshold Slider
        Text(text = "Confidence Threshold: ${"%.2f".format(confidenceThreshold)}")
        Slider(
            value = confidenceThreshold,
            valueRange = 0f..1f,
            steps = 10,
            onValueChange = { confidenceThreshold = it }
        )

        // IOU Threshold Slider
        Text(text = "IOU Threshold: ${"%.2f".format(iouThreshold)}")
        Slider(
            value = iouThreshold,
            valueRange = 0f..1f,
            steps = 10,
            onValueChange = { iouThreshold = it }
        )

        // Save Button
        Button(onClick = {
            val updatedConfig = ObstacleDetectorConfig(
                modelPath = selectedModelPath,
                labelPath = labelPath,
                threadsCount = threadsCount,
                useNNAPI = useNNAPI,
                confidenceThreshold = confidenceThreshold,
                iouThreshold = iouThreshold
            )
            ObstacleDetectorConfig.save(context, updatedConfig)
        }) {
            Text(text = "Save Settings")
        }
    }
}
