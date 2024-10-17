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
import androidx.compose.runtime.Composable
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.ui.theme.Realtime_Obstacle_DetectionTheme


class MainActivity : ComponentActivity(), ObstacleDetector.DetectorListener {
    private var image by mutableStateOf<Bitmap?>(null)
    private lateinit var obstacleDetector: ObstacleDetector

    private val processingScope = CoroutineScope(Dispatchers.IO)  // IOスレッドで実行

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
        }

        image = BitmapFactory.decodeResource(resources, R.drawable.sample)
        obstacleDetector = ObstacleDetector(baseContext, this)
        obstacleDetector.setup()
        image?.let {
            obstacleDetector.detect(it)
        }

        setContent {
            Realtime_Obstacle_DetectionTheme{
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyImage(bitmap = image)
                }
            }
        }
    }

    override fun onDetect(objectDetectionResults: List<ObjectDetectionResult>) {

        image?.let { bmp ->
            processingScope.launch {
                val updatedBitmap = drawBoundingBoxes(bmp, objectDetectionResults)
                image = updatedBitmap
            }
        }
    }

    override fun onEmptyDetect() {
        Log.i("empty", "empty")
    }

    private fun drawBoundingBoxes(bitmap: Bitmap, boxes: List<ObjectDetectionResult>): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.MAGENTA
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        val textPaint = Paint().apply {
            color = Color.rgb(0,255,0)
            textSize = 80f
            typeface = Typeface.DEFAULT_BOLD
        }

        for (box in boxes) {
            val rect = RectF(
                box.x1 * mutableBitmap.width,
                box.y1 * mutableBitmap.height,
                box.x2 * mutableBitmap.width,
                box.y2 * mutableBitmap.height
            )
            canvas.drawRect(rect, paint)
            canvas.drawText(box.clsName, rect.left, rect.bottom, textPaint)
        }

        return mutableBitmap
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun MyImage(bitmap: Bitmap?) {
    bitmap?.let {
        Image(bitmap = it.asImageBitmap(), contentDescription = "")
    }
}














/**
import androidx.compose.ui.Alignment
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import com.example.realtime_obstacle_detection.presentation.tensorflow.TensorFlowLiteFrameAnalyzer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.realtime_obstacle_detection.data.TensorFlowLiteClassifier
import com.example.realtime_obstacle_detection.domain.ClassificationResults
import com.example.realtime_obstacle_detection.presentation.camera.CameraPreview
import com.example.realtime_obstacle_detection.ui.theme.Realtime_Obstacle_DetectionTheme
import com.example.realtime_obstacle_detection.ui.theme.primary
import com.example.realtime_obstacle_detection.ui.theme.secondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(!hasCameraPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), 0
            )
        }
        setContent {
            Realtime_Obstacle_DetectionTheme {

                var classifications by remember {
                    mutableStateOf(emptyList<ClassificationResults>())
                }

                val analyzer = remember {
                    TensorFlowLiteFrameAnalyzer(
                        classifier = TensorFlowLiteClassifier(
                            context = applicationContext
                        ),
                        onResults = {
                            classifications = it
                        }
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
                    }
                }
            }
        }
    }


} **/