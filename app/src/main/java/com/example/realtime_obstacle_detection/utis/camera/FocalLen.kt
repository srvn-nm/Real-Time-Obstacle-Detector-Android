package com.example.realtime_obstacle_detection.utis.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/**
 * Retrieves the focal length (in millimeters) of the device's back-facing camera.
 *
 * The function queries the [CameraManager] for available cameras, checks for the
 * back-facing camera, and returns the first available focal length value.
 *
 * @param context The [Context] used to access the system's [CameraManager].
 * @return The focal length of the back-facing camera in millimeters, or `null`
 *         if it cannot be determined.
 *
 * Note:
 * - Devices may report multiple focal lengths (e.g., for zoom or multi-lens cameras);
 *   this function returns the first one.
 * - Requires the `android.permission.CAMERA` permission in some cases.
 * - May return `null` if the camera is inaccessible or does not provide focal length info.
 */
fun getFocalLength(context: Context): Float? {

    // Obtain the CameraManager for finding desired focal len
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

    // Return null if no focal length could be determined
    return null
}
