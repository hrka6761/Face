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
 * stored templates is used. This is the standard multi-gallery approach that makes
 * recognition robust to distance and slight pose changes.
 *
 * @property personRepository Source of enrolled embeddings.
 */
class IdentifyFacesUseCase @Inject constructor(
    private val personRepository: PersonRepository,
) {

    /**
     * Matches each query embedding to the best enrolled person when above [threshold].
     *
     * @param embeddings Map of tracking id → query embedding.
     * @param threshold Minimum cosine similarity required for a positive match.
     * @return Map of tracking id → [FaceMatchResult].
     */
    suspend operator fun invoke(
        embeddings: Map<Int, FaceEmbedding>,
        threshold: Float = DEFAULT_THRESHOLD,
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

            for ((_, templates) in byPerson) {
                var personBest = Float.NEGATIVE_INFINITY
                val person = templates.first().person
                for (template in templates) {
                    val score = cosineSimilarity(query.values, template.embedding.values)
                    if (score > personBest) personBest = score
                }
                if (personBest > bestScore) {
                    bestScore = personBest
                    bestPerson = person
                }
            }

            if (bestScore >= threshold) {
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
         * Cosine threshold tuned for multi-template MobileFaceNet galleries.
         * Prefer recall across distance changes; raise if false accepts appear.
         */
        const val DEFAULT_THRESHOLD: Float = 0.55f
    }
}
