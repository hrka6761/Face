package ir.hrka.face.recognition.api

/**
 * Error raised by the face-recognition pipeline.
 *
 * @param message Human-readable description.
 * @param cause Optional underlying exception.
 */
class FaceRecognitionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
