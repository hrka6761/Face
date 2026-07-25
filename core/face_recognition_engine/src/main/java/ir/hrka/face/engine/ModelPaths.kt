package ir.hrka.face.engine

/**
 * Absolute filesystem paths to InsightFace ONNX models on device storage.
 *
 * Models are **not** packaged in the APK. The host app downloads them
 * and passes their local paths here.
 *
 * Expected models (buffalo_l-compatible):
 * - [embeddingModelPath]: ArcFace w600k_r50 (required)
 * - [detectorModelPath]: SCRFD-10G_KPS (optional; only needed for [FaceRecognitionEngine.detectFaces])
 *
 * @property embeddingModelPath Absolute path to the face-embedding ONNX file.
 * @property detectorModelPath Absolute path to the face-detection ONNX file, or `null`
 * when detection is handled outside this engine (e.g. ML Kit).
 */
data class ModelPaths(
    val embeddingModelPath: String,
    val detectorModelPath: String? = null,
)
