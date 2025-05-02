package com.example.realtime_obstacle_detection.ui.screens.initialConfigurations

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationCard(
    onConfigurationSelected: (ModelConfig) -> Unit
) {
    var selectedModel by remember { mutableStateOf(Models.DEFAULT) }
    var configThreshold by remember { mutableFloatStateOf(0.2f) }
    var iouThreshold by remember { mutableFloatStateOf(0.2f) }
    var threadCount by remember { mutableFloatStateOf(4f) }
    var nnApiEnabled by remember { mutableStateOf(true) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .width(320.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Initial Configurations",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Model Dropdown
                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    TextField(
                        readOnly = true,
                        value = selectedModel.displayName,
                        onValueChange = {},
                        modifier = Modifier.menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = dropdownExpanded
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        Models.entries.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        model.displayName,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedModel = model
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Thresholds Row
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ThresholdSlider(
                        label = "Config Threshold",
                        value = configThreshold,
                        onValueChange = { configThreshold = it },
                        modifier = Modifier.weight(1f)
                    )

                    ThresholdSlider(
                        label = "IoU Threshold",
                        value = iouThreshold,
                        onValueChange = { iouThreshold = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Thread Count
                Column {
                    Text(
                        "Thread Count: ${threadCount.toInt()}",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = threadCount,
                        onValueChange = { threadCount = it },
                        valueRange = 0f..10f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.tertiary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // NN API Selection
                Column {
                    Text(
                        "Use NN API?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = nnApiEnabled,
                                onCheckedChange = { nnApiEnabled = true },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            Text("Yes", color = MaterialTheme.colorScheme.onSurface)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = !nnApiEnabled,
                                onCheckedChange = { nnApiEnabled = false },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                            Text("No", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // Get Started Button
                Button(
                    onClick = {
                        onConfigurationSelected(
                            ModelConfig(
                                selectedModel = selectedModel,
                                configThreshold = configThreshold,
                                iouThreshold = iouThreshold,
                                threadCount = threadCount.toInt(),
                                useNNAPI = nnApiEnabled
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Get Started!")
                }
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Composable
fun ConfigurationCardPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Your existing content that the card will overlay
            Text("Main Screen Content", modifier = Modifier.align(Alignment.Center))

            // The configuration card overlay
            ConfigurationCard(onConfigurationSelected = { config ->
                println("Preview Config: $config")
            })
        }
    }
}