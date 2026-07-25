package ir.hrka.face.engine

import ir.hrka.face.engine.embedding.ArcFaceW600kR50Embedder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Pure unit tests for ArcFace embedding post-processing.
 *
 * TODO(model): add instrumented tests that load `arcface_w600k_r50.onnx`,
 * embed a 112×112 crop, and assert `vector.size == 512` and `L2 ≈ 1`.
 */
class ArcFaceEmbedderTest {

    @Test
    fun l2Normalize_unitVectorUnchanged() {
        val v = floatArrayOf(0.6f, 0.8f)
        val n = ArcFaceW600kR50Embedder.l2Normalize(v)
        assertEquals(0.6f, n[0], 1e-5f)
        assertEquals(0.8f, n[1], 1e-5f)
        assertEquals(1f, ArcFaceW600kR50Embedder.l2Norm(n), 1e-5f)
    }

    @Test
    fun l2Normalize_scalesToUnitLength() {
        val v = FloatArray(EngineConfig.EMBEDDING_DIM) { i -> (i + 1).toFloat() }
        val n = ArcFaceW600kR50Embedder.l2Normalize(v)
        assertEquals(EngineConfig.EMBEDDING_DIM, n.size)
        assertTrue(abs(ArcFaceW600kR50Embedder.l2Norm(n) - 1f) < 1e-5f)
    }

    @Test
    fun l2Normalize_zeroVector_returnsCopy() {
        val v = FloatArray(8) { 0f }
        val n = ArcFaceW600kR50Embedder.l2Normalize(v)
        assertEquals(0f, ArcFaceW600kR50Embedder.l2Norm(n), 0f)
        assertTrue(n !== v)
    }
}
