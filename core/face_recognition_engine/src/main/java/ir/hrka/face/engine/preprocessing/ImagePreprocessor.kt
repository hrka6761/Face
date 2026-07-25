package ir.hrka.face.engine.preprocessing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import ir.hrka.face.engine.FaceEngineException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.min

/**
 * Deterministic image preprocessing for InsightFace ONNX models.
 *
 * Channel order:
 * Official InsightFace Python uses `cv2.dnn.blobFromImage(..., swapRB=True)` on BGR
 * OpenCV images, which yields **RGB** planar NCHW float tensors. This preprocessor
 * matches that contract when reading Android [Bitmap] (ARGB) pixels.
 *
 * Normalization:
 * - Detection (SCRFD): `(pixel - 127.5) / 128.0`
 * - Recognition (ArcFace): `(pixel - 127.5) / 127.5`
 */
internal object ImagePreprocessor {

    const val DETECT_MEAN: Float = 127.5f
    const val DETECT_STD: Float = 128.0f
    const val RECOG_MEAN: Float = 127.5f
    const val RECOG_STD: Float = 127.5f

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    /**
     * Letterboxes [source] into an [inputSize]×[inputSize] bitmap with black padding,
     * preserving aspect ratio (InsightFace-style).
     */
    fun letterbox(source: Bitmap, inputSize: Int): LetterboxResult {
        if (source.isRecycled) {
            throw FaceEngineException.InvalidInputException("Source bitmap is recycled.")
        }
        if (inputSize <= 0) {
            throw FaceEngineException.InvalidInputException("inputSize must be > 0.")
        }

        val srcW = source.width
        val srcH = source.height
        val scale = min(inputSize.toFloat() / srcW, inputSize.toFloat() / srcH)
        val resizedW = (srcW * scale).toInt().coerceAtLeast(1)
        val resizedH = (srcH * scale).toInt().coerceAtLeast(1)
        val padX = (inputSize - resizedW) / 2f
        val padY = (inputSize - resizedH) / 2f

        val output = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.BLACK)
        val resized = Bitmap.createScaledBitmap(source, resizedW, resizedH, true)
        try {
            canvas.drawBitmap(resized, padX, padY, bitmapPaint)
        } finally {
            if (resized !== source && !resized.isRecycled) {
                resized.recycle()
            }
        }

        return LetterboxResult(
            bitmap = output,
            scale = scale,
            padX = padX,
            padY = padY,
            sourceWidth = srcW,
            sourceHeight = srcH,
            inputSize = inputSize,
        )
    }

    /**
     * Converts a bitmap to an RGB NCHW float buffer with mean/std normalization.
     *
     * Layout: `[R plane | G plane | B plane]`, each plane row-major.
     */
    fun bitmapToRgbNchw(
        bitmap: Bitmap,
        mean: Float,
        std: Float,
    ): FloatBuffer {
        if (bitmap.isRecycled) {
            throw FaceEngineException.InvalidInputException("Bitmap is recycled.")
        }
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val plane = width * height
        val buffer = ByteBuffer
            .allocateDirect(3 * plane * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        val invStd = 1f / std

        // R plane
        for (i in pixels.indices) {
            val r = ((pixels[i] shr 16) and 0xFF).toFloat()
            buffer.put((r - mean) * invStd)
        }
        // G plane
        for (i in pixels.indices) {
            val g = ((pixels[i] shr 8) and 0xFF).toFloat()
            buffer.put((g - mean) * invStd)
        }
        // B plane
        for (i in pixels.indices) {
            val b = (pixels[i] and 0xFF).toFloat()
            buffer.put((b - mean) * invStd)
        }

        buffer.rewind()
        return buffer
    }

    /** SCRFD detection tensor from a letterboxed bitmap. */
    fun toDetectionTensor(letterboxed: Bitmap): FloatBuffer =
        bitmapToRgbNchw(letterboxed, DETECT_MEAN, DETECT_STD)

    /** ArcFace recognition tensor from an aligned 112×112 face bitmap. */
    fun toRecognitionTensor(alignedFace: Bitmap): FloatBuffer =
        bitmapToRgbNchw(alignedFace, RECOG_MEAN, RECOG_STD)
}
