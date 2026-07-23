package ir.hrka.face.model

import android.graphics.Rect

/**
 * A face detected in a camera frame or bitmap.
 *
 * @property trackingId Stable tracking id from the detector when available; otherwise a
 * synthetic id for the current frame.
 * @property boundingBox Axis-aligned face bounds in the source image coordinate space.
 * @property landmarks Optional landmark map for alignment.
 * @property headEulerAngleX Head tilt (degrees) when provided by the detector.
 * @property headEulerAngleY Head pan (degrees) when provided by the detector.
 * @property headEulerAngleZ Head roll (degrees) when provided by the detector.
 */
data class DetectedFace(
    val trackingId: Int,
    val boundingBox: Rect,
    val landmarks: Map<FaceLandmarkType, FaceLandmarkPoint> = emptyMap(),
    val headEulerAngleX: Float? = null,
    val headEulerAngleY: Float? = null,
    val headEulerAngleZ: Float? = null,
)
