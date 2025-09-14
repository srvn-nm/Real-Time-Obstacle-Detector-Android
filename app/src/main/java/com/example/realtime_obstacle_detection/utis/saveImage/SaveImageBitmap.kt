package com.example.realtime_obstacle_detection.utis.saveImage

import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

/**
 * Saves a given [Bitmap] and detected object's with its bounding box as a PNG file
 * in the device's public Pictures directory.
 *
 * The function ensures the target directory exists, and if a file with the same name
 * already exists, it appends an incrementing index to the filename until a unique
 * file is created (e.g., `filename.png`, `filename_1.png`, `filename_2.png`, ...).
 *
 * @param bitmap The [Bitmap] image to save.
 * @param filename The desired base filename. For detected objects will be detected class name.
 *
 * Note:
 * - PNG is a lossless format, so the `quality` parameter in [Bitmap.compress] is ignored.
 * - Requires WRITE_EXTERNAL_STORAGE permission on devices running Android 10 (API 29) or below.
 *   On Android 11 (API 30) and above, consider using scoped storage with MediaStore.
 */
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
        //Bitmap.CompressFormat.PNG: PNG is a lossless format, the quality parameter is ignored.
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
}