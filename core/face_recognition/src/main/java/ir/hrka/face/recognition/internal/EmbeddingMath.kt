package ir.hrka.face.recognition.internal

import kotlin.math.sqrt

/**
 * Math helpers for face embedding vectors.
 */
internal object EmbeddingMath {

    /**
     * Returns an L2-normalized copy of [values].
     *
     * If the vector has zero norm, the original values are copied unchanged.
     *
     * @param values Source embedding.
     * @return Normalized float array (new instance).
     */
    fun l2Normalize(values: FloatArray): FloatArray {
        var sumSquares = 0f
        for (v in values) {
            sumSquares += v * v
        }
        val norm = sqrt(sumSquares)
        if (norm == 0f) {
            return values.copyOf()
        }
        return FloatArray(values.size) { i -> values[i] / norm }
    }

    /**
     * Cosine similarity between two vectors.
     *
     * @param a First vector.
     * @param b Second vector.
     * @return Similarity in roughly `[-1, 1]`; `0` when either vector is empty/zero.
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val size = minOf(a.size, b.size)
        if (size == 0) return 0f

        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in 0 until size) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
    }

    /**
     * Averages several embeddings and L2-normalizes the result (template centroid).
     *
     * @param embeddings Non-empty list of same-dimensional vectors.
     * @return Centroid embedding.
     */
    fun average(embeddings: List<FloatArray>): FloatArray {
        require(embeddings.isNotEmpty()) { "embeddings must not be empty" }
        val dim = embeddings.first().size
        val sum = FloatArray(dim)
        for (emb in embeddings) {
            require(emb.size == dim) { "All embeddings must share the same dimension" }
            for (i in 0 until dim) {
                sum[i] += emb[i]
            }
        }
        val inv = 1f / embeddings.size
        for (i in 0 until dim) {
            sum[i] *= inv
        }
        return l2Normalize(sum)
    }
}
