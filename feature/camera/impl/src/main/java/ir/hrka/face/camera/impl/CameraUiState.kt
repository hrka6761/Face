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
 */
data class TrackedFaceUi(
    val trackingId: Int,
    val boundingBox: Rect,
    val person: Person?,
    val embedding: FaceEmbedding?,
    val similarity: Float,
)

/**
 * Immutable UI state for the camera feature.
 *
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
 */
data class CameraUiState(
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
) {
    /** Number of faces currently on screen. */
    val faceCount: Int get() = faces.size
}
