package ir.hrka.face.recognition.api

import ir.hrka.face.model.EnrolledFace
import ir.hrka.face.model.FaceEmbedding
import ir.hrka.face.model.FaceMatchResult

/**
 * Matches query embeddings against enrolled identities using cosine similarity.
 */
interface FaceMatcher {

    /**
     * Finds the best enrolled match for [query] when above [threshold] and [margin].
     *
     * @param query Query embedding (preferably L2-normalized).
     * @param enrolled Candidate identities.
     * @param threshold Minimum cosine similarity for a positive match.
     * @param margin Minimum gap between best and second-best person scores.
     */
    fun match(
        query: FaceEmbedding,
        enrolled: List<EnrolledFace>,
        threshold: Float = FaceRecognitionConfig.DEFAULT_MATCH_THRESHOLD,
        margin: Float = FaceRecognitionConfig.DEFAULT_MATCH_MARGIN,
    ): FaceMatchResult

    /**
     * Matches each query embedding independently.
     *
     * @param queries Map of tracking id → query embedding.
     * @param enrolled Candidate identities.
     * @param threshold Minimum cosine similarity for a positive match.
     * @param margin Minimum gap between best and second-best person scores.
     */
    fun matchAll(
        queries: Map<Int, FaceEmbedding>,
        enrolled: List<EnrolledFace>,
        threshold: Float = FaceRecognitionConfig.DEFAULT_MATCH_THRESHOLD,
        margin: Float = FaceRecognitionConfig.DEFAULT_MATCH_MARGIN,
    ): Map<Int, FaceMatchResult>
}
