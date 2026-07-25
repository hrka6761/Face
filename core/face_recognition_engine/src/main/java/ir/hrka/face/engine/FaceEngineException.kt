package ir.hrka.face.engine

/**
 * Typed failures thrown by [FaceRecognitionEngine] and its pipeline stages.
 */
sealed class FaceEngineException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** An ONNX model file is missing or not readable at the given filesystem path. */
    class ModelNotFoundException(
        val modelPath: String,
        cause: Throwable? = null,
    ) : FaceEngineException(
        message = "ONNX model not found or not readable: $modelPath. " +
            "Download buffalo_l-compatible models to device storage and pass their " +
            "absolute paths via ModelPaths.",
        cause = cause,
    )

    /** Model load / session creation failed. */
    class ModelLoadException(
        message: String,
        cause: Throwable? = null,
    ) : FaceEngineException(message, cause)

    /** Inference failed (invalid tensor shapes, ORT errors, etc.). */
    class InferenceException(
        message: String,
        cause: Throwable? = null,
    ) : FaceEngineException(message, cause)

    /** Input image / face data is invalid for the requested operation. */
    class InvalidInputException(
        message: String,
        cause: Throwable? = null,
    ) : FaceEngineException(message, cause)

    /** Engine was used after [FaceRecognitionEngine.close]. */
    class EngineClosedException :
        FaceEngineException("FaceRecognitionEngine has already been closed.")

    /** No face was found when the pipeline required one. */
    class NoFaceDetectedException :
        FaceEngineException("No face detected in the input image.")
}
