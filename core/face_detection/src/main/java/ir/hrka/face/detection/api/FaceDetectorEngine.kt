package ir.hrka.face.detection.api

import android.graphics.Bitmap
import android.media.Image
import androidx.camera.core.ImageProxy

/**
 * Face detector engine backed by ML Kit.
 *
 * Implementations are thread-safe for sequential use. Call [close] when the host no longer
 * needs the engine.
 *
 * Detection methods suspend and must not be called on the main thread for large workloads;
 * callers should still always close [ImageProxy] themselves (typically in `finally`).
 */
interface FaceDetectorEngine : AutoCloseable {

    /**
     * Detects faces in a CameraX [ImageProxy].
     *
     * Does **not** close [imageProxy]; the caller owns that responsibility.
     *
     * @param imageProxy Camera frame.
     * @return [FaceDetectionResult.Success] or [FaceDetectionResult.Failure].
     */
    suspend fun detect(imageProxy: ImageProxy): FaceDetectionResult

    /**
     * Detects faces in a media [Image].
     *
     * @param image YUV/JPEG media image.
     * @param rotationDegrees Rotation that should be applied for upright faces.
     * @return [FaceDetectionResult.Success] or [FaceDetectionResult.Failure].
     */
    suspend fun detect(image: Image, rotationDegrees: Int): FaceDetectionResult

    /**
     * Detects faces in a [Bitmap].
     *
     * @param bitmap Source bitmap (ARGB_8888 preferred).
     * @param rotationDegrees Optional additional rotation hint.
     * @return [FaceDetectionResult.Success] or [FaceDetectionResult.Failure].
     */
    suspend fun detect(bitmap: Bitmap, rotationDegrees: Int = 0): FaceDetectionResult

    /**
     * Releases native detector resources.
     */
    override fun close()
}
