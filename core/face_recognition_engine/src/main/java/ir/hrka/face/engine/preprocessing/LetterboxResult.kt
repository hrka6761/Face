package ir.hrka.face.engine.preprocessing

import android.graphics.Bitmap

/**
 * Result of letterboxing an image into a square network input.
 *
 * @property bitmap Square letterboxed bitmap (caller may recycle if it owns it).
 * @property scale Uniform scale applied to the source before padding.
 * @property padX Left padding in network pixels.
 * @property padY Top padding in network pixels.
 * @property sourceWidth Original image width.
 * @property sourceHeight Original image height.
 * @property inputSize Network square size.
 */
internal data class LetterboxResult(
    val bitmap: Bitmap,
    val scale: Float,
    val padX: Float,
    val padY: Float,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val inputSize: Int,
) {
    /**
     * Maps a point from network/letterbox coordinates back to the source image.
     */
    fun toSourceX(x: Float): Float = (x - padX) / scale

    /**
     * Maps a point from network/letterbox coordinates back to the source image.
     */
    fun toSourceY(y: Float): Float = (y - padY) / scale
}
