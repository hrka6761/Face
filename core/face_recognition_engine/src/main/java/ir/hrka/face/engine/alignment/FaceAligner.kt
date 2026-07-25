package ir.hrka.face.engine.alignment

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import ir.hrka.face.engine.EngineConfig
import ir.hrka.face.engine.FaceEngineException
import ir.hrka.face.engine.model.DetectedFace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * InsightFace 5-point similarity-transform face alignment (`norm_crop`).
 *
 * Warps a detected face into a canonical ArcFace 112×112 crop using the
 * standard ArcFace destination landmark template.
 */
internal class FaceAligner(
    private val outputSize: Int = EngineConfig.ALIGNED_FACE_SIZE,
) {

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    /**
     * Aligns [face] from [image] into an [outputSize]×[outputSize] bitmap.
     *
     * @throws FaceEngineException.InvalidInputException when inputs are invalid.
     */
    suspend fun align(image: Bitmap, face: DetectedFace): Bitmap =
        withContext(Dispatchers.Default) {
            alignBlocking(image, face)
        }

    /**
     * Blocking variant for tests / internal callers already on a worker thread.
     */
    fun alignBlocking(image: Bitmap, face: DetectedFace): Bitmap {
        if (image.isRecycled) {
            throw FaceEngineException.InvalidInputException("Source bitmap is recycled.")
        }
        if (outputSize <= 0) {
            throw FaceEngineException.InvalidInputException("outputSize must be > 0.")
        }

        val src = face.landmarks.toFloatArray()
        val dst = arcfaceDestinationLandmarkArray(outputSize)
        val matrixValues = estimateSimilarityTransform(src, dst)
            ?: throw FaceEngineException.InvalidInputException(
                "Failed to estimate InsightFace similarity transform from landmarks.",
            )

        val matrix = Matrix().apply { setValues(matrixValues) }
        return try {
            val output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(image, matrix, bitmapPaint)
            output
        } catch (e: RuntimeException) {
            throw FaceEngineException.InvalidInputException(
                "Face alignment warp failed: ${e.message}",
                e,
            )
        }
    }

    companion object {
        /**
         * Canonical ArcFace 112×112 destination landmarks (InsightFace `arcface_dst`),
         * flattened as `[x0,y0,...,x4,y4]`.
         *
         * Order: left eye, right eye, nose, left mouth, right mouth.
         */
        val ARCFACE_DST_112: FloatArray = floatArrayOf(
            38.2946f, 51.6963f,
            73.5318f, 51.5014f,
            56.0252f, 71.7366f,
            41.5493f, 92.3655f,
            70.7299f, 92.2041f,
        )

        /**
         * Scales [ARCFACE_DST_112] for an arbitrary multiple of 112 (InsightFace rule).
         * Returns flattened `[x0,y0,...,x4,y4]` (JVM-unit-test friendly; no Android stubs).
         */
        fun arcfaceDestinationLandmarkArray(imageSize: Int): FloatArray {
            require(imageSize % 112 == 0 || imageSize % 128 == 0) {
                "imageSize must be a multiple of 112 or 128"
            }
            val ratio: Float
            val diffX: Float
            if (imageSize % 112 == 0) {
                ratio = imageSize / 112f
                diffX = 0f
            } else {
                ratio = imageSize / 128f
                diffX = 8f * ratio
            }
            val out = FloatArray(ARCFACE_DST_112.size)
            var i = 0
            while (i < ARCFACE_DST_112.size) {
                out[i] = ARCFACE_DST_112[i] * ratio + diffX
                out[i + 1] = ARCFACE_DST_112[i + 1] * ratio
                i += 2
            }
            return out
        }

        /** PointF view of [arcfaceDestinationLandmarkArray] for Android callers. */
        fun arcfaceDestinationLandmarks(imageSize: Int): List<PointF> {
            val arr = arcfaceDestinationLandmarkArray(imageSize)
            return List(5) { i -> PointF(arr[i * 2], arr[i * 2 + 1]) }
        }

        /**
         * Estimates a similarity transform mapping [src] → [dst]:
         *
         * ```
         * x' = a·x − b·y + tx
         * y' = b·x + a·y + ty
         * ```
         *
         * [src]/[dst] are flattened landmark arrays `[x0,y0,...,x4,y4]`.
         * Solved by linear least squares. Returns Android [Matrix] values (9 floats),
         * or `null` if the system is degenerate.
         *
         * Pure function — unit-testable without ONNX / Android stubs.
         */
        fun estimateSimilarityTransform(
            src: FloatArray,
            dst: FloatArray,
        ): FloatArray? {
            if (src.size != dst.size || src.size < 4 || src.size % 2 != 0) return null
            val pointCount = src.size / 2

            // Normal equations for 4 unknowns [a, b, tx, ty].
            val ata = Array(4) { DoubleArray(4) }
            val atb = DoubleArray(4)

            fun accumulate(row: DoubleArray, rhs: Double) {
                for (i in 0 until 4) {
                    for (j in 0 until 4) {
                        ata[i][j] += row[i] * row[j]
                    }
                    atb[i] += row[i] * rhs
                }
            }

            for (p in 0 until pointCount) {
                val x = src[p * 2].toDouble()
                val y = src[p * 2 + 1].toDouble()
                val u = dst[p * 2].toDouble()
                val v = dst[p * 2 + 1].toDouble()
                accumulate(doubleArrayOf(x, -y, 1.0, 0.0), u)
                accumulate(doubleArrayOf(y, x, 0.0, 1.0), v)
            }

            val solved = solve4x4(ata, atb) ?: return null
            val a = solved[0]
            val b = solved[1]
            val tx = solved[2]
            val ty = solved[3]

            return floatArrayOf(
                a.toFloat(), (-b).toFloat(), tx.toFloat(),
                b.toFloat(), a.toFloat(), ty.toFloat(),
                0f, 0f, 1f,
            )
        }

        /** Gaussian elimination with partial pivoting for a 4×4 system. */
        private fun solve4x4(aIn: Array<DoubleArray>, bIn: DoubleArray): DoubleArray? {
            val a = Array(4) { i -> aIn[i].clone() }
            val b = bIn.clone()
            for (col in 0 until 4) {
                var pivot = col
                var maxAbs = abs(a[col][col])
                for (row in col + 1 until 4) {
                    val v = abs(a[row][col])
                    if (v > maxAbs) {
                        maxAbs = v
                        pivot = row
                    }
                }
                if (maxAbs < 1e-12) return null
                if (pivot != col) {
                    val tmp = a[col]
                    a[col] = a[pivot]
                    a[pivot] = tmp
                    val tb = b[col]
                    b[col] = b[pivot]
                    b[pivot] = tb
                }
                val diag = a[col][col]
                for (row in col + 1 until 4) {
                    val factor = a[row][col] / diag
                    for (j in col until 4) {
                        a[row][j] -= factor * a[col][j]
                    }
                    b[row] -= factor * b[col]
                }
            }
            val x = DoubleArray(4)
            for (i in 3 downTo 0) {
                var sum = b[i]
                for (j in i + 1 until 4) {
                    sum -= a[i][j] * x[j]
                }
                x[i] = sum / a[i][i]
            }
            return x
        }
    }
}
