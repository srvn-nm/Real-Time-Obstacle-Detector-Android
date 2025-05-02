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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.CircularProgressIndicator
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Surface
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
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.realtime_obstacle_detection.utis.boxdrawer.drawBoundingBoxes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnDetectionActivity : ComponentActivity(), ObstacleClassifier {
    private var image by mutableStateOf<Bitmap?>(null)
    private lateinit var obstacleDetector: ObstacleDetector
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraProvider: ProcessCameraProvider
    private val processingScope = CoroutineScope(Dispatchers.IO)
    private var modelConfig: ModelConfig? = null
    // State to track whether the detector is ready
    private var isDetectorReady by mutableStateOf(false)
    // Mutable state to hold the computed FPS value
    private var fps by mutableIntStateOf(0)
    private var inferenceTimeMs by mutableLongStateOf(0L)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isConfigured by remember { mutableStateOf(false) }

            if (!isConfigured) {
                // Show configuration card and wait for user configuration
                ConfigurationCard { config ->
                    modelConfig = config
                    isConfigured = true

                    // Use lazy async initialization on the IO dispatcher
                    val detectorDeferred = lifecycleScope.async(Dispatchers.IO) {
                        // Initialize detector with configuration
                        val detector = ObstacleDetector(
                            context = baseContext,
                            obstacleClassifier = this@OnDetectionActivity,
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
                    val controller = remember { LifecycleCameraController(applicationContext) }

                    LaunchedEffect(Unit) {
                        controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
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
                                text = "FPmS: ${fps.toDouble() / 1000.0}",
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

                        image?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Processed Image",
                                modifier = Modifier.fillMaxSize()
                            )
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

    override fun onDetect(objectDetectionResults: List<ObjectDetectionResult>, detectedScene: Bitmap) {
        Log.i("obstacle detector", "Detected objects: $objectDetectionResults")
        processingScope.launch {
            val startTime = System.currentTimeMillis()
            val updatedBitmap = drawBoundingBoxes(
                detectedScene,
                objectDetectionResults,
                modelConfig?.configThreshold ?: 0.5f,
                modelConfig?.iouThreshold ?: 0.5f
            )
            image = updatedBitmap
            val duration = (System.currentTimeMillis() - startTime) / 1000.0
            Log.d("Bounding Box", "Drawing took $duration seconds")
        }
    }

    override fun onEmptyDetect() {
        Log.i("obstacle detector", "No objects detected")
        image = null
    }

}