package ir.hrka.face.engine

import ir.hrka.face.engine.model.FaceEmbedding
import ir.hrka.face.engine.recognition.CosineSimilarityCalculator
import ir.hrka.face.engine.recognition.FaceRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests for cosine matching / threshold / margin behavior.
 */
class CosineSimilarityCalculatorTest {

    @Test
    fun identicalNormalizedVectors_haveSimilarityOne() {
        val v = floatArrayOf(0.6f, 0.8f)
        val sim = CosineSimilarityCalculator.similarity(v, v)
        assertEquals(1f, sim, 1e-5f)
    }

    @Test
    fun orthogonalVectors_haveSimilarityZero() {
        val a = floatArrayOf(1f, 0f)
        val b = floatArrayOf(0f, 1f)
        assertEquals(0f, CosineSimilarityCalculator.similarity(a, b), 1e-5f)
    }

    @Test
    fun recognizer_acceptsAboveThresholdWithMargin() {
        val config = EngineConfig(matchThreshold = 0.5f, matchMargin = 0.1f)
        val recognizer = FaceRecognizer(config)
        val query = emb("q", "probe", floatArrayOf(1f, 0f))
        val db = listOf(
            emb("1", "alice", floatArrayOf(0.95f, 0.3122f)), // ~0.95
            emb("2", "bob", floatArrayOf(0.6f, 0.8f)), // 0.6
        )
        val result = recognizer.recognizeBlocking(query, db)
        assertTrue(result.isMatch)
        assertEquals("alice", result.personId)
        assertTrue(result.similarityScore >= 0.5f)
    }

    @Test
    fun recognizer_rejectsWhenMarginTooSmall() {
        val config = EngineConfig(matchThreshold = 0.4f, matchMargin = 0.2f)
        val recognizer = FaceRecognizer(config)
        val query = emb("q", null, floatArrayOf(1f, 0f))
        val db = listOf(
            emb("1", "alice", floatArrayOf(0.9f, 0.4359f)), // ~0.90
            emb("2", "bob", floatArrayOf(0.85f, 0.5268f)), // ~0.85
        )
        val result = recognizer.recognizeBlocking(query, db)
        assertFalse(result.isMatch)
        assertEquals("alice", result.personId)
    }

    @Test
    fun recognizer_emptyGallery_returnsNoMatch() {
        val result = FaceRecognizer(EngineConfig()).recognizeBlocking(
            emb("q", null, floatArrayOf(1f, 0f)),
            emptyList(),
        )
        assertFalse(result.isMatch)
        assertEquals(null, result.personId)
        assertEquals(0f, result.similarityScore, 0f)
    }

    private fun emb(id: String, personId: String?, v: FloatArray): FaceEmbedding =
        FaceEmbedding(id = id, personId = personId, vector = v, isNormalized = true)
}
