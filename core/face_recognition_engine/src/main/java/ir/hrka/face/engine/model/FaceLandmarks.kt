package ir.hrka.face.engine.model

import android.graphics.PointF

/**
 * InsightFace 5-point facial landmarks in source-image coordinates.
 *
 * Order matches InsightFace / SCRFD_KPS:
 * left eye, right eye, nose, left mouth corner, right mouth corner.
 */
data class FaceLandmarks(
    val leftEye: PointF,
    val rightEye: PointF,
    val nose: PointF,
    val leftMouth: PointF,
    val rightMouth: PointF,
) {
    /** Returns landmarks as a 5×2 list in InsightFace order. */
    fun asList(): List<PointF> = listOf(leftEye, rightEye, nose, leftMouth, rightMouth)

    /** Flattened `[x0,y0,...,x4,y4]` array used by alignment math. */
    fun toFloatArray(): FloatArray = floatArrayOf(
        leftEye.x, leftEye.y,
        rightEye.x, rightEye.y,
        nose.x, nose.y,
        leftMouth.x, leftMouth.y,
        rightMouth.x, rightMouth.y,
    )
}
