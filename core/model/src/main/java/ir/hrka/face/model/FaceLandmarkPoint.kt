package ir.hrka.face.model

/**
 * A 2D landmark point in image coordinates.
 *
 * @property x Horizontal position in pixels.
 * @property y Vertical position in pixels.
 */
data class FaceLandmarkPoint(
    val x: Float,
    val y: Float,
)

/**
 * Named facial landmarks used for alignment and overlays.
 */
enum class FaceLandmarkType {
    /** Left eye center. */
    LEFT_EYE,

    /** Right eye center. */
    RIGHT_EYE,

    /** Nose base. */
    NOSE_BASE,

    /** Mouth left corner. */
    MOUTH_LEFT,

    /** Mouth right corner. */
    MOUTH_RIGHT,
}
