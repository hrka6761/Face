package ir.hrka.face.engine.model

/**
 * A single face detected by SCRFD-10G_KPS.
 *
 * @property boundingBox Face box in source-image coordinates.
 * @property confidenceScore Detector confidence in `[0, 1]`.
 * @property landmarks Five facial landmarks for InsightFace alignment.
 */
data class DetectedFace(
    val boundingBox: BoundingBox,
    val confidenceScore: Float,
    val landmarks: FaceLandmarks,
)
