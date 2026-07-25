package ir.hrka.face.model

/**
 * L2-normalized face embedding vector.
 *
 * Current engine (ArcFace / buffalo_l) produces 512-dimensional vectors.
 *
 * @property values Embedding components.
 */
data class FaceEmbedding(
    val values: FloatArray,
) {
    /**
     * Embedding dimensionality.
     */
    val dimension: Int get() = values.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceEmbedding) return false
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int = values.contentHashCode()
}
