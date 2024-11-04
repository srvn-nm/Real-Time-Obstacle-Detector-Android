package com.example.realtime_obstacle_detection.ui.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.realtime_obstacle_detection.ui.theme.Realtime_Obstacle_DetectionTheme

class AboutUsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Realtime_Obstacle_DetectionTheme {

            }
        }
    }
}
