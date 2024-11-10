package com.example.realtime_obstacle_detection.utis.camera

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.SizeF

fun getCameraSensorInfo(context: Context, useFrontCamera: Boolean = false): SizeF? {
    val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
        for (cameraId in manager.cameraIdList) {
            val characteristics = manager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if ((useFrontCamera && facing == CameraCharacteristics.LENS_FACING_FRONT) ||
                (!useFrontCamera && facing == CameraCharacteristics.LENS_FACING_BACK)) {
                val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                return sensorSize  // SizeF where width and height are in millimeters
            }
        }
    } catch (e: CameraAccessException) {
        e.printStackTrace()
    }
    return null
}
