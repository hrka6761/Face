package ir.hrka.face.camera.impl.ui

import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import ir.hrka.face.camera.impl.TrackedFaceUi
import kotlin.math.hypot

/**
 * Variable-size face/eye oval guides for enrollment and Test Scan.
 */
object EyeGuideLayout {

    /** Default (medium) face oval size as a fraction of the shorter view side. */
    const val OVAL_FRACTION: Float = 0.58f

    const val OVAL_TOP_BIAS: Float = 0.04f
    const val EYE_ROW_FROM_TOP: Float = 0.38f
    const val EYE_SIDE_INSET: Float = 0.28f
    const val EYE_RADIUS_FRACTION: Float = 0.085f
    const val HIT_RADIUS_FACTOR: Float = 1.4f

    /** Face must cover at least this fraction of the guide oval area. */
    const val MIN_FACE_OVAL_COVERAGE: Float = 0.35f

    /** Face center must stay inside this inset of the oval (0..0.5). */
    const val FACE_CENTER_INSET: Float = 0.12f

    fun ovalRect(
        viewWidth: Float,
        viewHeight: Float,
        ovalFraction: Float = OVAL_FRACTION,
    ): RectF {
        val side = minOf(viewWidth, viewHeight) * ovalFraction.coerceIn(0.25f, 0.9f)
        val ovalW = side
        val ovalH = side * 1.25f
        val left = (viewWidth - ovalW) / 2f
        val top = (viewHeight - ovalH) / 2f - viewHeight * OVAL_TOP_BIAS
        return RectF(left, top, left + ovalW, top + ovalH)
    }

    fun leftEyeTarget(
        viewWidth: Float,
        viewHeight: Float,
        ovalFraction: Float = OVAL_FRACTION,
    ): Offset {
        val oval = ovalRect(viewWidth, viewHeight, ovalFraction)
        return Offset(
            x = oval.left + oval.width() * EYE_SIDE_INSET,
            y = oval.top + oval.height() * EYE_ROW_FROM_TOP,
        )
    }

    fun rightEyeTarget(
        viewWidth: Float,
        viewHeight: Float,
        ovalFraction: Float = OVAL_FRACTION,
    ): Offset {
        val oval = ovalRect(viewWidth, viewHeight, ovalFraction)
        return Offset(
            x = oval.right - oval.width() * EYE_SIDE_INSET,
            y = oval.top + oval.height() * EYE_ROW_FROM_TOP,
        )
    }

    fun eyeRadius(
        viewWidth: Float,
        viewHeight: Float,
        ovalFraction: Float = OVAL_FRACTION,
    ): Float = ovalRect(viewWidth, viewHeight, ovalFraction).width() * EYE_RADIUS_FRACTION

    fun isEyeOnTarget(eye: PointF?, target: Offset, radius: Float): Boolean {
        if (eye == null) return false
        val limit = radius * HIT_RADIUS_FACTOR
        return hypot((eye.x - target.x).toDouble(), (eye.y - target.y).toDouble()) <= limit
    }

    /**
     * Maps analysis-space eyes into view space (handles front-camera mirror).
     */
    fun mapEyes(
        face: TrackedFaceUi,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        mirrorX: Boolean,
    ): Pair<PointF?, PointF?> {
        val left = face.leftEye?.let {
            FaceCoordinateMapper.mapPoint(
                it.x, it.y, imageWidth, imageHeight, viewWidth, viewHeight, mirrorX,
            )
        }
        val right = face.rightEye?.let {
            FaceCoordinateMapper.mapPoint(
                it.x, it.y, imageWidth, imageHeight, viewWidth, viewHeight, mirrorX,
            )
        }
        return left to right
    }

    /**
     * Both eyes sit on the guide circles (screen-left / screen-right targets).
     */
    fun eyesOnTargets(
        face: TrackedFaceUi,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        mirrorX: Boolean,
        ovalFraction: Float = OVAL_FRACTION,
    ): Boolean {
        val (viewLeftEye, viewRightEye) = mapEyes(
            face, imageWidth, imageHeight, viewWidth, viewHeight, mirrorX,
        )
        val leftTarget = leftEyeTarget(viewWidth, viewHeight, ovalFraction)
        val rightTarget = rightEyeTarget(viewWidth, viewHeight, ovalFraction)
        val radius = eyeRadius(viewWidth, viewHeight, ovalFraction)

        // After mirroring, person's left eye appears near the screen-right target.
        return if (mirrorX) {
            isEyeOnTarget(viewLeftEye, rightTarget, radius) &&
                isEyeOnTarget(viewRightEye, leftTarget, radius)
        } else {
            isEyeOnTarget(viewLeftEye, leftTarget, radius) &&
                isEyeOnTarget(viewRightEye, rightTarget, radius)
        }
    }

    /**
     * Face bounds (view space) are centered in the oval and large enough to fill it.
     */
    fun faceFitsOval(
        faceBox: Rect,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        mirrorX: Boolean,
        ovalFraction: Float = OVAL_FRACTION,
    ): Boolean {
        val mapped = FaceCoordinateMapper.mapRect(
            faceBox, imageWidth, imageHeight, viewWidth, viewHeight, mirrorX,
        )
        if (mapped.isEmpty) return false
        val oval = ovalRect(viewWidth, viewHeight, ovalFraction)
        val insetX = oval.width() * FACE_CENTER_INSET
        val insetY = oval.height() * FACE_CENTER_INSET
        val inner = RectF(
            oval.left + insetX,
            oval.top + insetY,
            oval.right - insetX,
            oval.bottom - insetY,
        )
        if (!inner.contains(mapped.centerX(), mapped.centerY())) return false

        val faceArea = mapped.width() * mapped.height()
        val ovalArea = oval.width() * oval.height()
        return faceArea >= ovalArea * MIN_FACE_OVAL_COVERAGE
    }

    /**
     * Full-face guide: face fills the oval and both eyes sit on the eye targets.
     */
    fun isFrontGuideAligned(
        face: TrackedFaceUi,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        mirrorX: Boolean,
        ovalFraction: Float,
    ): Boolean {
        if (viewWidth <= 0f || viewHeight <= 0f || imageWidth <= 0) return false
        return faceFitsOval(
            face.boundingBox, imageWidth, imageHeight, viewWidth, viewHeight, mirrorX, ovalFraction,
        ) && eyesOnTargets(
            face, imageWidth, imageHeight, viewWidth, viewHeight, mirrorX, ovalFraction,
        )
    }

    /**
     * Profile guide: face fills the oval (eyes optional — often one is occluded).
     */
    fun isProfileGuideAligned(
        face: TrackedFaceUi,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        mirrorX: Boolean,
        ovalFraction: Float,
    ): Boolean {
        if (viewWidth <= 0f || viewHeight <= 0f || imageWidth <= 0) return false
        return faceFitsOval(
            face.boundingBox, imageWidth, imageHeight, viewWidth, viewHeight, mirrorX, ovalFraction,
        )
    }
}
