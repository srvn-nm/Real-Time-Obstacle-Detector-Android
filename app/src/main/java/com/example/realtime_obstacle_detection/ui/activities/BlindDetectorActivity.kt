package com.example.realtime_obstacle_detection.ui.activities

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.domain.ObstacleClassifier

class BlindDetectorActivity : ComponentActivity(),ObstacleClassifier{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

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
