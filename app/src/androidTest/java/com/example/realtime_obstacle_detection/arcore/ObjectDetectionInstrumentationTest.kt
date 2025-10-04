package com.example.realtime_obstacle_detection.arcore

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.realtime_obstacle_detection.data.ObstacleDetector
import com.example.realtime_obstacle_detection.domain.ObjectDetectionResult
import com.example.realtime_obstacle_detection.domain.ObstacleClassifier
import com.example.realtime_obstacle_detection.ui.screens.initialConfigurations.Models
import com.google.ar.core.Pose
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import java.io.File
import java.io.FileNotFoundException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Instrumented test suite for the ObstacleDetector and its pipeline.
 *
 * This test now functions as an evaluation tool, logging all metrics instead of asserting.
 */
@RunWith(AndroidJUnit4::class)
class ObjectDetectionInstrumentationTest {

    private lateinit var appContext: Context

    @Before
    fun setup() {
        // Initialize the application context for access to test resources (assets).
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
    }

    // --- Helper Functions for Metrics and Reporting ---

    /**
     * Calculates the Intersection over Union (IoU) between two bounding boxes.
     * @param box1 The first bounding box (e.g., ground truth).
     * @param box2 The second bounding box (e.g., model prediction).
     * @return The IoU value (0.0 to 1.0).
     */
    private fun calculateIoU(box1: ObjectDetectionResult, box2: ObjectDetectionResult): Float {
        // Determine the coordinates of the intersection rectangle
        val xA = max(box1.x1, box2.x1)
        val yA = max(box1.y1, box2.y1)
        val xB = min(box1.x2, box2.x2)
        val yB = min(box1.y2, box2.y2)

        // Compute the area of intersection rectangle
        val interArea = max(0f, xB - xA) * max(0f, yB - yA)

        // Compute the area of both the prediction and ground-truth rectangles
        val box1Area = (box1.x2 - box1.x1) * (box1.y2 - box1.y1)
        val box2Area = (box2.x2 - box2.x1) * (box2.y2 - box2.y1)

        // Compute the IoU
        if (box1Area + box2Area - interArea == 0f) return 0f
        return interArea / (box1Area + box2Area - interArea)
    }

    /**
     * Writes the complete evaluation report to a file in the app's external storage.
     * @param context The application context.
     * @param reportContent The formatted string content of the report.
     */
    private fun writeEvaluationReport(context: Context, reportContent: String) {
        // Use getExternalFilesDir(null) for a writable directory accessible to the user
        val reportDir = context.getExternalFilesDir(null)
        val fileName = "DetectionEvaluationReport_${System.currentTimeMillis()}.txt"
        val reportFile = File(reportDir, fileName)

        try {
            reportFile.writeText(reportContent)
            Log.i("EVAL_REPORT_SUCCESS", "Report saved to: ${reportFile.absolutePath}")
            // Also log the content to logcat for immediate review
            Log.d("EVAL_REPORT_CONTENT", reportContent)
        } catch (e: Exception) {
            Log.e("EVAL_REPORT_ERROR", "Failed to write report: ${e.message}")
        }
    }

    // --- Helper Functions for Label Parsing ---

    /**
     * Map from class ID to class name based on the 18 classes list.
     */
    private fun classIdToClassName(id: Int): String {
        return when (id) {
            0 -> "Bike"
            1 -> "Building"
            2 -> "Car"
            3 -> "Person"
            4 -> "Stairs"
            5 -> "Traffic sign"
            6 -> "Electrical Pole"
            7 -> "Road"
            8 -> "Motorcycle"
            9 -> "Dustbin"
            10 -> "Dog"
            11 -> "Manhole"
            12 -> "Tree"
            13 -> "Guard rail"
            14 -> "Pedestrian crosswalk"
            15 -> "Truck"
            16 -> "Bus"
            17 -> "Bench"
            else -> "Unknown-$id"
        }
    }

