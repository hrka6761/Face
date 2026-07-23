package ir.hrka.face.recognition.internal

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import ir.hrka.face.model.DetectedFace
import ir.hrka.face.model.FaceLandmarkType
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Crops and aligns a face region into a canonical MobileFaceNet / ArcFace 112×112 crop.
 *
 * Uses an eye-based similarity transform drawn into an exact [inputSize]×[inputSize] canvas.
 * This normalizes scale (distance-to-camera) and in-plane rotation so the same identity
 * yields similar embeddings across different distances.
 */
internal object FaceAligner {

    /** Paint with bilinear filtering for the warp. */
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /**
     * Produces an [inputSize]×[inputSize] aligned face crop from an upright [bitmap].
     *
     * @param bitmap Upright full frame.
     * @param face Detected face.
     * @param inputSize Target square size (typically 112).
     * @param scaleFactor Multiplier on the reference eye distance (`1.0` = standard crop;
     *   `<1` zooms in, `>1` includes more context). Used for multi-scale robustness.
     * @return Aligned crop, or `null` if the region is invalid.
     */
    fun cropAlignedFace(
        bitmap: Bitmap,
        face: DetectedFace,
        inputSize: Int,
        scaleFactor: Float = 1f,
    ): Bitmap? {
        val leftEye = face.landmarks[FaceLandmarkType.LEFT_EYE]
        val rightEye = face.landmarks[FaceLandmarkType.RIGHT_EYE]

        return if (leftEye != null && rightEye != null) {
            alignByEyes(
                bitmap = bitmap,
                leftEyeX = leftEye.x,
                leftEyeY = leftEye.y,
                rightEyeX = rightEye.x,
                rightEyeY = rightEye.y,
                boundingBox = face.boundingBox,
                inputSize = inputSize,
                scaleFactor = scaleFactor,
            )
        } else {
            cropByBoundingBox(bitmap, face.boundingBox, inputSize, scaleFactor)
        }
    }

    /**
     * Builds several aligned crops at different scale factors for distance robustness.
     */
    fun cropMultiScale(
        bitmap: Bitmap,
        face: DetectedFace,
        inputSize: Int,
        scaleFactors: FloatArray = DEFAULT_SCALE_FACTORS,
    ): List<Bitmap> {
        val crops = ArrayList<Bitmap>(scaleFactors.size)
        for (scale in scaleFactors) {
            cropAlignedFace(bitmap, face, inputSize, scale)?.let(crops::add)
        }
        return crops
    }

    private fun cropByBoundingBox(
        bitmap: Bitmap,
        box: Rect,
        inputSize: Int,
        scaleFactor: Float,
    ): Bitmap? {
        if (bitmap.isRecycled) return null
        val basePad = box.width().coerceAtLeast(box.height()) * 0.25f
        val pad = (basePad * scaleFactor).toInt().coerceAtLeast(2)
        val left = (box.left - pad).coerceAtLeast(0)
        val top = (box.top - pad).coerceAtLeast(0)
        val right = (box.right + pad).coerceAtMost(bitmap.width)
        val bottom = (box.bottom + pad).coerceAtMost(bitmap.height)
        val width = right - left
        val height = bottom - top
        if (width <= 8 || height <= 8) return null

        return try {
            val cropped = Bitmap.createBitmap(bitmap, left, top, width, height)
            val scaled = Bitmap.createScaledBitmap(cropped, inputSize, inputSize, true)
            if (scaled !== cropped && !cropped.isRecycled) {
                cropped.recycle()
            }
            scaled
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun alignByEyes(
        bitmap: Bitmap,
        leftEyeX: Float,
        leftEyeY: Float,
        rightEyeX: Float,
        rightEyeY: Float,
        boundingBox: Rect,
        inputSize: Int,
        scaleFactor: Float,
    ): Bitmap? {
        val dx = rightEyeX - leftEyeX
        val dy = rightEyeY - leftEyeY
        val eyeDist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (eyeDist < 2f) {
            return cropByBoundingBox(bitmap, boundingBox, inputSize, scaleFactor)
        }

        // Canonical ArcFace / MobileFaceNet crop: eyes near 0.35*size horizontally apart,
        // vertically around 0.4*size from the top.
        val desiredEyeDist = inputSize * 0.35f / scaleFactor.coerceIn(0.6f, 1.6f)
        val scale = desiredEyeDist / eyeDist
        val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()

        val eyesCenterX = (leftEyeX + rightEyeX) / 2f
        val eyesCenterY = (leftEyeY + rightEyeY) / 2f

        // Destination where the midpoint between the eyes should land.
        val destEyesX = inputSize / 2f
        val destEyesY = inputSize * 0.38f

        val matrix = Matrix()
        matrix.postTranslate(-eyesCenterX, -eyesCenterY)
        matrix.postRotate(-Math.toDegrees(angle.toDouble()).toFloat())
        matrix.postScale(scale, scale)
        matrix.postTranslate(destEyesX, destEyesY)

        return try {
            if (bitmap.isRecycled) return cropByBoundingBox(bitmap, boundingBox, inputSize, scaleFactor)
            val output = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawColor(android.graphics.Color.BLACK)
            canvas.drawBitmap(bitmap, matrix, bitmapPaint)
            output
        } catch (_: RuntimeException) {
            cropByBoundingBox(bitmap, boundingBox, inputSize, scaleFactor)
        }
    }

    /** Default multi-scale factors used for robust embedding. */
    val DEFAULT_SCALE_FACTORS: FloatArray = floatArrayOf(0.85f, 1.0f, 1.2f)
}
