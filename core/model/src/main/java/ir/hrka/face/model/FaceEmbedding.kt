package ir.hrka.face.model

/**
 * L2-normalized face embedding vector produced by MobileFaceNet.
 *
 * @property values Embedding components (typically 192-dimensional).
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
