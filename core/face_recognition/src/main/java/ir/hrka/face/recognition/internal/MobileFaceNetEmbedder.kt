package ir.hrka.face.recognition.internal

import android.content.Context
import android.graphics.Bitmap
import ir.hrka.face.model.DetectedFace
import ir.hrka.face.model.FaceEmbedding
import ir.hrka.face.recognition.api.FaceEmbedder
import ir.hrka.face.recognition.api.FaceRecognitionConfig
import ir.hrka.face.recognition.api.FaceRecognitionException
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * MobileFaceNet TFLite [FaceEmbedder] with multi-scale robust embedding.
 *
 * @param context Application context for asset loading.
 * @param config Model / preprocessing configuration.
 */
internal class MobileFaceNetEmbedder(
    context: Context,
    private val config: FaceRecognitionConfig,
) : FaceEmbedder {

    private val interpreter: Interpreter
    private val imageProcessor: ImageProcessor
    private val lock = Any()

    init {
        try {
            val options = Interpreter.Options().apply {
                setNumThreads(config.numThreads)
            }
            interpreter = Interpreter(loadModelFile(context, config.modelAssetPath), options)
            imageProcessor = ImageProcessor.Builder()
                .add(
                    ResizeOp(
                        config.inputSize,
                        config.inputSize,
                        ResizeOp.ResizeMethod.BILINEAR,
                    ),
                )
                .add(NormalizeOp(0f, 255f))
                .build()
        } catch (t: Throwable) {
            throw FaceRecognitionException(
                message = "Failed to load face recognition model: ${t.message}",
                cause = t,
            )
        }
    }

    override fun embed(bitmap: Bitmap, face: DetectedFace): FaceEmbedding? {
        val faceBitmap = FaceAligner.cropAlignedFace(bitmap, face, config.inputSize) ?: return null
        return runInference(faceBitmap)
    }

    override fun embedRobust(bitmap: Bitmap, face: DetectedFace): FaceEmbedding? {
        val scales = embedMultiScale(bitmap, face)
        if (scales.isEmpty()) return null
        if (scales.size == 1) return scales.first()
        return FaceEmbedding(EmbeddingMath.average(scales.map { it.values }))
    }

    override fun embedMultiScale(bitmap: Bitmap, face: DetectedFace): List<FaceEmbedding> {
        val crops = FaceAligner.cropMultiScale(bitmap, face, config.inputSize)
        if (crops.isEmpty()) return emptyList()
        val results = ArrayList<FaceEmbedding>(crops.size)
        for (crop in crops) {
            try {
                results += runInference(crop)
            } catch (_: FaceRecognitionException) {
                // Skip failed scale.
            } finally {
                if (!crop.isRecycled) crop.recycle()
            }
        }
        return results
    }

    override fun embedAll(bitmap: Bitmap, faces: List<DetectedFace>): Map<Int, FaceEmbedding> {
        val result = LinkedHashMap<Int, FaceEmbedding>(faces.size)
        for (face in faces) {
            val embedding = embedRobust(bitmap, face) ?: continue
            result[face.trackingId] = embedding
        }
        return result
    }

    override fun close() {
        synchronized(lock) {
            interpreter.close()
        }
    }

    private fun runInference(faceBitmap: Bitmap): FaceEmbedding {
        synchronized(lock) {
            return try {
                if (faceBitmap.isRecycled) {
                    throw FaceRecognitionException("Face crop bitmap is recycled")
                }
                val argb = if (faceBitmap.config == Bitmap.Config.ARGB_8888) {
                    faceBitmap
                } else {
                    faceBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        ?: throw FaceRecognitionException("Unable to convert face crop to ARGB_8888")
                }
                val tensorImage = TensorImage.fromBitmap(argb)
                val processed = imageProcessor.process(tensorImage)
                val output = Array(1) { FloatArray(config.embeddingDim) }
                interpreter.run(processed.buffer, output)
                if (argb !== faceBitmap && !argb.isRecycled) {
                    argb.recycle()
                }
                FaceEmbedding(EmbeddingMath.l2Normalize(output[0]))
            } catch (t: FaceRecognitionException) {
                throw t
            } catch (t: Throwable) {
                throw FaceRecognitionException(
                    message = "Face embedding inference failed: ${t::class.java.simpleName}: ${t.message}",
                    cause = t,
                )
            }
        }
    }

    private fun loadModelFile(context: Context, assetPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(assetPath)
        FileInputStream(fileDescriptor.fileDescriptor).use { input ->
            val channel = input.channel
            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                fileDescriptor.startOffset,
                fileDescriptor.declaredLength,
            )
        }
    }
}
