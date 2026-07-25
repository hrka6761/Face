package ir.hrka.face.engine.embedding

import ai.onnxruntime.OnnxTensor
import android.graphics.Bitmap
import ir.hrka.face.engine.EngineConfig
import ir.hrka.face.engine.FaceEngineException
import ir.hrka.face.engine.model.FaceEmbedding
import ir.hrka.face.engine.onnx.OnnxSessionManager
import ir.hrka.face.engine.preprocessing.ImagePreprocessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.sqrt

/**
 * ArcFace embedding extractor for buffalo_l-compatible ONNX models.
 *
 * Default weights: WebFace600K ResNet50 (`arcface_w600k_r50.onnx`).
 * Also supports glintr100 when configured via [EngineConfig.recognitionModel].
 *
 * Input: aligned 112×112 RGB face (InsightFace `norm_crop` output).
 * Output: L2-normalized [FloatArray] of size 512.
 */
internal class ArcFaceW600kR50Embedder(
    private val sessionManager: OnnxSessionManager,
    private val inputSize: Int = EngineConfig.ALIGNED_FACE_SIZE,
    private val embeddingDim: Int = EngineConfig.EMBEDDING_DIM,
) : FaceEmbedder {

    override suspend fun embed(alignedFace: Bitmap): FaceEmbedding =
        withContext(Dispatchers.Default) {
            if (alignedFace.isRecycled) {
                throw FaceEngineException.InvalidInputException("Aligned face bitmap is recycled.")
            }
            if (alignedFace.width != inputSize || alignedFace.height != inputSize) {
                throw FaceEngineException.InvalidInputException(
                    "Aligned face must be ${inputSize}x$inputSize, " +
                        "got ${alignedFace.width}x${alignedFace.height}.",
                )
            }

            val tensorData = ImagePreprocessor.toRecognitionTensor(alignedFace)
            sessionManager.withLock {
                val session = embeddingSession()
                val env = environment()
                val inputName = session.inputNames.first()
                val shape = longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())
                OnnxTensor.createTensor(env, tensorData, shape).use { inputTensor ->
                    val results = try {
                        session.run(mapOf(inputName to inputTensor))
                    } catch (e: Exception) {
                        throw FaceEngineException.InferenceException(
                            "ArcFace inference failed: ${e.message}",
                            e,
                        )
                    }
                    results.use { outputs ->
                        val outTensor = outputs.get(0) as? OnnxTensor
                            ?: throw FaceEngineException.InferenceException(
                                "Missing ArcFace embedding output.",
                            )
                        val raw = readEmbedding(outTensor)
                        if (raw.size != embeddingDim) {
                            throw FaceEngineException.InferenceException(
                                "Unexpected embedding size ${raw.size}, expected $embeddingDim.",
                            )
                        }
                        val normalized = l2Normalize(raw)
                        FaceEmbedding(
                            id = UUID.randomUUID().toString(),
                            personId = null,
                            vector = normalized,
                            isNormalized = true,
                        )
                    }
                }
            }
        }

    override fun close() {
        // Sessions are owned by OnnxSessionManager.
    }

    companion object {
        /**
         * L2-normalizes [values]. Returns a copy; zero-norm vectors are copied unchanged.
         * Pure function — unit-testable without ONNX.
         */
        fun l2Normalize(values: FloatArray): FloatArray {
            var sumSq = 0f
            for (v in values) sumSq += v * v
            val norm = sqrt(sumSq)
            if (norm == 0f) return values.copyOf()
            val inv = 1f / norm
            return FloatArray(values.size) { i -> values[i] * inv }
        }

        /** L2 norm of [values]. */
        fun l2Norm(values: FloatArray): Float {
            var sumSq = 0f
            for (v in values) sumSq += v * v
            return sqrt(sumSq)
        }
    }

    private fun readEmbedding(tensor: OnnxTensor): FloatArray {
        val value = tensor.value
        return when (value) {
            is FloatArray -> value
            is Array<*> -> {
                val first = value.firstOrNull()
                when (first) {
                    is FloatArray -> first
                    is Float -> value.map { (it as Float) }.toFloatArray()
                    is Number -> value.map { (it as Number).toFloat() }.toFloatArray()
                    else -> tensor.floatBuffer.let { buf ->
                        FloatArray(buf.remaining()).also { buf.get(it) }
                    }
                }
            }
            else -> {
                val buf = tensor.floatBuffer
                FloatArray(buf.remaining()).also { buf.get(it) }
            }
        }
    }
}
