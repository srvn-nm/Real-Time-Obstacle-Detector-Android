package com.example.realtime_obstacle_detection.utis.camera

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.SizeF

/**
 * Retrieves the physical sensor size of the device's camera in millimeters.
 *
 * The function queries the [CameraManager] for available cameras, selects either
 * the front or back camera based on the [useFrontCamera] parameter, and returns
 * the sensor's physical dimensions as a [SizeF].
 *
 * @param context The [Context] used to access the system's [CameraManager].
 * @param useFrontCamera If `true`, retrieves info for the front-facing camera. otherwise retrieves the back-facing camera (default).
 * @return A [SizeF] object where width and height are the physical sensor dimensions in millimeters, or `null` if unavailable.
 *
 * Note:
 * - Requires `android.permission.CAMERA` in some scenarios.
 * - Useful for calculating the field of view (FOV) or real-world object size
 *   estimation when combined with focal length.
 * - Some devices may not expose sensor size information, in which case `null` is returned.
 */
fun getCameraSensorInfo(context: Context, useFrontCamera: Boolean = false): SizeF? {

    // Obtain the CameraManager for finding senor's size
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    try {

        for (cameraId in manager.cameraIdList) {

            val characteristics = manager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

            if ((useFrontCamera && facing == CameraCharacteristics.LENS_FACING_FRONT) ||
                (!useFrontCamera && facing == CameraCharacteristics.LENS_FACING_BACK)){

                //Retrieve camera sensors length from its characteristics
                val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                // SizeF where width and height are in millimeters
                return sensorSize
            }
        }
    } catch (e: CameraAccessException) {
        e.printStackTrace()
    }

    return null
}
