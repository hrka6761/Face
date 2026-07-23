package ir.hrka.face.recognition.api

/**
 * Configuration for the MobileFaceNet recognition pipeline.
 *
 * @property modelAssetPath Asset path of the TFLite model inside this module.
 * @property inputSize Square input size expected by MobileFaceNet.
 * @property embeddingDim Output embedding dimensionality.
 * @property matchThreshold Default cosine-similarity threshold for a positive match.
 * @property numThreads TFLite interpreter thread count.
 */
data class FaceRecognitionConfig(
    val modelAssetPath: String = DEFAULT_MODEL_ASSET,
    val inputSize: Int = DEFAULT_INPUT_SIZE,
    val embeddingDim: Int = DEFAULT_EMBEDDING_DIM,
    val matchThreshold: Float = DEFAULT_MATCH_THRESHOLD,
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
         * `0.85` was far too strict for distance/pose variation with a single template.
         * With multi-scale + multi-template galleries, `0.55` gives high recall while
         * still separating different identities in practice.
         */
        const val DEFAULT_MATCH_THRESHOLD: Float = 0.55f

        /** Default interpreter threads. */
        const val DEFAULT_NUM_THREADS: Int = 4

        /** How many multi-scale / multi-frame templates to gather during enrollment. */
        const val ENROLL_TARGET_TEMPLATES: Int = 12

        /** Max milliseconds to spend collecting enrollment templates. */
        const val ENROLL_TIMEOUT_MS: Long = 2500L
    }
}
