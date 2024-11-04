package com.example.realtime_obstacle_detection.ui.activities

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.domain.ObstacleClassifier
import com.example.realtime_obstacle_detection.ui.theme.Realtime_Obstacle_DetectionTheme

class BlindDetectorActivity : ComponentActivity(),ObstacleClassifier{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Realtime_Obstacle_DetectionTheme {

            }
        }
    }

    override fun onEmptyDetect() {
        TODO("Not yet implemented")
    }

    override fun onDetect(
        objectDetectionResults: List<ObjectDetectionResult>,
        detectedScene: Bitmap
    ) {
        TODO("Not yet implemented")
    }
}
