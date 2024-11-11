package com.example.realtime_obstacle_detection.ui.activities
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Vibrator
import android.os.Build
import android.os.VibrationEffect
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.realtime_obstacle_detection.data.ObstacleDetector
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.domain.ObstacleClassifier
import com.example.realtime_obstacle_detection.presentation.camera.CameraPreview
import com.example.realtime_obstacle_detection.presentation.tensorflow.TensorFlowLiteFrameAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WalkAroundActivity : ComponentActivity(), ObstacleClassifier {
    private var modelMessage by mutableStateOf<String?>(null)
    private var showAlert by mutableStateOf(false)
    private var alertColor by mutableStateOf(Color.Yellow)
    private var alertText by mutableStateOf("CAUTION")
    private lateinit var obstacleDetector: ObstacleDetector
    private lateinit var cameraProvider: ProcessCameraProvider
    private val processingScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCameraXExtensions()

        setContent {
            val context = LocalContext.current

            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                CameraPreview(
                    controller = LifecycleCameraController(context).apply {
                        setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                        bindToLifecycle(this@WalkAroundActivity)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (showAlert) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(alertColor)
                            .clickable { showAlert = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = alertText,
                                style = MaterialTheme.typography.h4,
                                color = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                            Text(
                                text = modelMessage ?: "",
                                style = MaterialTheme.typography.subtitle1,
                                color = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(onClick = { showAlert = false }, colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black)) {
                                Text("Acknowledged", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupCameraXExtensions() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases(LifecycleCameraController(applicationContext))
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases(controller: LifecycleCameraController) {
        controller.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(applicationContext),
            TensorFlowLiteFrameAnalyzer(obstacleDetector)
        )
        controller.bindToLifecycle(this)
    }

    override fun onDetect(objectDetectionResults: List<ObjectDetectionResult>, detectedScene: Bitmap) {
        val closestObject = objectDetectionResults.minByOrNull { it.distance ?: Float.MAX_VALUE }
        closestObject?.let {
            if (it.distance != null) {
                val distance = it.distance
                val warningLevel = if (distance < 1f) "DANGER" else "CAUTION"
                val message = "You are ${if (warningLevel == "DANGER") "very close" else "approaching"} to a ${it.className}."

                modelMessage = message
                alertText = warningLevel
                alertColor = if (warningLevel == "DANGER") Color.Red else Color.Yellow
                showAlert = true

                // Trigger vibration
                vibratePhone(warningLevel == "DANGER")
            }
        }
    }

    private fun vibratePhone(isDanger: Boolean) {
        val pattern = if (isDanger) longArrayOf(0, 500, 100, 500) else longArrayOf(0, 300)
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(pattern, -1)
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(pattern, -1)
        }
    }

    override fun onEmptyDetect() {
        modelMessage = "No object has been detected yet."
        showAlert = false
    }
}