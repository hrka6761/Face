package ir.hrka.face.recognition.internal

import ir.hrka.face.model.EnrolledFace
import ir.hrka.face.model.FaceEmbedding
import ir.hrka.face.model.FaceMatchResult
import ir.hrka.face.model.Person
import ir.hrka.face.recognition.api.FaceMatcher


/**
 * [FaceMatcher] implementation using cosine similarity with max-over-gallery scoring.
 */
internal class CosineFaceMatcher : FaceMatcher {

    override fun match(
        query: FaceEmbedding,
        enrolled: List<EnrolledFace>,
        threshold: Float,
    ): FaceMatchResult {
        if (enrolled.isEmpty()) {
            return FaceMatchResult(person = null, similarity = 0f)
        }

        val byPerson = enrolled.groupBy { it.person.id }
        var bestPerson: Person? = null
        var bestScore = Float.NEGATIVE_INFINITY

        for ((_, templates) in byPerson) {
            var personBest = Float.NEGATIVE_INFINITY
            val person = templates.first().person
            for (template in templates) {
                val score = EmbeddingMath.cosineSimilarity(query.values, template.embedding.values)
                if (score > personBest) personBest = score
            }
            if (personBest > bestScore) {
                bestScore = personBest
                bestPerson = person
            }
        }

        return if (bestScore >= threshold) {
            FaceMatchResult(person = bestPerson, similarity = bestScore)
        } else {
            FaceMatchResult(person = null, similarity = bestScore.coerceAtLeast(0f))
        }
    }

    override fun matchAll(
        queries: Map<Int, FaceEmbedding>,
        enrolled: List<EnrolledFace>,
        threshold: Float,
    ): Map<Int, FaceMatchResult> =
        queries.mapValues { (_, embedding) -> match(embedding, enrolled, threshold) }
}
