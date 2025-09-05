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
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import io.github.sceneview.ar.ARSceneView
import kotlin.math.sqrt

fun drawBoundingBoxes(
    bitmap: Bitmap,
    boxes: List<ObjectDetectionResult>,
    confThreshold: Float,
    iouThreshold: Float,
    arSceneView: ARSceneView
): Bitmap {
    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)

    // Filter boxes by confidence threshold
    val filteredBoxes = boxes.filter { it.confidenceRate >= confThreshold }

    // Apply Non-Maximum Suppression (NMS) using IoU threshold
    val sortedBoxes = filteredBoxes.sortedByDescending { it.confidenceRate }
    val selectedBoxes = mutableListOf<ObjectDetectionResult>()

    for (box in sortedBoxes) {
        var shouldAdd = true
        for (selected in selectedBoxes) {
            if (calculateIoU(box, selected) > iouThreshold) {
                shouldAdd = false
                break
            }
        }
        if (shouldAdd) {
            selectedBoxes.add(box)
        }
    }

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

    //  Get current AR frame from SceneView
    val frame: Frame? = arSceneView.frame

    for (box in selectedBoxes) {
        // Set color based on confidence level
        paint.color = when {
            box.confidenceRate >= 0.7 -> Color.GREEN
            box.confidenceRate >= 0.4 -> Color.YELLOW
            else -> Color.RED
        }

        Log.i("obstacle detector confidence rate", "confidenceRate ratio: ${box.confidenceRate}")
        Log.i("obstacle detector distance ratio", "distance ratio: ${box.distance}")

        val rect = RectF(
            box.x1 * mutableBitmap.width,
            box.y1 * mutableBitmap.height,
            box.x2 * mutableBitmap.width,
            box.y2 * mutableBitmap.height
        )

        // Draw bounding box
        canvas.drawRect(rect, paint)

        // Get center of bounding box
        val centerX = (rect.left + rect.right) / 2f
        val centerY = (rect.top + rect.bottom) / 2f

        var distanceText = "N/A"

        // Perform AR raycast
        frame?.let { currentFrame ->
            try {
                // Perform hit test
                val hits: List<HitResult> = currentFrame.hitTest(centerX, centerY)
                if (hits.isNotEmpty()) {
                    val hit = hits[0]
                    // Camera pose (for distance calculation)
                    val cameraPose = currentFrame.camera.pose
                    val objPose = hit.hitPose

                    // Compute distance using 3D Euclidean distance :cite[2]:cite[6]
                    val dx = objPose.tx() - cameraPose.tx()
                    val dy = objPose.ty() - cameraPose.ty()
                    val dz = objPose.tz() - cameraPose.tz()
                    val distanceMeters = sqrt(dx * dx + dy * dy + dz * dz)

                    distanceText = "%.2f m".format(distanceMeters)
                    // Store the calculated distance in the detection result
                    box.distance = distanceMeters
                } else {
                    distanceText = "No Hit"
                    box.distance = null
                    Log.w("ARHitTest", "No surface detected for bounding box at center ($centerX, $centerY)")
                }
            } catch (e: Exception) {
                Log.e("ARHitTest", "Error during hit test: ${e.message}")
            }
        }

        // Draw label with confidence + distance
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

private fun calculateIoU(a: ObjectDetectionResult, b: ObjectDetectionResult): Float {
    val areaA = (a.x2 - a.x1) * (a.y2 - a.y1)
    val areaB = (b.x2 - b.x1) * (b.y2 - b.y1)

    val interX1 = maxOf(a.x1, b.x1)
    val interY1 = maxOf(a.y1, b.y1)
    val interX2 = minOf(a.x2, b.x2)
    val interY2 = minOf(a.y2, b.y2)

    val interArea = maxOf(0f, interX2 - interX1) * maxOf(0f, interY2 - interY1)
    return interArea / (areaA + areaB - interArea)
}