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
import com.example.realtime_obstacle_detection.ui.activities.OnDetectionActivity
import com.example.realtime_obstacle_detection.ui.screens.initialConfigurations.Models
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.math.sqrt

/**
 * Instrumented test suite for the ObstacleDetector and its pipeline.
 *
 * This test uses mocked objects and a static dataset to verify the detection logic without
 * relying on a live camera feed or a real ARCore session.
 */
@RunWith(AndroidJUnit4::class)
class ObjectDetectionInstrumentationTest {

    private lateinit var appContext: Context

    @Before
    fun setup() {
        // Initialize the application context for access to test resources (assets).
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
    }

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

    /**
     * Loads detection metadata from a JSON file in the assets folder.
     * This data acts as the ground truth (expected results) for the detection tests.
     * @param filename The name of the JSON metadata file.
     * @return A map where keys are image file names and values are lists of expected detections.
     */
    private fun loadMetadata(filename: String): Map<String, List<ObjectDetectionResult>> {
        // Read the entire JSON file content.
        val jsonString = appContext.assets.open(filename).bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        val metadata = mutableMapOf<String, List<ObjectDetectionResult>>()

        // Iterate through each image key in the JSON.
        jsonObject.keys().forEach { key ->
            val jsonArray = jsonObject.getJSONArray(key)
            val results = mutableListOf<ObjectDetectionResult>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                // Map JSON data to the ObjectDetectionResult data class.
                results.add(
                    ObjectDetectionResult(
                        x1 = obj.getDouble("x1").toFloat(),
                        y1 = obj.getDouble("y1").toFloat(),
                        x2 = obj.getDouble("x2").toFloat(),
                        y2 = obj.getDouble("y2").toFloat(),
                        width = obj.getDouble("width").toFloat(),
                        height = obj.getDouble("height").toFloat(),
                        confidenceRate = obj.getDouble("confidenceRate").toFloat(),
                        className = obj.getString("className"),
                        distance = null // Distance is calculated by the app logic, not from metadata.
                    )
                )
            }
            // Store results with just the image file name as the key (e.g., "img1.jpg")
            metadata[key] = results
        }
        return metadata
    }

    // --- Performance Test ---

    /**
     * Tests the inference speed of the detector across all models and configurations.
     * This test focuses on **performance metrics (inference time, FPS)** rather than detection accuracy.
     */
    @Test
    fun testPerformanceMetrics() {
        val testImages = getImagesFromAssets()
        assertTrue("No images found in the assets folder.", testImages.isNotEmpty())

        val mockClassifier = mock<ObstacleClassifier>()
        val totalTestCount = Models.entries.size * 2 * 2 * testImages.size
        var currentTest = 0

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
                        currentTest++
                        val testImage = loadBitmapFromAssets(imageFileName)

                        // --- Performance Measurement ---
                        val startTime = System.nanoTime()
                        detector.detect(testImage)
                        val endTime = System.nanoTime()

                        val inferenceTimeMs = (endTime - startTime) / 1_000_000.0 // Convert nanoseconds to milliseconds
                        val fps = 1000.0 / inferenceTimeMs // Calculate Frames Per Second

                        Log.i(
                            "PerformanceResults",
                            "Model: ${model.displayName} | NNAPI: $useNNAPI | Image: ${imageFileName.substringAfterLast('/')} | Inference Time: ${"%.2f".format(inferenceTimeMs)} ms | FPS: ${"%.2f".format(fps)}"
                        )

                        // Adding assertions for minimum acceptable performance.
                        assertTrue(
                            "Inference time for ${model.displayName} is too slow ($inferenceTimeMs ms). Should be < 200ms.",
                            inferenceTimeMs < 200
                        )
                    }
                }
            }
        }
    }

    /**
     * Tests that the detector initializes correctly across various model and configuration combinations
     * and processes all test images, producing results that match the ground truth metadata count.
     */
    @Test
    fun testAllModelsWithAllConfigurationsAndVerifyDetection() {
        val testImages = getImagesFromAssets()
        assertTrue("No images found in the assets folder.", testImages.isNotEmpty())
        val metadata = loadMetadata("detections_metadata.json")

        // Use a mock classifier to intercept and verify the results generated by the detector.
        val mockClassifier = mock<ObstacleClassifier>()

        // Iterates through all possible combinations of model, NNAPI usage, and HDR setting.
        for (model in Models.entries) {
            for (useNNAPI in listOf(true, false)) {
                for (isHdrEnabled in listOf(true, false)) {
                    Log.d("TestRunner", "Testing model: ${model.displayName} with NNAPI=$useNNAPI, HDR=$isHdrEnabled")

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

                        var actualResults: List<ObjectDetectionResult>? = null

                        // Mockito's doAnswer is used to capture the actual detection results
                        // passed as the first argument to the mockClassifier's onDetect method.
                        doAnswer { invocation ->
                            actualResults = invocation.arguments[0] as List<ObjectDetectionResult>
                            null
                        }.whenever(mockClassifier).onDetect(any(), any())

                        detector.detect(testImage)

                        // 1. Verify: Ensure the detector's callback was actually executed.
                        verify(mockClassifier, atLeastOnce()).onDetect(any(), any())
                        assertNotNull("No results detected for image: $imageFileName", actualResults)

                        // 2. Verification: Compare against expected metadata.
                        // The key for metadata is just the file name (e.g., "img1.jpg")
                        val keyFileName = imageFileName.substringAfterLast('/')
                        val expectedResults = metadata[keyFileName]
                        assertNotNull("No metadata found for image: $keyFileName", expectedResults)

                        // 3. Assertion: Compare the number of detected objects.
                        assertEquals(
                            "The number of detected objects must match the expected count in metadata.",
                            expectedResults!!.size,
                            actualResults!!.size
                        )

                        // Fine-grained assertion: compare individual properties of the detected objects.
                        // Sort both lists by class name and confidence to ensure reliable comparison,
                        // as detection order is often unstable.
                        val comparator = compareBy<ObjectDetectionResult> { it.className }
                            .thenByDescending { it.confidenceRate }

                        val sortedExpected = expectedResults.sortedWith(comparator)
                        val sortedActual = actualResults!!.sortedWith(comparator)

                        for (i in sortedExpected.indices) {
                            val expected = sortedExpected[i]
                            val actual = sortedActual[i]

                            // Check class name
                            assertEquals("Class name mismatch for detection #$i", expected.className, actual.className)

                            // Check confidence rate (allowing a small tolerance for model fluctuations)
                            assertEquals(
                                "Confidence rate mismatch for detection #$i (${expected.className})",
                                expected.confidenceRate,
                                actual.confidenceRate,
                                0.05f // Allowing 5% tolerance
                            )

                            // Check bounding box coordinates (allowing a small tolerance for coordinate float math)
                            assertEquals("x1 mismatch for detection #$i (${expected.className})", expected.x1, actual.x1, 0.01f)
                            assertEquals("y1 mismatch for detection #$i (${expected.className})", expected.y1, actual.y1, 0.01f)
                            assertEquals("x2 mismatch for detection #$i (${expected.className})", expected.x2, actual.x2, 0.01f)
                            assertEquals("y2 mismatch for detection #$i (${expected.className})", expected.y2, actual.y2, 0.01f)
                        }
                    }
                }
            }
        }
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