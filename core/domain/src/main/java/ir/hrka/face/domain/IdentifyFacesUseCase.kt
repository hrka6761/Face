package ir.hrka.face.domain

import ir.hrka.face.data.repository.PersonRepository
import ir.hrka.face.model.FaceEmbedding
import ir.hrka.face.model.FaceMatchResult
import ir.hrka.face.model.Person
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Identifies faces by comparing query embeddings against enrolled identity templates.
 *
 * For each enrolled person, the **maximum** cosine similarity across all of that person's
 * stored templates is used. A match is accepted only when the best score clears
 * [threshold] and beats the second-best person by at least [margin].
 *
 * @property personRepository Source of enrolled embeddings.
 */
class IdentifyFacesUseCase @Inject constructor(
    private val personRepository: PersonRepository,
) {

    /**
     * Matches each query embedding to the best enrolled person when above [threshold]
     * and [margin].
     *
     * @param embeddings Map of tracking id → query embedding.
     * @param threshold Minimum cosine similarity required for a positive match.
     * @param margin Minimum gap between best and second-best person scores.
     * @return Map of tracking id → [FaceMatchResult].
     */
    suspend operator fun invoke(
        embeddings: Map<Int, FaceEmbedding>,
        threshold: Float = DEFAULT_THRESHOLD,
        margin: Float = DEFAULT_MARGIN,
    ): Map<Int, FaceMatchResult> {
        if (embeddings.isEmpty()) return emptyMap()

        val enrolled = personRepository.getEnrolledFaces()
        if (enrolled.isEmpty()) {
            return embeddings.mapValues { FaceMatchResult(person = null, similarity = 0f) }
        }

        val byPerson = enrolled.groupBy { it.person.id }

        return embeddings.mapValues { (_, query) ->
            var bestPerson: Person? = null
            var bestScore = Float.NEGATIVE_INFINITY
            var secondBest = Float.NEGATIVE_INFINITY

            for ((_, templates) in byPerson) {
                var personBest = Float.NEGATIVE_INFINITY
                val person = templates.first().person
                for (template in templates) {
                    val score = cosineSimilarity(query.values, template.embedding.values)
                    if (score > personBest) personBest = score
                }
                if (personBest > bestScore) {
                    secondBest = bestScore
                    bestScore = personBest
                    bestPerson = person
                } else if (personBest > secondBest) {
                    secondBest = personBest
                }
            }

            val accepted = bestScore >= threshold &&
                (secondBest == Float.NEGATIVE_INFINITY || bestScore - secondBest >= margin)

            if (accepted) {
                FaceMatchResult(person = bestPerson, similarity = bestScore)
            } else {
                FaceMatchResult(person = null, similarity = bestScore.coerceAtLeast(0f))
            }
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
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
        return if (denom == 0f) 0f else (dot / denom)
    }

    companion object {
        /**
         * Cosine threshold for ArcFace (buffalo_l / w600k_r50) galleries.
         * Keep in sync with [ir.hrka.face.engine.EngineConfig.DEFAULT_MATCH_THRESHOLD].
         */
        const val DEFAULT_THRESHOLD: Float = 0.42f

        /**
         * Best-vs-second-best margin to reject ambiguous identities.
         * Keep in sync with [ir.hrka.face.engine.EngineConfig.DEFAULT_MATCH_MARGIN].
         */
        const val DEFAULT_MARGIN: Float = 0.08f
    }
}
