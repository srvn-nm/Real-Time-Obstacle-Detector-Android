package com.example.realtime_obstacle_detection


import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.Manifest
import android.os.Build
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.Image
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.presentation.camera.CameraPreview
import com.example.realtime_obstacle_detection.presentation.tensorflow.TensorFlowLiteFrameAnalyzer
import com.example.realtime_obstacle_detection.ui.theme.Realtime_Obstacle_DetectionTheme
import com.example.realtime_obstacle_detection.utis.boxdrawer.drawBoundingBoxes

class MainActivity : ComponentActivity(), ObstacleDetector.DetectorListener {

    private var image by mutableStateOf<Bitmap?>(null)
    private lateinit var obstacleDetector: ObstacleDetector

    private val processingScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
        }

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), 0)
        }

        setContent {

            obstacleDetector = ObstacleDetector(baseContext, this)
            obstacleDetector.setup()

            val analyzer = remember {
                TensorFlowLiteFrameAnalyzer(
                    obstacleDetector= obstacleDetector,
                    image
                )
            }

            val controller = remember {
                LifecycleCameraController(applicationContext).apply {
                    setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                    setImageAnalysisAnalyzer(
                        ContextCompat.getMainExecutor(applicationContext),
                        analyzer
                    )
                }
            }

            Realtime_Obstacle_DetectionTheme{
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    //we need a camera preview as background
                    CameraPreview(
                        controller = controller,
                        modifier = Modifier.fillMaxSize()
                    )

                    image?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Detected Image"
                        )
                    }

                }
            }
        }
    }

    override fun onDetect(objectDetectionResults: List<ObjectDetectionResult>) {

        print("11111111111111111111111111111")
        image?.let { bmp ->
            processingScope.launch {
                val updatedBitmap = drawBoundingBoxes(bmp, objectDetectionResults)
                image = updatedBitmap
            }
        }
    }

    override fun onEmptyDetect() {
        Log.i("obstacle detector", "no object has been detected yet")
    }



    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

/**
Box(
modifier = Modifier
.fillMaxSize()
){

//we need a camera preview as background
CameraPreview(
controller = controller,
modifier = Modifier.fillMaxSize()
)

//our final results will be attached to page header
Column(
modifier = Modifier
.fillMaxWidth()
.align(Alignment.TopCenter)
) {
classifications.forEach {
Text(
text = it.label,
modifier = Modifier
.fillMaxWidth()
.background(secondary)
.padding(10.dp),
textAlign = TextAlign.Center,
fontSize = 20.sp,
color = primary
)
}
} **/