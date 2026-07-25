package ir.hrka.face.engine.detection

import android.graphics.Bitmap
import ir.hrka.face.engine.FaceEngineException
import ir.hrka.face.engine.model.DetectedFace

/**
 * Placeholder used when no SCRFD model path was supplied.
 * Embedding / recognize APIs remain available.
 */
internal class UnavailableFaceDetector : FaceDetector {

    override suspend fun detect(image: Bitmap): List<DetectedFace> {
        throw FaceEngineException.ModelNotFoundException(
            "(no detector model path — detection is disabled for this engine instance)",
        )
    }

    override fun close() = Unit
}
