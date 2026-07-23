package ir.hrka.face.camera.impl

import android.graphics.PointF
import android.graphics.Rect
import ir.hrka.face.model.FaceEmbedding
import ir.hrka.face.model.Person

/**
 * A face currently shown on the camera overlay.
 *
 * @property trackingId Detector tracking id.
 * @property boundingBox Face bounds in analysis-image coordinates.
 * @property person Matched identity when known; `null` when unknown.
 * @property embedding Latest robust embedding (used while collecting enrollment samples).
 * @property similarity Match score when [person] is non-null.
 * @property headEulerAngleY Head yaw in degrees when provided by the detector.
 * @property leftEye Left-eye landmark in analysis-image coordinates, if available.
 * @property rightEye Right-eye landmark in analysis-image coordinates, if available.
 */
data class TrackedFaceUi(
    val trackingId: Int,
    val boundingBox: Rect,
    val person: Person?,
    val embedding: FaceEmbedding?,
    val similarity: Float,
    val headEulerAngleY: Float? = null,
    val leftEye: PointF? = null,
    val rightEye: PointF? = null,
)

/**
 * Immutable UI state for the camera feature.
 */
data class CameraUiState(
    val mode: CameraMode = CameraMode.Recognition,
    val faces: List<TrackedFaceUi> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val isFrontCamera: Boolean = false,
    val isTorchOn: Boolean = false,
    val errorMessage: String? = null,
    val enrollTarget: TrackedFaceUi? = null,
    val enrollPhase: EnrollPhase = EnrollPhase.Idle,
    val isEnrolling: Boolean = false,
    val enrollProgress: Int = 0,
    val enrollTargetCount: Int = 12,
    val enrollStep: EnrollPoseStep? = null,
    val enrollStepProgress: Int = 0,
    val enrollStepTarget: Int = 4,
    val enrollHint: String = "",
    val enrollPoseAligned: Boolean = false,
    val enrollYawProgress: Float = 0f,
    val enrollEyesAligned: Boolean = false,
    val enrollGuideAligned: Boolean = false,
    val enrollTestStep: EnrollTestStep? = null,
    val enrollTestProgress: Int = 0,
    val enrollTestTarget: Int = EnrollTestStep.TARGET_SAMPLES,
    val enrollQualityGrade: EnrollQualityGrade? = null,
    val enrollQualityScore: Float = 0f,
) {
    /** Number of faces currently on screen. */
    val faceCount: Int get() = faces.size
}
