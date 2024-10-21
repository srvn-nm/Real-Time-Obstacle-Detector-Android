package com.example.realtime_obstacle_detection.utis.boxdrawer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.utis.saveImage.saveBitmapAsPNG

fun drawBoundingBoxes(bitmap: Bitmap, boxes: List<ObjectDetectionResult>): Bitmap {
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)
    val paint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    val textPaint = Paint().apply {
        color = Color.BLUE
        textSize = 80f
        typeface = Typeface.DEFAULT_BOLD
    }

    for (box in boxes) {
        val rect = RectF(
            box.x1 * mutableBitmap.width,
            box.y1 * mutableBitmap.height,
            box.x2 * mutableBitmap.width,
            box.y2 * mutableBitmap.height
        )
        canvas.drawRect(rect, paint)
        canvas.drawText(box.className, rect.left, rect.bottom, textPaint)

        saveBitmapAsPNG(mutableBitmap, box.className)
    }

    return mutableBitmap
}