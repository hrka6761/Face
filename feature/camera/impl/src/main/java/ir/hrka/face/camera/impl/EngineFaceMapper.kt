package ir.hrka.face.camera.impl

import android.graphics.PointF
import ir.hrka.face.engine.model.BoundingBox
import ir.hrka.face.engine.model.DetectedFace as EngineDetectedFace
import ir.hrka.face.engine.model.FaceEmbedding as EngineFaceEmbedding
import ir.hrka.face.engine.model.FaceLandmarks
import ir.hrka.face.model.DetectedFace
import ir.hrka.face.model.FaceEmbedding
import ir.hrka.face.model.FaceLandmarkType

/**
 * Maps between app-level face models and [ir.hrka.face.engine] types.
 */
object EngineFaceMapper {

    /** Converts an ML Kit domain face to an engine face when 5 landmarks exist. */
    fun toEngineFace(face: DetectedFace): EngineDetectedFace? {
        val leftEye = face.landmarks[FaceLandmarkType.LEFT_EYE] ?: return null
        val rightEye = face.landmarks[FaceLandmarkType.RIGHT_EYE] ?: return null
        val nose = face.landmarks[FaceLandmarkType.NOSE_BASE] ?: return null
        val leftMouth = face.landmarks[FaceLandmarkType.MOUTH_LEFT] ?: return null
        val rightMouth = face.landmarks[FaceLandmarkType.MOUTH_RIGHT] ?: return null
        val box = face.boundingBox

        return EngineDetectedFace(
            boundingBox = BoundingBox(
                left = box.left.toFloat(),
                top = box.top.toFloat(),
                right = box.right.toFloat(),
                bottom = box.bottom.toFloat(),
            ),
            confidenceScore = 1f,
            landmarks = FaceLandmarks(
                leftEye = PointF(leftEye.x, leftEye.y),
                rightEye = PointF(rightEye.x, rightEye.y),
                nose = PointF(nose.x, nose.y),
                leftMouth = PointF(leftMouth.x, leftMouth.y),
                rightMouth = PointF(rightMouth.x, rightMouth.y),
            ),
        )
    }

    /** Maps an engine embedding into the shared domain [FaceEmbedding]. */
    fun toDomainEmbedding(embedding: EngineFaceEmbedding): FaceEmbedding =
        FaceEmbedding(values = embedding.vector.copyOf())
}
