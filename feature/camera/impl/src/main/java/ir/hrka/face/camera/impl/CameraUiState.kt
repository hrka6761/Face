package ir.hrka.face.camera.impl

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
 */
data class TrackedFaceUi(
    val trackingId: Int,
    val boundingBox: Rect,
    val person: Person?,
    val embedding: FaceEmbedding?,
    val similarity: Float,
    val headEulerAngleY: Float? = null,
)

/**
 * Immutable UI state for the camera feature.
 *
 * @property mode Active camera operating mode.
 * @property faces Faces detected in the latest processed frame.
 * @property imageWidth Analysis image width.
 * @property imageHeight Analysis image height.
 * @property isFrontCamera Whether the front lens is active.
 * @property isTorchOn Whether torch is enabled.
 * @property errorMessage Optional user-visible error.
 * @property enrollTarget Face selected for the enroll dialog, if any.
 * @property isEnrolling Whether an enroll operation is in progress.
 * @property enrollProgress Collected enrollment templates so far.
 * @property enrollTargetCount Desired number of enrollment templates.
 * @property enrollStep Active guided pose step while enrolling.
 * @property enrollStepProgress Samples collected for the current pose step.
 * @property enrollStepTarget Samples required for the current pose step.
 * @property enrollHint Live guidance text for the current pose / yaw.
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
    val isEnrolling: Boolean = false,
    val enrollProgress: Int = 0,
    val enrollTargetCount: Int = 12,
    val enrollStep: EnrollPoseStep? = null,
    val enrollStepProgress: Int = 0,
    val enrollStepTarget: Int = 4,
    val enrollHint: String = "",
) {
    /** Number of faces currently on screen. */
    val faceCount: Int get() = faces.size
}
