package ir.hrka.face.engine.detection

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.PointF
import ir.hrka.face.engine.EngineConfig
import ir.hrka.face.engine.FaceEngineException
import ir.hrka.face.engine.model.BoundingBox
import ir.hrka.face.engine.model.DetectedFace
import ir.hrka.face.engine.model.FaceLandmarks
import ir.hrka.face.engine.onnx.OnnxSessionManager
import ir.hrka.face.engine.preprocessing.ImagePreprocessor
import ir.hrka.face.engine.preprocessing.LetterboxResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * SCRFD-10G_KPS face detector running on ONNX Runtime.
 *
 * Implements InsightFace-compatible multi-stride FPN decode (strides 8/16/32),
 * score filtering, landmark decode, letterbox undo, and NMS.
 */
internal class Scrfd10GFaceDetector(
    private val sessionManager: OnnxSessionManager,
    private val config: EngineConfig,
) : FaceDetector {

    override suspend fun detect(image: Bitmap): List<DetectedFace> = withContext(Dispatchers.Default) {
        if (image.isRecycled) {
            throw FaceEngineException.InvalidInputException("Input bitmap is recycled.")
        }
        if (image.width <= 0 || image.height <= 0) {
            throw FaceEngineException.InvalidInputException("Input bitmap has invalid size.")
        }

        val letterbox = ImagePreprocessor.letterbox(image, config.detectorInputSize)
        try {
            val tensorData = ImagePreprocessor.toDetectionTensor(letterbox.bitmap)
            sessionManager.withLock {
                val session = detectorSession()
                val env = environment()
                val inputName = session.inputNames.first()
                val shape = longArrayOf(
                    1,
                    3,
                    config.detectorInputSize.toLong(),
                    config.detectorInputSize.toLong(),
                )
                OnnxTensor.createTensor(env, tensorData, shape).use { inputTensor ->
                    val results = try {
                        session.run(mapOf(inputName to inputTensor))
                    } catch (e: Exception) {
                        throw FaceEngineException.InferenceException(
                            "SCRFD inference failed: ${e.message}",
                            e,
                        )
                    }
                    results.use { outputs ->
                        decodeOutputs(
                            orderedOutputs = orderedOutputTensors(session, outputs),
                            letterbox = letterbox,
                            scoreThreshold = config.detectorScoreThreshold,
                            nmsThreshold = config.nmsThreshold,
                        )
                    }
                }
            }
        } finally {
            if (!letterbox.bitmap.isRecycled) {
                letterbox.bitmap.recycle()
            }
        }
    }

    override fun close() {
        // Sessions are owned by OnnxSessionManager.
    }

    companion object {
        /** Feature strides used by SCRFD-10G family. */
        val DEFAULT_STRIDES: IntArray = intArrayOf(8, 16, 32)

        /** Typical anchor count per location for 9-output SCRFD packs. */
        const val DEFAULT_NUM_ANCHORS: Int = 2

        /**
         * Decodes distance predictions relative to anchor centers into boxes.
         * Pure function — unit-testable without ONNX.
         */
        fun distanceToBbox(
            anchorX: Float,
            anchorY: Float,
            distLeft: Float,
            distTop: Float,
            distRight: Float,
            distBottom: Float,
        ): BoundingBox = BoundingBox(
            left = anchorX - distLeft,
            top = anchorY - distTop,
            right = anchorX + distRight,
            bottom = anchorY + distBottom,
        )

        /**
         * Greedy NMS on detections sorted by descending score.
         * Pure function — unit-testable without ONNX.
         */
        fun nms(faces: List<DetectedFace>, iouThreshold: Float): List<DetectedFace> {
            if (faces.isEmpty()) return emptyList()
            val ordered = faces.sortedByDescending { it.confidenceScore }
            val kept = ArrayList<DetectedFace>(ordered.size)
            val suppressed = BooleanArray(ordered.size)
            for (i in ordered.indices) {
                if (suppressed[i]) continue
                val a = ordered[i]
                kept.add(a)
                for (j in i + 1 until ordered.size) {
                    if (suppressed[j]) continue
                    if (iou(a.boundingBox, ordered[j].boundingBox) > iouThreshold) {
                        suppressed[j] = true
                    }
                }
            }
            return kept
        }

        /** Intersection-over-union of two boxes. */
        fun iou(a: BoundingBox, b: BoundingBox): Float {
            val interLeft = max(a.left, b.left)
            val interTop = max(a.top, b.top)
            val interRight = min(a.right, b.right)
            val interBottom = min(a.bottom, b.bottom)
            val interW = (interRight - interLeft).coerceAtLeast(0f)
            val interH = (interBottom - interTop).coerceAtLeast(0f)
            val inter = interW * interH
            val union = a.area + b.area - inter
            return if (union <= 0f) 0f else inter / union
        }

        /**
         * Builds anchor centers for a feature map.
         * Pure function — unit-testable without ONNX.
         */
        fun buildAnchorCenters(
            height: Int,
            width: Int,
            stride: Int,
            numAnchors: Int,
        ): FloatArray {
            val out = FloatArray(height * width * numAnchors * 2)
            var idx = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val cx = (x + 0.5f) * stride
                    val cy = (y + 0.5f) * stride
                    repeat(numAnchors) {
                        out[idx++] = cx
                        out[idx++] = cy
                    }
                }
            }
            return out
        }
    }

    /**
     * Collects outputs in model-defined order (not [Set] iteration order).
     */
    private fun orderedOutputTensors(
        session: OrtSession,
        outputs: OrtSession.Result,
    ): List<OnnxTensor> {
        // Prefer index access — Result preserves model output order.
        val count = session.outputInfo.size
        return List(count) { index ->
            val value = outputs.get(index)
            value as? OnnxTensor
                ?: throw FaceEngineException.InferenceException(
                    "SCRFD output[$index] is not a float tensor (${value?.javaClass?.simpleName})",
                )
        }
    }

    private fun decodeOutputs(
        orderedOutputs: List<OnnxTensor>,
        letterbox: LetterboxResult,
        scoreThreshold: Float,
        nmsThreshold: Float,
    ): List<DetectedFace> {
        val outputCount = orderedOutputs.size
        val (fmc, strides, numAnchors, useKps) = inferHeadLayout(outputCount)

        val raw = ArrayList<DetectedFace>(64)
        for (level in 0 until fmc) {
            val scoreTensor = orderedOutputs[level]
            val bboxTensor = orderedOutputs[level + fmc]
            val kpsTensor = if (useKps) orderedOutputs[level + fmc * 2] else null

            val scores = floatArrayFrom(scoreTensor)
            val bboxes = floatArrayFrom(bboxTensor)
            val kps = kpsTensor?.let { floatArrayFrom(it) }

            val stride = strides[level]
            val featH = letterbox.inputSize / stride
            val featW = letterbox.inputSize / stride
            val anchors = buildAnchorCenters(featH, featW, stride, numAnchors)
            val locations = featH * featW * numAnchors

            if (scores.size < locations) {
                throw FaceEngineException.InferenceException(
                    "Score tensor too small for stride=$stride: ${scores.size} < $locations",
                )
            }

            for (i in 0 until locations) {
                val score = asProbability(scores[i])
                if (score < scoreThreshold) continue

                val boxBase = i * 4
                if (boxBase + 3 >= bboxes.size) break
                val distL = bboxes[boxBase] * stride
                val distT = bboxes[boxBase + 1] * stride
                val distR = bboxes[boxBase + 2] * stride
                val distB = bboxes[boxBase + 3] * stride
                val ax = anchors[i * 2]
                val ay = anchors[i * 2 + 1]
                val networkBox = distanceToBbox(ax, ay, distL, distT, distR, distB)

                val landmarks = if (kps != null) {
                    val kpsBase = i * 10
                    if (kpsBase + 9 >= kps.size) continue
                    val points = Array(5) { p ->
                        val px = ax + kps[kpsBase + p * 2] * stride
                        val py = ay + kps[kpsBase + p * 2 + 1] * stride
                        PointF(
                            clamp(letterbox.toSourceX(px), 0f, letterbox.sourceWidth - 1f),
                            clamp(letterbox.toSourceY(py), 0f, letterbox.sourceHeight - 1f),
                        )
                    }
                    FaceLandmarks(
                        leftEye = points[0],
                        rightEye = points[1],
                        nose = points[2],
                        leftMouth = points[3],
                        rightMouth = points[4],
                    )
                } else {
                    // Should not happen for SCRFD_KPS; synthesize from box center as last resort.
                    val cx = (networkBox.left + networkBox.right) / 2f
                    val cy = (networkBox.top + networkBox.bottom) / 2f
                    val sx = letterbox.toSourceX(cx)
                    val sy = letterbox.toSourceY(cy)
                    val p = PointF(sx, sy)
                    FaceLandmarks(p, p, p, p, p)
                }

                val sourceBox = BoundingBox(
                    left = clamp(letterbox.toSourceX(networkBox.left), 0f, letterbox.sourceWidth - 1f),
                    top = clamp(letterbox.toSourceY(networkBox.top), 0f, letterbox.sourceHeight - 1f),
                    right = clamp(letterbox.toSourceX(networkBox.right), 0f, letterbox.sourceWidth - 1f),
                    bottom = clamp(letterbox.toSourceY(networkBox.bottom), 0f, letterbox.sourceHeight - 1f),
                )

                raw.add(
                    DetectedFace(
                        boundingBox = sourceBox,
                        confidenceScore = score,
                        landmarks = landmarks,
                    ),
                )
            }
        }

        return nms(raw, nmsThreshold)
    }

    private data class HeadLayout(
        val fmc: Int,
        val strides: IntArray,
        val numAnchors: Int,
        val useKps: Boolean,
    )

    private fun inferHeadLayout(outputCount: Int): HeadLayout = when (outputCount) {
        6 -> HeadLayout(3, DEFAULT_STRIDES, DEFAULT_NUM_ANCHORS, useKps = false)
        9 -> HeadLayout(3, DEFAULT_STRIDES, DEFAULT_NUM_ANCHORS, useKps = true)
        10 -> HeadLayout(3, DEFAULT_STRIDES, 1, useKps = true)
        15 -> HeadLayout(5, intArrayOf(8, 16, 32, 64, 128), 1, useKps = true)
        else -> throw FaceEngineException.InferenceException(
            "Unsupported SCRFD output count: $outputCount. Expected 6, 9, 10, or 15.",
        )
    }

    private fun floatArrayFrom(tensor: OnnxTensor): FloatArray {
        val value = tensor.value
        return when (value) {
            is FloatArray -> value
            is Array<*> -> flattenFloatArray(value)
            is FloatBuffer -> {
                val buf = value.duplicate()
                buf.rewind()
                FloatArray(buf.remaining()).also { buf.get(it) }
            }
            else -> {
                val buffer = tensor.floatBuffer
                val arr = FloatArray(buffer.remaining())
                buffer.get(arr)
                arr
            }
        }
    }

    private fun flattenFloatArray(value: Array<*>): FloatArray {
        val out = ArrayList<Float>(256)
        fun walk(node: Any?) {
            when (node) {
                null -> Unit
                is Float -> out.add(node)
                is FloatArray -> node.forEach { out.add(it) }
                is Array<*> -> node.forEach { walk(it) }
                is Number -> out.add(node.toFloat())
                else -> Unit
            }
        }
        walk(value)
        return out.toFloatArray()
    }

    private fun asProbability(raw: Float): Float {
        // Some exports are logits; values already in [0,1] pass through.
        return if (raw in 0f..1f) raw else (1f / (1f + exp(-raw)))
    }

    private fun clamp(v: Float, minV: Float, maxV: Float): Float = min(max(v, minV), maxV)
}
