package ir.hrka.face.engine

/**
 * Absolute filesystem paths to InsightFace ONNX models on device storage.
 *
 * Models are **not** packaged in the APK. The host app (or user) downloads them
 * and passes their local paths here.
 *
 * Expected models (buffalo_l-compatible):
 * - [detectorModelPath]: SCRFD-10G_KPS (`det_10g.onnx` / `scrfd_10g_kps.onnx`)
 * - [embeddingModelPath]: ArcFace w600k_r50 (`w600k_r50.onnx` / `arcface_w600k_r50.onnx`)
 *
 * @property detectorModelPath Absolute path to the face-detection ONNX file.
 * @property embeddingModelPath Absolute path to the face-embedding ONNX file.
 */
data class ModelPaths(
    val detectorModelPath: String,
    val embeddingModelPath: String,
)
