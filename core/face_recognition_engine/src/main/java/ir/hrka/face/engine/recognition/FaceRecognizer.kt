package ir.hrka.face.engine.recognition

import ir.hrka.face.engine.EngineConfig
import ir.hrka.face.engine.embedding.ArcFaceW600kR50Embedder
import ir.hrka.face.engine.model.FaceEmbedding
import ir.hrka.face.engine.model.RecognitionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 1:N face identification using cosine similarity with threshold + person margin.
 *
 * Supports multiple gallery embeddings per `personId`. The best score per person is
 * used; a match is accepted only when:
 * 1) best person score ≥ [EngineConfig.matchThreshold]
 * 2) best − secondBest ≥ [EngineConfig.matchMargin] (when a second person exists)
 */
internal class FaceRecognizer(
    private val config: EngineConfig,
) {

    /**
     * Compares [embedding] against [database] and returns the best candidate.
     *
     * Always returns the top candidate's score; [RecognitionResult.isMatch] encodes
     * threshold/margin acceptance. [RecognitionResult.personId] is null only when
     * the gallery is empty.
     */
    suspend fun recognize(
        embedding: FaceEmbedding,
        database: List<FaceEmbedding>,
    ): RecognitionResult = withContext(Dispatchers.Default) {
        recognizeBlocking(embedding, database)
    }

    /**
     * Blocking variant for unit tests.
     */
    fun recognizeBlocking(
        embedding: FaceEmbedding,
        database: List<FaceEmbedding>,
    ): RecognitionResult {
        if (database.isEmpty()) {
            return RecognitionResult(
                personId = null,
                similarityScore = 0f,
                isMatch = false,
                matchedEmbeddingId = null,
            )
        }

        val query = ensureNormalized(embedding)

        data class PersonScore(
            val personId: String?,
            val score: Float,
            val embeddingId: String,
        )

        val bestByPerson = LinkedHashMap<String, PersonScore>()
        for (candidate in database) {
            val key = candidate.personId ?: candidate.id
            val score = CosineSimilarityCalculator.similarity(
                query.vector,
                ensureNormalized(candidate).vector,
            )
            val prev = bestByPerson[key]
            if (prev == null || score > prev.score) {
                bestByPerson[key] = PersonScore(candidate.personId, score, candidate.id)
            }
        }

        var best: PersonScore? = null
        var second = Float.NEGATIVE_INFINITY
        for (ps in bestByPerson.values) {
            val currentBest = best
            when {
                currentBest == null || ps.score > currentBest.score -> {
                    second = currentBest?.score ?: Float.NEGATIVE_INFINITY
                    best = ps
                }
                ps.score > second -> second = ps.score
            }
        }

        val top = checkNotNull(best)
        val marginOk = second == Float.NEGATIVE_INFINITY ||
            (top.score - second) >= config.matchMargin
        val isMatch = top.score >= config.matchThreshold && marginOk

        return RecognitionResult(
            personId = top.personId,
            similarityScore = top.score,
            isMatch = isMatch,
            matchedEmbeddingId = top.embeddingId,
        )
    }

    private fun ensureNormalized(embedding: FaceEmbedding): FaceEmbedding {
        if (embedding.isNormalized) return embedding
        return embedding.copy(
            vector = ArcFaceW600kR50Embedder.l2Normalize(embedding.vector),
            isNormalized = true,
        )
    }
}
