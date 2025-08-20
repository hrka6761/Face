package ir.hrka.face.core.utilities

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import ir.hrka.face.core.utilities.Constants.FACE_NET_INPUT_IMAGE_SIZE
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import androidx.core.graphics.scale

class FaceEmbeddingGenerator @Inject constructor(
    private val interpreter: Interpreter,
    private val imageProcessor: ImageProcessor
) {

    private val embedding = Array(1) { FloatArray(192) }


    fun generate(imageProxy: ImageProxy, boundingBox: Rect): Array<FloatArray>? {
        val faceBitmap = provideFaceBitmap(imageProxy, boundingBox)

        return if (faceBitmap != null) {
            val tensorImg = TensorImage.fromBitmap(resizeFaceBitmapToFaceNetInputSize(faceBitmap))
            val tensorBuffer = imageProcessor.process(tensorImg).buffer
            interpreter.run(tensorBuffer, embedding)

            embedding
        } else
            null
    }


    private fun provideFaceBitmap(imageProxy: ImageProxy, boundingBox: Rect): Bitmap? {
        val imageRotationDegree = imageProxy.imageInfo.rotationDegrees
        val originalBitmap = imageProxyToBitmap(imageProxy)

        return if (imageRotationDegree != 90)
            inferFaceBitmapFromOriginalBitmap(rotateOriginalBitmap(originalBitmap), boundingBox)
        else
            inferFaceBitmapFromOriginalBitmap(originalBitmap, boundingBox)
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun rotateOriginalBitmap(originalBitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f)

        return Bitmap.createBitmap(
            originalBitmap,
            0, 0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )
    }

    private fun inferFaceBitmapFromOriginalBitmap(
        originalBitmap: Bitmap,
        boundingBox: Rect
    ): Bitmap? {
        val x = boundingBox.left
        val y = boundingBox.top
        val with = boundingBox.width()
        val height = boundingBox.height()

        if (x < 0 || y < 0)
            return null

        if (x + with > originalBitmap.width)
            return null

        if (y + height > originalBitmap.height)
            return null

        return Bitmap.createBitmap(originalBitmap, x, y, with, height)
    }

    private fun resizeFaceBitmapToFaceNetInputSize(originalBitmap: Bitmap): Bitmap =
        originalBitmap.scale(FACE_NET_INPUT_IMAGE_SIZE, FACE_NET_INPUT_IMAGE_SIZE, false)
}