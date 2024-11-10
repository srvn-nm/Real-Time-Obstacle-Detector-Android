package com.example.realtime_obstacle_detection.utis.vibration

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

fun vibratePhone(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    if (vibrator.hasVibrator()) {  // Check if the device has a vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // New API for Android Oreo and above with better control over patterns
            // Example: Vibrate for 500 milliseconds
            val effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            // Deprecated in API 26, but necessary for pre-Oreo devices
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)  // Vibrate for 500 milliseconds
        }
    }
}


