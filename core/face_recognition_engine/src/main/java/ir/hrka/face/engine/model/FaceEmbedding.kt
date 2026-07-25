package ir.hrka.face.engine.model

/**
 * 512-D ArcFace embedding vector.
 *
 * @property id Stable unique id for this template (caller-defined).
 * @property personId Optional person / identity key.
 * @property vector Embedding values; expected size 512 and L2-normalized when [isNormalized] is true.
 * @property isNormalized Whether [vector] is already unit-length.
 */
data class FaceEmbedding(
    val id: String,
    val personId: String? = null,
    val vector: FloatArray,
    val isNormalized: Boolean = true,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FaceEmbedding) return false
        return id == other.id &&
            personId == other.personId &&
            isNormalized == other.isNormalized &&
            vector.contentEquals(other.vector)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (personId?.hashCode() ?: 0)
        result = 31 * result + vector.contentHashCode()
        result = 31 * result + isNormalized.hashCode()
        return result
    }

    override fun toString(): String =
        "FaceEmbedding(id=$id, personId=$personId, dim=${vector.size}, isNormalized=$isNormalized)"
}
