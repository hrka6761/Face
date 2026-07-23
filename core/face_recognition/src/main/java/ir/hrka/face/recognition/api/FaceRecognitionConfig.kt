package ir.hrka.face.recognition.api

/**
 * Configuration for the MobileFaceNet recognition pipeline.
 *
 * @property modelAssetPath Asset path of the TFLite model inside this module.
 * @property inputSize Square input size expected by MobileFaceNet.
 * @property embeddingDim Output embedding dimensionality.
 * @property matchThreshold Default cosine-similarity threshold for a positive match.
 * @property matchMargin Minimum gap between best and second-best person scores.
 * @property numThreads TFLite interpreter thread count.
 */
data class FaceRecognitionConfig(
    val modelAssetPath: String = DEFAULT_MODEL_ASSET,
    val inputSize: Int = DEFAULT_INPUT_SIZE,
    val embeddingDim: Int = DEFAULT_EMBEDDING_DIM,
    val matchThreshold: Float = DEFAULT_MATCH_THRESHOLD,
    val matchMargin: Float = DEFAULT_MATCH_MARGIN,
    val numThreads: Int = DEFAULT_NUM_THREADS,
) {
    companion object {
        /** Bundled MobileFaceNet asset file name. */
        const val DEFAULT_MODEL_ASSET: String = "mobile_face_net.tflite"

        /** MobileFaceNet input spatial size. */
        const val DEFAULT_INPUT_SIZE: Int = 112

        /** MobileFaceNet embedding size. */
        const val DEFAULT_EMBEDDING_DIM: Int = 192

        /**
         * Cosine similarity threshold after L2 normalization.
         *
         * Raised for multi-pose (front + profile) galleries so different identities
         * are not accepted. Pair with [DEFAULT_MATCH_MARGIN].
         */
        const val DEFAULT_MATCH_THRESHOLD: Float = 0.68f

        /**
         * Required gap between the best and second-best person scores.
         * Rejects ambiguous matches that would otherwise cause false identification.
         */
        const val DEFAULT_MATCH_MARGIN: Float = 0.08f

        /** Default interpreter threads. */
        const val DEFAULT_NUM_THREADS: Int = 4

        /** Accepted samples required per pose step during guided enrollment. */
        const val ENROLL_SAMPLES_PER_POSE: Int = 4

        /** Number of guided pose steps (front, left profile, right profile). */
        const val ENROLL_POSE_STEPS: Int = 3

        /** Total templates targeted across all pose steps. */
        const val ENROLL_TARGET_TEMPLATES: Int =
            ENROLL_SAMPLES_PER_POSE * ENROLL_POSE_STEPS

        /**
         * Soft timeout for the whole guided enrollment session.
         * Enrollment does not auto-finish on timeout; the UI keeps guiding until
         * all pose steps complete or the user cancels.
         */
        const val ENROLL_TIMEOUT_MS: Long = 90_000L

        /** Front face: absolute yaw must stay within this many degrees. */
        const val POSE_FRONT_YAW_MAX: Float = 15f

        /** Profile face: minimum absolute yaw (degrees). */
        const val POSE_PROFILE_YAW_MIN: Float = 28f

        /** Profile face: maximum absolute yaw (degrees). */
        const val POSE_PROFILE_YAW_MAX: Float = 65f
    }
}
