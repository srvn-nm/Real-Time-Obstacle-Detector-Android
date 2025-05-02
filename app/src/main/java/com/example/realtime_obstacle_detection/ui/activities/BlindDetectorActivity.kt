package com.example.realtime_obstacle_detection.ui.activities

import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.example.realtime_obstacle_detection.ui.theme.primary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Locale


class BlindDetectorActivity : ComponentActivity(), ObstacleClassifier, TextToSpeech.OnInitListener {
    private lateinit var obstacleDetector: ObstacleDetector
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraProvider: ProcessCameraProvider
    private var textToSpeech: TextToSpeech? = null
    private val processingScope = CoroutineScope(Dispatchers.IO)
    private var modelConfig: ModelConfig? = null
    // State to track whether the detector is ready
    private var isDetectorReady by mutableStateOf(false)
    // Mutable state to hold the computed FPS value
    private var fps by mutableIntStateOf(0)
    private var inferenceTimeMs by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startTime = System.currentTimeMillis()
        textToSpeech = TextToSpeech(this, this)
        val endTime = System.currentTimeMillis()
        val duration = (endTime - startTime) / 1000.0
        Log.d(
            "processing time",
            "HDR Preprocessing and textToSpeech initialization took $duration seconds"
        )

        setContent {
            var isConfigured by remember { mutableStateOf(false) }

            if (!isConfigured) {
                // Show configuration card and wait for user configuration
                ConfigurationCard { config ->
                    modelConfig = config
                    isConfigured = true

                    // Initialize detector with configuration
                    // Offload heavy detector initialization to a background thread using async.
                    // The detector is lazily initialized and setup() is called in the background.
                    val detectorDeferred = lifecycleScope.async(Dispatchers.IO) {
                        val detector = ObstacleDetector(
                            context = baseContext,
                            obstacleClassifier = this@BlindDetectorActivity,
                            modelPath = config.selectedModel.modelFileName,
                            labelPath = config.selectedModel.labelFileName,
                            confidenceThreshold = config.configThreshold,
                            iouThreshold = config.iouThreshold,
                            threadsCount = config.threadCount,
                            useNNAPI = config.useNNAPI
                        )
                        detector.setup()  // setup returns Unit
                        detector // Return the detector instance
                    }
                    // Await detector initialization and update the state.
                    lifecycleScope.launch {
                        obstacleDetector = detectorDeferred.await()
                        isDetectorReady = true
                    }
                }
            } else {
                if (!isDetectorReady) {
                    // Show a loading indicator until the detector is ready.
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Once the detector is ready, initialize the camera preview.
                    val controller = remember { LifecycleCameraController(applicationContext) }

                    LaunchedEffect(Unit) {
                        controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                        setupCameraXExtensions(controller)
                        bindCameraUseCases(controller)
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = primary
                    ) {
                        // Overlay the FPS text on top of the camera preview.

                            CameraPreview(
                                controller = controller,
                                modifier = Modifier.fillMaxSize()
                            )
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "FPS: $fps",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .padding(16.dp)
                            )
                            Text(
                                text = "Infer: $inferenceTimeMs ms",
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setupCameraXExtensions(controller: LifecycleCameraController) {
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
                    controller.cameraSelector = hdrCameraSelector
                }
            }, ContextCompat.getMainExecutor(this))
        }, ContextCompat.getMainExecutor(this))
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

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "US Language supported")
            } else {
                Log.e("TTS", "US Language not supported")
            }
        } else {
            Log.e("TTS", "Initialization failed due unknown reason")
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    private fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    override fun onDetect(objectDetectionResults: List<ObjectDetectionResult>, detectedScene: Bitmap) {
        processingScope.launch {
            objectDetectionResults.forEach { result ->
                val text = "You are approaching to ${result.className}"
                speak(text)
            }
        }
    }

    override fun onEmptyDetect() {
        Log.i("obstacle detector", "No object has been detected yet")
    }
}
