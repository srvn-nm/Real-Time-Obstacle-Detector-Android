package com.example.realtime_obstacle_detection.utis.saveImage

import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream


fun saveBitmapAsPNG(bitmap: Bitmap, filename: String) {
    val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    if (!storageDir.exists()) {
        storageDir.mkdirs()
    }

    var imageFile = File(storageDir, "$filename.png")
    var fileIndex = 1

    // Check if the file exists and create a new filename with an index realtime_object_detection
    while (imageFile.exists()) {
        imageFile = File(storageDir, "${filename}_$fileIndex.png")
        fileIndex++
    }

    FileOutputStream(imageFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)  // PNG is a lossless format, the quality parameter is ignored.
    }
}