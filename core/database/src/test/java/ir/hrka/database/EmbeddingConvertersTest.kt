package ir.hrka.database

import ir.hrka.database.util.EmbeddingConverters
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for embedding blob serialization.
 */
class EmbeddingConvertersTest {

    @Test
    fun roundTrip_preservesValues() {
        val original = floatArrayOf(0.1f, -0.25f, 1.5f, 0f)
        val bytes = EmbeddingConverters.toByteArray(original)
        val restored = EmbeddingConverters.toFloatArray(bytes)
        assertEquals(original.size, restored.size)
        assertArrayEquals(original, restored, 1e-6f)
    }
}
