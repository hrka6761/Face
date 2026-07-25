package ir.hrka.face.engine

/**
 * Optional tuning for [FaceRecognitionEngine].
 *
 * Model file locations are supplied separately via [ModelPaths].
 *
 * @property matchThreshold Minimum cosine similarity for a positive match (~0.40–0.45 starting point).
 * @property matchMargin Required gap between best and second-best person scores.
 * @property detectorScoreThreshold Minimum SCRFD face score before NMS.
 * @property nmsThreshold IoU threshold for non-maximum suppression.
 * @property detectorInputSize Square letterbox size for SCRFD (default 640).
 * @property numThreads ONNX Runtime intra-op threads.
 * @property enableNnapi Attempt NNAPI (default off; CPU is the safe default).
 */
data class EngineConfig(
    val matchThreshold: Float = DEFAULT_MATCH_THRESHOLD,
    val matchMargin: Float = DEFAULT_MATCH_MARGIN,
    val detectorScoreThreshold: Float = DEFAULT_DETECTOR_SCORE_THRESHOLD,
    val nmsThreshold: Float = DEFAULT_NMS_THRESHOLD,
    val detectorInputSize: Int = DEFAULT_DETECTOR_INPUT_SIZE,
    val numThreads: Int = DEFAULT_NUM_THREADS,
    val enableNnapi: Boolean = false,
) {
    companion object {
        const val DEFAULT_DETECTOR_INPUT_SIZE: Int = 640
        const val DEFAULT_DETECTOR_SCORE_THRESHOLD: Float = 0.5f
        const val DEFAULT_NMS_THRESHOLD: Float = 0.4f
        const val DEFAULT_MATCH_THRESHOLD: Float = 0.42f
        const val DEFAULT_MATCH_MARGIN: Float = 0.08f
        const val DEFAULT_NUM_THREADS: Int = 4
        const val ALIGNED_FACE_SIZE: Int = 112
        const val EMBEDDING_DIM: Int = 512

        /** Suggested on-disk filename for the detector model. */
        const val SUGGESTED_DETECTOR_FILENAME: String = "scrfd_10g_kps.onnx"

        /** Suggested on-disk filename for the embedding model. */
        const val SUGGESTED_EMBEDDING_FILENAME: String = "arcface_w600k_r50.onnx"
    }
}
