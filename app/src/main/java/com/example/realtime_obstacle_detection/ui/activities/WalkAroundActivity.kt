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
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.realtime_obstacle_detection.data.ObstacleDetector
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.domain.ObstacleClassifier
import com.example.realtime_obstacle_detection.presentation.camera.CameraPreview
import com.example.realtime_obstacle_detection.presentation.tensorflow.TensorFlowLiteFrameAnalyzer
import com.example.realtime_obstacle_detection.ui.screens.initialConfigurations.ConfigurationCard
import com.example.realtime_obstacle_detection.ui.screens.initialConfigurations.ModelConfig
import com.example.realtime_obstacle_detection.utis.vibration.vibratePhone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WalkAroundActivity : ComponentActivity(), ObstacleClassifier {
    private var modelMessage by mutableStateOf<String?>(null)
    private lateinit var extensionsManager: ExtensionsManager
    private var showAlert by mutableStateOf(false)
    private var alertColor by mutableStateOf(Color.Yellow)
    private var alertText by mutableStateOf("CAUTION")
    private lateinit var obstacleDetector: ObstacleDetector
    private lateinit var cameraProvider: ProcessCameraProvider
    private val processingScope = CoroutineScope(Dispatchers.IO)
    private var modelConfig: ModelConfig? = null
    private var isConfigured by mutableStateOf(false)
    // State to track whether the detector is ready
    private var isDetectorReady by mutableStateOf(false)
    // Mutable state to hold the computed FPS value
    private var fps by mutableIntStateOf(0)
    private var inferenceTimeMs by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current

            if (!isConfigured) {
                // Show configuration card and wait for user configuration
                ConfigurationCard { config ->
                    modelConfig = config
                    isConfigured = true

                    // Initialize detector with configuration
                    // Lazy async initialization on the IO dispatcher.
                    // Note: We explicitly return the detector instance after calling setup()
                    val detectorDeferred = lifecycleScope.async(Dispatchers.IO) {
                        val detector = ObstacleDetector(
                            context = baseContext,
                            obstacleClassifier = this@WalkAroundActivity,
                            modelPath = config.selectedModel.modelFileName,
                            labelPath = config.selectedModel.labelFileName,
                            confidenceThreshold = config.configThreshold,
                            iouThreshold = config.iouThreshold,
                            threadsCount = config.threadCount,
                            useNNAPI = config.useNNAPI
                        )
                        detector.setup() // setup returns Unit
                        detector      // return the detector instance
                    }
                    // Await detector initialization in a coroutine
                    lifecycleScope.launch {
                        obstacleDetector = detectorDeferred.await()
                        isDetectorReady = true
                        withContext(Dispatchers.Main) {
                            setupCameraXExtensions()
                        }
                    }
                }
            } else {
                // If configured but detector is not yet ready, show a loading indicator
                if (!isDetectorReady) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Once the detector is ready, show the camera preview and overlay processed image if available.
                    val controller = remember { LifecycleCameraController(context) }

                    LaunchedEffect(Unit) {
                        controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                        bindCameraUseCases(controller)
                    }

                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                        // Overlay the FPS text on top of the camera preview.

                            CameraPreview(
                                controller = controller,
                                modifier = Modifier.fillMaxSize()
                            )
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            androidx.compose.material3.Text(
                                text = "FPmS: ${fps / 1000}",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .padding(16.dp)
                            )
                            androidx.compose.material3.Text(
                                text = "Infer: $inferenceTimeMs ms",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .padding(16.dp)
                            )
                        }


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
                                    Button(
                                        onClick = { showAlert = false },
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color.Black
                                        )
                                    ) {
                                        Text("Acknowledged", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindCameraUseCases(controller: LifecycleCameraController) {
        // Update the FPS state on the main thread.
        val analyzer = TensorFlowLiteFrameAnalyzer(
            obstacleDetector = obstacleDetector,
            onFpsCalculated = { calculatedFps ->
                runOnUiThread { fps = calculatedFps } // 'fps' is your mutable state variable
            },
            onInferenceTime = { ms ->
                runOnUiThread { inferenceTimeMs = ms }
            }
        )
        controller.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(applicationContext),
            analyzer
        )
        controller.bindToLifecycle(this)
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
                    val hdrCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
                        selector,
                        ExtensionMode.HDR
                    )
                    bindCameraUseCases(LifecycleCameraController(applicationContext).apply {
                        cameraSelector = hdrCameraSelector
                    })
                }
            }, ContextCompat.getMainExecutor(this))
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDetect(objectDetectionResults: List<ObjectDetectionResult>, detectedScene: Bitmap) {
        val filteredResults = objectDetectionResults.filter {
            it.confidenceRate >= (modelConfig?.configThreshold ?: 0.5f)
        }

        val closestObject = filteredResults.minByOrNull { it.distance ?: Float.MAX_VALUE }
        closestObject?.let {
            if (it.distance != null) {
                val distance = it.distance
                val warningLevel = if (distance < 1f) "DANGER" else "CAUTION"
                val message = "You are ${if (warningLevel == "DANGER") "very close" else "approaching"} to a ${it.className}."

                modelMessage = message
                alertText = warningLevel
                alertColor = if (warningLevel == "DANGER") Color.Red else Color.Yellow
                showAlert = true

                vibratePhone(context = this@WalkAroundActivity)
            }
        }
    }

    override fun onEmptyDetect() {
        Log.i("obstacle detector", "No object has been detected yet")
        showAlert = false
    }
}