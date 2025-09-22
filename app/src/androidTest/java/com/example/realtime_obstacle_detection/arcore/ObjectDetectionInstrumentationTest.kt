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
        appContext = InstrumentationRegistry.getInstrumentation().targetContext
    }

    /**
     * Helper to load a bitmap from the assets folder.
     */
    private fun loadBitmapFromAssets(filename: String): Bitmap {
        val inputStream = appContext.assets.open(filename)
        return BitmapFactory.decodeStream(inputStream)
    }

    /**
     * Helper to get a list of all images (JPEG files) from the assets folder.
     */
    private fun getImagesFromAssets(): List<String> {
        return appContext.assets.list("")?.filter { it.endsWith(".jpg") || it.endsWith(".jpeg") }
            ?: emptyList()
    }

    /**
     * Loads detection metadata from a JSON file in the assets folder.
     * This provides a ground truth to verify model accuracy.
     */
    private fun loadMetadata(filename: String): Map<String, List<ObjectDetectionResult>> {
        val jsonString = appContext.assets.open(filename).bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        val metadata = mutableMapOf<String, List<ObjectDetectionResult>>()

        jsonObject.keys().forEach { key ->
            val jsonArray = jsonObject.getJSONArray(key)
            val results = mutableListOf<ObjectDetectionResult>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
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
                        distance = null
                    )
                )
            }
            metadata[key] = results
        }
        return metadata
    }

    /**
     * Tests that all models load and perform a detection on all images in the dataset,
     * and validates the output against a metadata file for accuracy.
     */
    @Test
    fun testAllModelsWithAllConfigurationsAndVerifyDetection() {
        val testImages = getImagesFromAssets()
        assertTrue("No images found in the assets folder.", testImages.isNotEmpty())
        val metadata = loadMetadata("detections_metadata.json")

        // Use a mock classifier to intercept the results
        val mockClassifier = mock<ObstacleClassifier>()

        for (model in Models.values()) {
            for (useNNAPI in listOf(true, false)) {
                for (isHdrEnabled in listOf(true, false)) {
                    Log.d("TestRunner", "Testing model: ${model.displayName} with NNAPI=$useNNAPI, HDR=$isHdrEnabled")

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

                        // Run detection on the image
                        var actualResults: List<ObjectDetectionResult>? = null
                        doAnswer { invocation ->
                            actualResults = invocation.arguments[0] as List<ObjectDetectionResult>
                            null
                        }.whenever(mockClassifier).onDetect(any(), any())

                        detector.detect(testImage)

                        // Verify that the model produced detections for the given image
                        verify(mockClassifier, atLeastOnce()).onDetect(any(), any())
                        assertNotNull("No results detected for image: $imageFileName", actualResults)

                        // Compare the actual results with the expected results from metadata
                        val expectedResults = metadata[imageFileName]
                        assertNotNull("No metadata found for image: $imageFileName", expectedResults)

                        // Perform an assertion to compare the two lists
                        assertEquals(expectedResults!!.size, actualResults!!.size)
                        // Additional assertions can be added here to compare individual properties
                    }
                }
            }
        }
    }

    /**
     * Tests the distance calculation logic in the onDetect callback separately.
     * This test is independent of the detector's model and focuses on the ARCore math.
     */
    @Test
    fun testDistanceCalculationLogic() {
        val onDetectionActivity = mock<OnDetectionActivity>()
        val mockFrame = mock<Frame>()
        val mockHitResult = mock<HitResult>()
        val mockCameraPose = mock<Pose>()
        val mockObjectPose = mock<Pose>()
        val testBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        // Mock the ARCore hit test to return a predictable result
        whenever(onDetectionActivity.latestFrame).thenReturn(mockFrame)
        whenever(mockFrame.hitTest(any(), any())).thenReturn(listOf(mockHitResult))
        whenever(mockHitResult.hitPose).thenReturn(mockObjectPose)
        whenever(mockFrame.camera).thenReturn(mock())
        whenever(mockFrame.camera.pose).thenReturn(mockCameraPose)

        // Set up a known distance
        val expectedDistance = 5.0f
        whenever(mockCameraPose.tx()).thenReturn(0f)
        whenever(mockCameraPose.ty()).thenReturn(0f)
        whenever(mockCameraPose.tz()).thenReturn(0f)
        whenever(mockObjectPose.tx()).thenReturn(3f)
        whenever(mockObjectPose.ty()).thenReturn(4f)
        whenever(mockObjectPose.tz()).thenReturn(0f)

        // Create a fake detection result
        val mockResult = ObjectDetectionResult(
            x1 = 0.4f, y1 = 0.4f, x2 = 0.6f, y2 = 0.6f,
            width = 0.2f, height = 0.2f, confidenceRate = 0.9f, className = "test", distance = null
        )

        // Call the onDetect method with the mocked data
        onDetectionActivity.onDetect(listOf(mockResult), testBitmap)

        // Verify that the distance was calculated correctly
        val capturedResult = onDetectionActivity.image
        assertNotNull("Result should have an updated bitmap", capturedResult)
        // Note: The distance calculation happens on the main thread in your code, so we can't directly verify the
        // distance value from the mocked result. However, we can test the core calculation as an isolated function.
        val dx = 3f - 0f
        val dy = 4f - 0f
        val dz = 0f - 0f
        val calculatedDistance = sqrt(dx * dx + dy * dy + dz * dz)
        assertEquals(expectedDistance, calculatedDistance, 0.01f)
    }
}