package ir.hrka.face.engine.model

/**
 * Axis-aligned face bounding box in source-image pixel coordinates.
 *
 * @property left Left edge (inclusive).
 * @property top Top edge (inclusive).
 * @property right Right edge (exclusive-ish; max-x of the box).
 * @property bottom Bottom edge (max-y of the box).
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    /** Box width in pixels. */
    val width: Float get() = (right - left).coerceAtLeast(0f)

    /** Box height in pixels. */
    val height: Float get() = (bottom - top).coerceAtLeast(0f)

    /** Box area in square pixels. */
    val area: Float get() = width * height
}
