package ir.hrka.face.recognition

import ir.hrka.face.model.EnrolledFace
import ir.hrka.face.model.FaceEmbedding
import ir.hrka.face.model.Person
import ir.hrka.face.recognition.internal.CosineFaceMatcher
import ir.hrka.face.recognition.internal.EmbeddingMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for embedding math and cosine matching.
 */
class EmbeddingMathTest {

    @Test
    fun l2Normalize_unitVectorHasNormOne() {
        val normalized = EmbeddingMath.l2Normalize(floatArrayOf(3f, 4f))
        val norm = kotlin.math.sqrt(
            (normalized[0] * normalized[0] + normalized[1] * normalized[1]).toDouble(),
        )
        assertEquals(1.0, norm, 1e-5)
    }

    @Test
    fun average_ofIdenticalVectors_staysNormalized() {
        val v = EmbeddingMath.l2Normalize(floatArrayOf(1f, 2f, 3f))
        val avg = EmbeddingMath.average(listOf(v, v, v))
        val norm = kotlin.math.sqrt(avg.sumOf { (it * it).toDouble() })
        assertEquals(1.0, norm, 1e-5)
    }

    @Test
    fun cosineSimilarity_identicalVectors_isOne() {
        val v = floatArrayOf(0.1f, 0.2f, 0.3f)
        assertEquals(1f, EmbeddingMath.cosineSimilarity(v, v), 1e-5f)
    }

    @Test
    fun matcher_usesBestTemplate_forPerson() {
        val person = Person("id-1", "Ada", 0L, 0L)
        val far = FaceEmbedding(EmbeddingMath.l2Normalize(floatArrayOf(0f, 1f, 0f)))
        val near = FaceEmbedding(EmbeddingMath.l2Normalize(floatArrayOf(1f, 0f, 0f)))
        val enrolled = listOf(
            EnrolledFace(person, far),
            EnrolledFace(person, near),
        )
        val matcher = CosineFaceMatcher()

        val result = matcher.match(
            query = FaceEmbedding(EmbeddingMath.l2Normalize(floatArrayOf(0.95f, 0.05f, 0f))),
            enrolled = enrolled,
            threshold = 0.55f,
            margin = 0f,
        )

        assertEquals("Ada", result.person?.name)
        assertTrue(result.similarity >= 0.55f)
    }

    @Test
    fun matcher_returnsNull_whenBelowThreshold() {
        val person = Person("id-1", "Ada", 0L, 0L)
        val embedding = FaceEmbedding(EmbeddingMath.l2Normalize(floatArrayOf(1f, 0f, 0f)))
        val enrolled = listOf(EnrolledFace(person, embedding))
        val matcher = CosineFaceMatcher()

        val result = matcher.match(
            query = FaceEmbedding(EmbeddingMath.l2Normalize(floatArrayOf(0f, 1f, 0f))),
            enrolled = enrolled,
            threshold = 0.55f,
            margin = 0f,
        )

        assertNull(result.person)
    }

    @Test
    fun matcher_rejectsAmbiguousMatch_whenMarginNotMet() {
        val ada = Person("id-1", "Ada", 0L, 0L)
        val bob = Person("id-2", "Bob", 0L, 0L)
        val enrolled = listOf(
            EnrolledFace(ada, FaceEmbedding(EmbeddingMath.l2Normalize(floatArrayOf(1f, 0f, 0f)))),
            EnrolledFace(bob, FaceEmbedding(EmbeddingMath.l2Normalize(floatArrayOf(0.9f, 0.1f, 0f)))),
        )
        val matcher = CosineFaceMatcher()

        val result = matcher.match(
            query = FaceEmbedding(EmbeddingMath.l2Normalize(floatArrayOf(0.95f, 0.05f, 0f))),
            enrolled = enrolled,
            threshold = 0.5f,
            margin = 0.2f,
        )

        assertNull(result.person)
    }
}
