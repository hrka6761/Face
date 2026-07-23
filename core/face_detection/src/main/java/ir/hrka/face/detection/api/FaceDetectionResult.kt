package ir.hrka.face.detection.api

import ir.hrka.face.model.DetectedFace

/**
 * Outcome of a face-detection request.
 */
sealed class FaceDetectionResult {

    /**
     * Detection succeeded.
     *
     * @property faces Detected faces in upright-image coordinates.
     * @property imageWidth Upright image width in pixels (after camera rotation).
     * @property imageHeight Upright image height in pixels (after camera rotation).
     * @property rotationDegrees Frame rotation degrees applied by the camera.
     */
    data class Success(
        val faces: List<DetectedFace>,
        val imageWidth: Int,
        val imageHeight: Int,
        val rotationDegrees: Int,
    ) : FaceDetectionResult()

    /**
     * Detection failed.
     *
     * @property message Human-readable error message.
     * @property cause Optional underlying exception.
     */
    data class Failure(
        val message: String,
        val cause: Throwable? = null,
    ) : FaceDetectionResult()
}
