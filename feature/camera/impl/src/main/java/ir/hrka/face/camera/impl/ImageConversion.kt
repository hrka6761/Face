package ir.hrka.face.camera.impl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Camera / bitmap conversion helpers for the recognition pipeline.
 */
object ImageConversion {

    private const val TAG = "ImageConversion"

    /**
     * Converts a camera [ImageProxy] to an upright ARGB_8888 [Bitmap].
     *
     * Call this **before** closing [imageProxy].
     */
    fun imageProxyToUprightBitmap(
        imageProxy: ImageProxy,
        rotationDegrees: Int = imageProxy.imageInfo.rotationDegrees,
    ): Bitmap? {
        return try {
            val raw = imageProxyToBitmap(imageProxy) ?: return null
            val upright = if (rotationDegrees % 360 == 0) {
                raw
            } else {
                val rotated = rotateBitmap(raw, rotationDegrees.toFloat())
                if (rotated !== raw && !raw.isRecycled) {
                    raw.recycle()
                }
                rotated
            }
            ensureArgb8888(upright)
        } catch (t: Throwable) {
            Log.e(TAG, "imageProxyToUprightBitmap failed", t)
            null
        }
    }

    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        runCatching { imageProxy.toBitmap() }
            .onSuccess { return it }
            .onFailure { Log.w(TAG, "ImageProxy.toBitmap() failed, trying fallback", it) }

        return when (imageProxy.format) {
            ImageFormat.YUV_420_888 -> yuv420888ToBitmap(imageProxy)
            else -> {
                Log.e(TAG, "Unsupported ImageProxy format: ${imageProxy.format}")
                null
            }
        }
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun ensureArgb8888(source: Bitmap): Bitmap {
        if (!source.isRecycled && source.config == Bitmap.Config.ARGB_8888) {
            return source
        }
        return source.copy(Bitmap.Config.ARGB_8888, false)
            ?: error("Failed to copy bitmap to ARGB_8888 (${source.width}x${source.height})")
    }

    private fun yuv420888ToBitmap(image: ImageProxy): Bitmap? {
        val nv21 = yuv420888ToNv21(image) ?: return null
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 95, out)
        val bytes = out.toByteArray()
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray? {
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0 || image.planes.size < 3) return null

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuffer = yPlane.buffer.duplicate().also { it.clear() }
        val uBuffer = uPlane.buffer.duplicate().also { it.clear() }
        val vBuffer = vPlane.buffer.duplicate().also { it.clear() }

        val nv21 = ByteArray(width * height + width * height / 2)
        var outputOffset = 0

        outputOffset = copyPlane(
            src = yBuffer,
            rowStride = yPlane.rowStride,
            pixelStride = yPlane.pixelStride,
            width = width,
            height = height,
            out = nv21,
            outOffset = outputOffset,
        )

        val chromaHeight = height / 2
        val chromaWidth = width / 2
        val vRowStride = vPlane.rowStride
        val uRowStride = uPlane.rowStride
        val vPixelStride = vPlane.pixelStride
        val uPixelStride = uPlane.pixelStride

        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val vIndex = row * vRowStride + col * vPixelStride
                val uIndex = row * uRowStride + col * uPixelStride
                if (vIndex >= vBuffer.limit() || uIndex >= uBuffer.limit()) continue
                nv21[outputOffset++] = vBuffer.get(vIndex)
                nv21[outputOffset++] = uBuffer.get(uIndex)
            }
        }
        return nv21
    }

    private fun copyPlane(
        src: ByteBuffer,
        rowStride: Int,
        pixelStride: Int,
        width: Int,
        height: Int,
        out: ByteArray,
        outOffset: Int,
    ): Int {
        var offset = outOffset
        for (row in 0 until height) {
            val rowStart = row * rowStride
            if (rowStart >= src.limit()) break
            src.position(rowStart)
            if (pixelStride == 1 && rowStride == width && src.remaining() >= width) {
                src.get(out, offset, width)
                offset += width
            } else {
                val rowData = ByteArray(minOf(rowStride, src.remaining()).coerceAtLeast(0))
                if (rowData.isEmpty()) break
                src.get(rowData)
                var col = 0
                while (col < width) {
                    val idx = col * pixelStride
                    if (idx >= rowData.size) break
                    out[offset++] = rowData[idx]
                    col++
                }
            }
        }
        return offset
    }
}
