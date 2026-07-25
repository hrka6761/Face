package ir.hrka.face.engine.model

/**
 * Result of 1:N face identification against an in-memory gallery.
 *
 * @property personId Best-matching person id, or `null` when gallery is empty.
 * @property similarityScore Cosine similarity of the best match in roughly `[-1, 1]`.
 * @property isMatch `true` when best score clears [ir.hrka.face.engine.EngineConfig.matchThreshold]
 *   and the best-vs-second-best person margin.
 * @property matchedEmbeddingId Id of the gallery embedding that produced the best score.
 */
data class RecognitionResult(
    val personId: String?,
    val similarityScore: Float,
    val isMatch: Boolean,
    val matchedEmbeddingId: String? = null,
)
