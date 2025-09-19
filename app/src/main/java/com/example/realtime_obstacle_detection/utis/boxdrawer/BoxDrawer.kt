package com.example.realtime_obstacle_detection.utis.boxdrawer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.utis.saveImage.saveBitmapAsPNG


/**
 * Draws bounding boxes on a bitmap. This function no longer handles ARCore hit tests.
 * Instead, it receives the distance already computed on the main thread.
 *
 * @param bitmap The bitmap to draw on.
 * @param boxes The list of detection results, now including the distance.
 * @return The new bitmap with the bounding boxes drawn.
 */
fun drawBoundingBoxes(
    bitmap: Bitmap,
    boxes: List<ObjectDetectionResult>
): Bitmap {
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)

    // Configure paints
    val paint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.CYAN
        strokeWidth = 8f
    }

    val textPaint = Paint().apply {
        color = Color.BLUE
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(5f, 0f, 0f, Color.WHITE)
    }

    for (box in boxes) {
        // Set color based on confidence level
        paint.color = when {
            box.confidenceRate >= 0.7 -> Color.GREEN
            box.confidenceRate >= 0.4 -> Color.YELLOW
            else -> Color.RED
        }

        val rect = RectF(
            box.x1 * mutableBitmap.width,
            box.y1 * mutableBitmap.height,
            box.x2 * mutableBitmap.width,
            box.y2 * mutableBitmap.height
        )

        // Draw bounding box
        canvas.drawRect(rect, paint)

        // Draw label with confidence + distance
        val distanceText = box.distance?.let { "%.2f m".format(it) } ?: "N/A"
        val labelText = "${box.className} (${"%.2f".format(box.confidenceRate)}) in $distanceText "
        canvas.drawText(labelText, rect.left, rect.top - 10, textPaint)

        // Log detection metrics
        Log.i(
            "DetectionMetrics", """
            Class: ${box.className}
            Confidence: ${"%.2f".format(box.confidenceRate)}
            Distance: ${"%.2f".format(box.distance)}
            Position: (${"%.2f".format(box.x1)}, ${"%.2f".format(box.y1)}) - 
            (${"%.2f".format(box.x2)}, ${"%.2f".format(box.y2)})
        """.trimIndent()
        )

        saveBitmapAsPNG(mutableBitmap, box.className)
    }

    return mutableBitmap
}

fun calculateIoU(a: ObjectDetectionResult, b: ObjectDetectionResult): Float {
    val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
    val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)

    val interX1 = maxOf(a.x1, b.x1)
    val interY1 = maxOf(a.y1, b.y1)
    val interX2 = minOf(a.x2, b.x2)
    val interY2 = minOf(a.y2, b.y2)

    val interArea = maxOf(0f, interX2 - interX1) * maxOf(0f, interY2 - interY1)
    return interArea / (areaA + areaB - interArea)
}