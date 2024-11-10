package com.example.realtime_obstacle_detection.ui.activities

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.realtime_obstacle_detection.data.ObstacleDetector
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.domain.ObstacleClassifier
import com.example.realtime_obstacle_detection.presentation.camera.CameraPreview
import com.example.realtime_obstacle_detection.presentation.tensorflow.TensorFlowLiteFrameAnalyzer
import com.example.realtime_obstacle_detection.ui.theme.primary
import com.example.realtime_obstacle_detection.utis.boxdrawer.drawBoundingBoxes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlindDetectorActivity : ComponentActivity(),ObstacleClassifier{
    private var image by mutableStateOf<Bitmap?>(null)
    private lateinit var obstacleDetector: ObstacleDetector
    private lateinit var extensionsManager: ExtensionsManager
    private lateinit var cameraProvider: ProcessCameraProvider
    private val processingScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setupCameraXExtensions()

        setContent {

            obstacleDetector = ObstacleDetector(
                context = baseContext,
                obstacleClassifier = this
            )

            obstacleDetector.setup()

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = primary
            ) {

                CameraPreview(
                    controller = LifecycleCameraController(applicationContext).apply {
                        setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                        bindCameraUseCases(this)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                image?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Processed Image"
                    )
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
                    val hdrCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(selector, ExtensionMode.HDR)
                    bindCameraUseCases(LifecycleCameraController(applicationContext).apply {
                        cameraSelector = hdrCameraSelector
                    })
                }
            }, ContextCompat.getMainExecutor(this))
        }, ContextCompat.getMainExecutor(this))
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

    override fun onDetect(objectDetectionResults: List<ObjectDetectionResult>, detectedScene: Bitmap) {

        Log.i("obstacle detector", "detected objects: $objectDetectionResults")

        processingScope.launch {

            val startTime = System.currentTimeMillis()
            val updatedBitmap = drawBoundingBoxes(detectedScene, objectDetectionResults)
            image = updatedBitmap

            val endTime = System.currentTimeMillis()

            val duration = (endTime - startTime) / 1000.0

            Log.d("bounding box drawing", "bounding box drawing took $duration seconds")
        }
    }

    override fun onEmptyDetect() {
        Log.i("obstacle detector", "no object has been detected yet")
        image = null
    }
}
