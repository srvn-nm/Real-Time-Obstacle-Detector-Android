package com.example.realtime_obstacle_detection.utis.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

fun getFocalLength(context: Context): Float? {
    // Obtain the CameraManager
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
        // Loop through available cameras and check for back-facing camera
        for (cameraId in cameraManager.cameraIdList) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)

            // Check if this is the back camera (you can also look for the front camera if needed)
            if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                // Retrieve focal length (in millimeters)
                val focalLengths = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                return focalLengths?.get(0)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null // Return null if no focal length could be determined
}