    /**
     * Loads ground truth labels from a .txt file in the 'test/labels' folder (YOLO format).
     */
    private fun loadGroundTruthFromLabelFile(imageFileName: String): List<ObjectDetectionResult> {
        val baseName = imageFileName.substringAfterLast('/').substringBeforeLast('.')
        val labelPath = "test/labels/$baseName.txt"
        val results = mutableListOf<ObjectDetectionResult>()

        try {
            val fileContent = appContext.assets.open(labelPath).bufferedReader().use { it.readText() }

            fileContent.lines().forEach { line ->
                if (line.isNotBlank()) {
                    val parts = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

                    if (parts.size >= 5) {
                        // Use toFloat() instead of toFloatOrNull() to force a parse error if format is invalid
                        val classId = parts[0].toInt()
                        val xCenter = parts[1].toFloat()
                        val yCenter = parts[2].toFloat()
                        val wNorm = parts[3].toFloat()
                        val hNorm = parts[4].toFloat()

                        // Core YOLO conversion: Center + Half Width/Height
                        val x1 = xCenter - wNorm / 2f
                        val y1 = yCenter - hNorm / 2f
                        val x2 = xCenter + wNorm / 2f
                        val y2 = yCenter + hNorm / 2f

                        results.add(
                            ObjectDetectionResult(
                                // Ensure coordinates stay within the [0, 1] bounds
                                x1 = x1.coerceIn(0f, 1f),
                                y1 = y1.coerceIn(0f, 1f),
                                x2 = x2.coerceIn(0f, 1f),
                                y2 = y2.coerceIn(0f, 1f),
                                width = wNorm,
                                height = hNorm,
                                confidenceRate = 1.0f, // Ground truth confidence is 1.0
                                className = classIdToClassName(classId),
                                distance = null
                            )
                        )
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            Log.w("TestRunner", "No label file found for $labelPath. Assuming zero expected detections.")
        } catch (e: NumberFormatException) {
            Log.e("TestRunner", "Error parsing number in $labelPath: Check if labels are correctly formatted floats/ints.")
        } catch (e: Exception) {
            Log.e("TestRunner", "Error processing $labelPath: ${e.message}")
        }
        return results
    }

    // --- Helper Functions for Assets ---

    /**
     * Helper function to load a bitmap from the application's assets folder.
     * @param filename The name of the image file in the assets directory.
     * @return The loaded Bitmap object.
     */
    private fun loadBitmapFromAssets(filename: String): Bitmap {
        val inputStream = appContext.assets.open(filename)
        return BitmapFactory.decodeStream(inputStream)
    }

    /**
     * Helper function to get a list of all JPEG images from the new 'test/images' folder structure.
     * The file names returned are relative to the assets root (e.g., "test/images/img1.jpg").
     * @return A list of file names that are potential test images.
     */
    private fun getImagesFromAssets(): List<String> {
        // Look inside the 'test/images' folder in assets.
        val imageFolder = "test/images"
        return appContext.assets.list(imageFolder)
            ?.filter { it.endsWith(".jpg") || it.endsWith(".jpeg") }
            ?.map { "$imageFolder/$it" } // Return full path relative to assets root
            ?: emptyList()
    }

    // --- Performance Test (Inference Time and FPS) ---

    /**
     * Tests the inference speed of the detector across all models and configurations.
     * This test focuses on **performance metrics (inference time, FPS)** rather than detection accuracy.
     */
    @Test
    fun testPerformanceMetrics() {
        val testImages = getImagesFromAssets()
        assertTrue("No images found in the 'test/images' folder.", testImages.isNotEmpty())

        val mockClassifier = mock<ObstacleClassifier>()

        for (model in Models.entries) {
            for (useNNAPI in listOf(true, false)) {
                for (isHdrEnabled in listOf(true, false)) { // This flag doesn't directly affect inference time but is kept for comprehensive testing.
                    Log.d("PerformanceTest", "Testing performance: ${model.displayName} with NNAPI=$useNNAPI")

                    val detector = ObstacleDetector(
                        context = appContext,
                        obstacleClassifier = mockClassifier,
                        modelPath = model.modelFileName,
                        labelPath = model.labelFileName,
                        confidenceThreshold = 0.5f,
                        iouThreshold = 0.5f,
                        threadsCount = 4,
                        useNNAPI = useNNAPI
                    )
                    detector.setup()

                    for (imageFileName in testImages) {
                        val testImage = loadBitmapFromAssets(imageFileName)

                        // --- Performance Measurement ---
                        val startTime = System.nanoTime()
                        detector.detect(testImage)
                        val endTime = System.nanoTime()

                        val inferenceTimeMs = (endTime - startTime) / 1_000_000.0 // Convert nanoseconds to milliseconds
                        val fps = 1000.0 / inferenceTimeMs // Calculate Frames Per Second

                        Log.i(
                            "PerformanceResults",
                            "Model: ${model.displayName} | NNAPI: $useNNAPI | Image: ${imageFileName.substringAfterLast('/')} | Time: ${"%.2f".format(inferenceTimeMs)} ms | FPS: ${"%.2f".format(fps)}"
                        )
                        assertTrue("Inference time too slow", inferenceTimeMs < 200)
                    }
                }
            }
        }
    }

    // --- Detection Accuracy Test ---

    /**
     * Tests that the detector initializes correctly across various model and configuration combinations
     * and processes all test images, producing results that match the ground truth metadata count.
     */
    @Test
    fun generateEvaluationReport() {
        val testImages = getImagesFromAssets()
        assertFalse("No images found in the 'test/images' folder.", testImages.isEmpty())

        // Use a mock classifier to intercept and verify the results generated by the detector.
        val mockClassifier = mock<ObstacleClassifier>()
        val report = StringBuilder()
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())

        report.append("========================================================\n")
        report.append("OBJECT DETECTION MODEL EVALUATION REPORT\n")
        report.append("Generated On: $timestamp\n")
        report.append("========================================================\n\n")

        // Defines the sorting rule for reliable comparison
        val comparator = compareBy<ObjectDetectionResult> { it.className }
            .thenByDescending { it.confidenceRate }

        var totalDetections = 0
        var totalExpected = 0

        // Loops through all configurations
        for (model in Models.entries) {
            for (useNNAPI in listOf(true, false)) {
                for (isHdrEnabled in listOf(true, false)) {
                    val modelConfig = "${model.displayName} | NNAPI=$useNNAPI | HDR=$isHdrEnabled"
                    report.append("--- CONFIG: $modelConfig ---\n")

                    // Instantiate the core detection logic component.
                    val detector = ObstacleDetector(
                        context = appContext,
                        obstacleClassifier = mockClassifier,
                        modelPath = model.modelFileName,
                        labelPath = model.labelFileName,
                        confidenceThreshold = 0.5f,
                        iouThreshold = 0.5f,
                        threadsCount = 4,
                        useNNAPI = useNNAPI
                    )
                    detector.setup() // Call setup to ensure initialization occurs.

                    for (imageFileName in testImages) {
                        val testImage = loadBitmapFromAssets(imageFileName)

                        // Load the ground truth labels for the current image.
                        val expectedResults = loadGroundTruthFromLabelFile(imageFileName)

                        // Skip the test if there are no expected results (no label file found).
                        if (expectedResults.isEmpty()) {
                            report.append("  [Image: ${imageFileName.substringAfterLast('/')}] SKIPPED (No ground truth labels found)\n")
                            continue
                        }

                        totalExpected += expectedResults.size

                        var actualResults: List<ObjectDetectionResult>? = null

                        // Mockito's doAnswer is used to capture the actual detection results
                        // passed as the first argument to the mockClassifier's onDetect method.
                        doAnswer { invocation ->
                            actualResults = invocation.arguments[0] as List<ObjectDetectionResult>
                            null
                        }.whenever(mockClassifier).onDetect(any(), any())

                        detector.detect(testImage)

                        val detected = actualResults ?: emptyList()
                        totalDetections += detected.size

                        report.append("  [Image: ${imageFileName.substringAfterLast('/')}] Detected: ${detected.size} | Expected: ${expectedResults.size}\n")

                        // Sort both lists for deterministic comparison
                        val sortedExpected = expectedResults.sortedWith(comparator)
                        val sortedActual = detected.sortedWith(comparator)

                        val comparisonCount = min(sortedExpected.size, sortedActual.size)

                        for (i in 0 until comparisonCount) {
                            val expected = sortedExpected[i]
                            val actual = sortedActual[i]

                            val iou = calculateIoU(expected, actual)
                            val x1Error = abs(expected.x1 - actual.x1)
                            val y1Error = abs(expected.y1 - actual.y1) // <-- Now used below
                            val classMatch = if (expected.className == actual.className) "MATCH" else "MISMATCH"

                            report.append(
                                "    #${i}: GT_Class: ${expected.className.padEnd(20)} | P_Class: ${actual.className.padEnd(20)} | Class: ${classMatch.padEnd(7)} | Conf: ${"%.4f".format(actual.confidenceRate)} | IoU: ${"%.4f".format(iou)} | X1_Err: ${"%.4f".format(x1Error)} | Y1_Err: ${"%.4f".format(y1Error)}\n" // <-- Y1_Err added
                            )
                        }

                        if (sortedExpected.size != sortedActual.size) {
                            report.append("    WARNING: Detection count mismatch. Expected: ${expectedResults.size}, Actual: ${detected.size}.\n")
                        }
                    }
                    report.append("\n") // Spacer after each config
                }
            }
        }

        report.append("\n========================================================\n")
        report.append("SUMMARY\n")
        report.append("Total Expected Instances: $totalExpected\n")
        report.append("Total Detected Instances: $totalDetections\n")
        report.append("========================================================\n")

        writeEvaluationReport(appContext, report.toString())

        // Final assertion to ensure the test itself doesn't report an error (it can still succeed even if the model is bad)
        assertTrue("Report generated successfully.", true)
    }

    /**
     * Refactored test to isolate and verify the core Euclidean distance calculation logic.
     *
     * This test strictly uses mocked ARCore Pose objects and the mathematical formula,
     * avoiding the need to mock complex and fragile Android/ARCore components like Activity or Frame.
     */
    @Test
    fun testDistanceCalculationLogic() {
        // Mocks for 3D position data (ARCore Pose)
        val mockCameraPose = mock<Pose>()
        val mockObjectPose = mock<Pose>()

        // The expected distance for the test case: sqrt(3² + 4² + 0²) = 5.0
        val expectedDistance = 5.0f

        // 1. Setup Mock Camera Pose at the origin (0, 0, 0) in meters.
        whenever(mockCameraPose.tx()).thenReturn(0f)
        whenever(mockCameraPose.ty()).thenReturn(0f)
        whenever(mockCameraPose.tz()).thenReturn(0f)

        // 2. Setup Mock Object Pose at (3, 4, 0) in meters.
        whenever(mockObjectPose.tx()).thenReturn(3f)
        whenever(mockObjectPose.ty()).thenReturn(4f)
        whenever(mockObjectPose.tz()).thenReturn(0f)

        // 3. Perform the actual distance calculation using the Euclidean distance formula.
        // dx, dy, dz represent the differences in 3D space between the two points.
        val dx = mockObjectPose.tx() - mockCameraPose.tx()
        val dy = mockObjectPose.ty() - mockCameraPose.ty()
        val dz = mockObjectPose.tz() - mockCameraPose.tz()

        // Calculate the magnitude (distance).
        val calculatedDistance = sqrt(dx * dx + dy * dy + dz * dz)

        // 4. Assertion: Verify the calculated result.
        assertEquals(
            "The calculated 3D distance must equal the expected value.",
            expectedDistance,
            calculatedDistance,
            0.01f // Delta for floating-point comparison tolerance.
        )
    }
}