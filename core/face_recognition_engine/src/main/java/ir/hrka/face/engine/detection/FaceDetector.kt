package ir.hrka.face.engine.detection

import android.graphics.Bitmap
import ir.hrka.face.engine.model.DetectedFace

/**
 * Face detector contract used by [ir.hrka.face.engine.FaceRecognitionEngine].
 */
internal interface FaceDetector : AutoCloseable {

    /**
     * Detects faces and 5-point landmarks in [image].
     *
     * @return Faces sorted by descending confidence.
     */
    suspend fun detect(image: Bitmap): List<DetectedFace>
}
