package ir.hrka.face.engine

import ir.hrka.face.engine.alignment.FaceAligner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests for InsightFace 5-point similarity alignment math.
 *
 * Uses flattened float arrays (not Android PointF) so JVM unit tests do not
 * depend on Android framework stubs.
 *
 * TODO(model): add instrumented tests that warp a real face bitmap to 112×112
 * and assert eye positions land near ArcFace template coordinates.
 */
class FaceAlignerTest {

    @Test
    fun arcfaceDestinationLandmarks_112_matchesCanonicalTemplate() {
        val dst = FaceAligner.arcfaceDestinationLandmarkArray(112)
        assertEquals(10, dst.size)
        assertEquals(38.2946f, dst[0], 1e-3f)
        assertEquals(51.6963f, dst[1], 1e-3f)
        assertEquals(73.5318f, dst[2], 1e-3f)
    }

    @Test
    fun estimateSimilarityTransform_identityMapping_isNearIdentity() {
        val dst = FaceAligner.arcfaceDestinationLandmarkArray(112)
        val matrix = FaceAligner.estimateSimilarityTransform(dst, dst)
        assertNotNull(matrix)
        assertEquals(1f, matrix!![0], 1e-3f)
        assertEquals(0f, matrix[1], 1e-3f)
        assertEquals(0f, matrix[2], 1e-2f)
        assertEquals(0f, matrix[3], 1e-3f)
        assertEquals(1f, matrix[4], 1e-3f)
        assertEquals(0f, matrix[5], 1e-2f)
    }

    @Test
    fun estimateSimilarityTransform_translationOnly() {
        val src = floatArrayOf(
            0f, 0f,
            10f, 0f,
            0f, 10f,
            10f, 10f,
            5f, 5f,
        )
        val dst = FloatArray(src.size) { i ->
            if (i % 2 == 0) src[i] + 3f else src[i] + 4f
        }
        val matrix = FaceAligner.estimateSimilarityTransform(src, dst)
        assertNotNull(matrix)
        assertEquals(1f, matrix!![0], 1e-3f)
        assertEquals(3f, matrix[2], 1e-3f)
        assertEquals(4f, matrix[5], 1e-3f)
    }

    @Test
    fun estimateSimilarityTransform_rejectsDegenerateInput() {
        val one = floatArrayOf(1f, 1f)
        val result = FaceAligner.estimateSimilarityTransform(one, one)
        assertTrue(result == null)
    }
}
