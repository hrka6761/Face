package ir.hrka.face.model

/**
 * Result of matching a query embedding against enrolled identities.
 *
 * @property person Matched person, or `null` when below the similarity threshold.
 * @property similarity Cosine similarity score in `[0, 1]` (after L2 normalization).
 */
data class FaceMatchResult(
    val person: Person?,
    val similarity: Float,
)

/**
 * An enrolled embedding paired with its owner for in-memory matching.
 *
 * @property person Owner identity.
 * @property embedding Stored face embedding.
 */
data class EnrolledFace(
    val person: Person,
    val embedding: FaceEmbedding,
)
