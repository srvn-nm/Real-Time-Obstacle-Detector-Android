package com.example.realtime_obstacle_detection.utis.vibration

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Triggers a short vibration on the device (500 milliseconds)
 * aim to alert user from detected objects and possible obstacles in path.
 *
 * This function checks if the device has a vibrator and uses the appropriate
 * API depending on the Android version:
 * - For Android Oreo (API 26) and above, it uses [VibrationEffect] for precise control.
 * - For earlier versions, it falls back to the deprecated [Vibrator.vibrate] method.
 *
 * @param context The [Context] used to access the system's [Vibrator] service.
 */
fun vibratePhone(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    if (vibrator.hasVibrator()) {  // Check if the device has a vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // New API for Android Oreo and above with better control over patterns
            // Vibrate for 500 milliseconds
            val effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            // Deprecated in API 26, but necessary for pre-Oreo devices
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)  // Vibrate for 500 milliseconds
        }
    }
}


