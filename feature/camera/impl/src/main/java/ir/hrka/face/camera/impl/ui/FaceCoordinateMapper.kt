package ir.hrka.face.camera.impl.ui

import android.graphics.Rect
import android.graphics.RectF

/**
 * Maps face bounding boxes from analysis-image coordinates into preview view coordinates.
 *
 * Uses a center-crop (FILL_CENTER) mapping consistent with [androidx.camera.view.PreviewView]
 * scale type [androidx.camera.view.PreviewView.ScaleType.FILL_CENTER].
 */
object FaceCoordinateMapper {

    /**
     * Maps an image-space [box] into view-space.
     *
     * @param box Face bounds in upright analysis image coordinates.
     * @param imageWidth Analysis image width.
     * @param imageHeight Analysis image height.
     * @param viewWidth Preview view width in pixels.
     * @param viewHeight Preview view height in pixels.
     * @param mirrorX Whether to mirror horizontally (front camera).
     * @return Mapped rectangle in view coordinates.
     */
    fun mapRect(
        box: Rect,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        mirrorX: Boolean,
    ): RectF {
        if (imageWidth <= 0 || imageHeight <= 0 || viewWidth <= 0f || viewHeight <= 0f) {
            return RectF()
        }

        val scale = maxOf(viewWidth / imageWidth, viewHeight / imageHeight)
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        val offsetX = (viewWidth - scaledWidth) / 2f
        val offsetY = (viewHeight - scaledHeight) / 2f

        var left = box.left * scale + offsetX
        var top = box.top * scale + offsetY
        var right = box.right * scale + offsetX
        var bottom = box.bottom * scale + offsetY

        if (mirrorX) {
            val mirroredLeft = viewWidth - right
            val mirroredRight = viewWidth - left
            left = mirroredLeft
            right = mirroredRight
        }

        return RectF(left, top, right, bottom)
    }
}
