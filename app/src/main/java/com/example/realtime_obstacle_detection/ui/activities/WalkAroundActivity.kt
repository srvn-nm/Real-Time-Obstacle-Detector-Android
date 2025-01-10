package com.example.realtime_obstacle_detection.ui.activities
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
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
import androidx.lifecycle.LifecycleOwner
import com.example.realtime_obstacle_detection.data.ObstacleDetector
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.domain.ObstacleClassifier
import com.example.realtime_obstacle_detection.presentation.camera.CameraPreview
import com.example.realtime_obstacle_detection.presentation.tensorflow.TensorFlowLiteFrameAnalyzer
import com.example.realtime_obstacle_detection.utis.vibration.vibratePhone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class WalkAroundActivity : ComponentActivity(), ObstacleClassifier {
    private var modelMessage by mutableStateOf<String?>(null)
    private lateinit var extensionsManager: ExtensionsManager
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

            obstacleDetector = ObstacleDetector(
                context = baseContext,
                obstacleClassifier = this@WalkAroundActivity
            )
            obstacleDetector.setup()

            Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                CameraPreview(
                    controller = LifecycleCameraController(context).apply {
                        setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                        bindCameraUseCases(this)
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
                                color = Color.Black,
                                modifier = Modifier.padding(16.dp)
                            )
                            Text(
                                text = modelMessage ?: "",
                                style = MaterialTheme.typography.subtitle1,
                                color = Color.Black,
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

    private fun bindCameraUseCases(controller: LifecycleCameraController) {
        controller.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(applicationContext),
            TensorFlowLiteFrameAnalyzer(
                obstacleDetector = obstacleDetector,
            )
        )
        controller.bindToLifecycle(this as LifecycleOwner)
    }

    private fun setupCameraXExtensions() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(this, cameraProvider)
            extensionsManagerFuture.addListener({
                extensionsManager = extensionsManagerFuture.get()
                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                if (extensionsManager.isExtensionAvailable(selector, ExtensionMode.HDR)) {
                    val hdrCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(selector, ExtensionMode.HDR)
                    bindCameraUseCases(LifecycleCameraController(applicationContext).apply {
                        cameraSelector = hdrCameraSelector
                    })
                }
            }, ContextCompat.getMainExecutor(this))
        }, ContextCompat.getMainExecutor(this))
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

                vibratePhone(
                    context = this@WalkAroundActivity
                )
            }
        }
    }


    override fun onEmptyDetect() {
        Log.i("obstacle detector", "No object has been detected yet")
        //showAlert = false
    }
}