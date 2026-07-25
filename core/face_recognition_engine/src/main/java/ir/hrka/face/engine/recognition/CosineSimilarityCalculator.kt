package ir.hrka.face.engine.recognition

import kotlin.math.sqrt

/**
 * Cosine similarity helpers for L2-normalized (or raw) embedding vectors.
 */
internal object CosineSimilarityCalculator {

    /**
     * Cosine similarity between [a] and [b].
     *
     * When both vectors are already L2-normalized this equals the dot product.
     * Returns `0` when either vector is empty or has zero norm.
     */
    fun similarity(a: FloatArray, b: FloatArray): Float {
        val size = minOf(a.size, b.size)
        if (size == 0) return 0f

        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in 0 until size) {
            val av = a[i]
            val bv = b[i]
            dot += av * bv
            normA += av * av
            normB += bv * bv
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
    }

    /**
     * Dot product (preferable when both vectors are known L2-normalized).
     */
    fun dot(a: FloatArray, b: FloatArray): Float {
        val size = minOf(a.size, b.size)
        var sum = 0f
        for (i in 0 until size) {
            sum += a[i] * b[i]
        }
        return sum
    }
}
