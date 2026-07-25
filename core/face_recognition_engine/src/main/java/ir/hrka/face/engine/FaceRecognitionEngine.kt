package ir.hrka.face.engine

import android.content.Context
import android.graphics.Bitmap
import ir.hrka.face.engine.alignment.FaceAligner
import ir.hrka.face.engine.detection.FaceDetector
import ir.hrka.face.engine.detection.Scrfd10GFaceDetector
import ir.hrka.face.engine.detection.UnavailableFaceDetector
import ir.hrka.face.engine.embedding.ArcFaceW600kR50Embedder
import ir.hrka.face.engine.embedding.FaceEmbedder
import ir.hrka.face.engine.model.DetectedFace
import ir.hrka.face.engine.model.FaceEmbedding
import ir.hrka.face.engine.model.RecognitionResult
import ir.hrka.face.engine.onnx.OnnxModelLoader
import ir.hrka.face.engine.onnx.OnnxSessionManager
import ir.hrka.face.engine.recognition.CosineSimilarityCalculator
import ir.hrka.face.engine.recognition.FaceRecognizer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Offline face recognition engine (InsightFace buffalo_l + ONNX Runtime).
 *
 * Models are **not** bundled in the APK. The host app downloads them to device
 * storage and supplies absolute paths via [ModelPaths].
 *
 * Typical usage:
 * ```
 * val models = ModelPaths(
 *     embeddingModelPath = "/data/.../arcface_w600k_r50.onnx",
 *     detectorModelPath = "/data/.../scrfd_10g_kps.onnx", // optional
 * )
 * val engine = FaceRecognitionEngine.create(context, models)
 * try {
 *     val galleryEmbedding = engine.embed(enrollBitmap).copy(personId = "alice")
 *     val result = engine.identify(probeBitmap, listOf(galleryEmbedding))
 * } finally {
 *     engine.close()
 * }
 * ```
 *
 * Thread-safe. Call [close] when finished to release ONNX sessions.
 */
class FaceRecognitionEngine private constructor(
    private val config: EngineConfig,
    private val modelPaths: ModelPaths,
    private val sessionManager: OnnxSessionManager,
    private val detector: FaceDetector,
    private val aligner: FaceAligner,
    private val embedder: FaceEmbedder,
    private val recognizer: FaceRecognizer,
) : AutoCloseable {

    private val closed = AtomicBoolean(false)

    // ------------------------------------------------------------------
    // High-level API
    // ------------------------------------------------------------------

    /**
     * Detect → align → embed the primary face in [image].
     *
     * Primary face = largest box, ties broken by confidence.
     *
     * @throws FaceEngineException.NoFaceDetectedException when no face is found.
     */
    suspend fun embed(image: Bitmap): FaceEmbedding {
        ensureOpen()
        val face = requirePrimaryFace(detectFaces(image))
        return embed(image, face)
    }

    /**
     * Align → embed a specific previously detected [face].
     */
    suspend fun embed(image: Bitmap, face: DetectedFace): FaceEmbedding {
        ensureOpen()
        val aligned = alignFace(image, face)
        try {
            return generateEmbedding(aligned)
        } finally {
            if (!aligned.isRecycled) aligned.recycle()
        }
    }

    /**
     * Full identify pipeline: detect primary face → embed → 1:N match against [gallery].
     *
     * @throws FaceEngineException.NoFaceDetectedException when no face is found.
     */
    suspend fun identify(
        image: Bitmap,
        gallery: List<FaceEmbedding>,
    ): RecognitionResult {
        ensureOpen()
        val embedding = embed(image)
        return recognize(embedding, gallery)
    }

    /**
     * Cosine similarity between two embeddings (usually in `[-1, 1]`).
     * Prefer L2-normalized embeddings (produced by this engine).
     */
    fun similarity(a: FaceEmbedding, b: FaceEmbedding): Float =
        CosineSimilarityCalculator.similarity(a.vector, b.vector)

    // ------------------------------------------------------------------
    // Stage API (optional finer control)
    // ------------------------------------------------------------------

    /** Detects faces + 5-point landmarks (post-NMS, confidence-descending). */
    suspend fun detectFaces(image: Bitmap): List<DetectedFace> {
        ensureOpen()
        return detector.detect(image)
    }

    /** InsightFace 5-point alignment to a 112×112 crop. Caller may recycle the result. */
    suspend fun alignFace(image: Bitmap, face: DetectedFace): Bitmap {
        ensureOpen()
        return aligner.align(image, face)
    }

    /** 512-D L2-normalized ArcFace embedding from an already-aligned 112×112 crop. */
    suspend fun generateEmbedding(alignedFace: Bitmap): FaceEmbedding {
        ensureOpen()
        return embedder.embed(alignedFace)
    }

    /** 1:N gallery match for an existing embedding. */
    suspend fun recognize(
        embedding: FaceEmbedding,
        gallery: List<FaceEmbedding>,
    ): RecognitionResult {
        ensureOpen()
        return recognizer.recognize(embedding, gallery)
    }

    /** Immutable config used by this instance. */
    fun config(): EngineConfig = config

    /** Filesystem model paths used by this instance. */
    fun modelPaths(): ModelPaths = modelPaths

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { detector.close() }
        runCatching { embedder.close() }
        runCatching { sessionManager.close() }
    }

    private fun ensureOpen() {
        if (closed.get()) throw FaceEngineException.EngineClosedException()
    }

    private fun requirePrimaryFace(faces: List<DetectedFace>): DetectedFace {
        if (faces.isEmpty()) throw FaceEngineException.NoFaceDetectedException()
        return selectPrimaryFace(faces)
    }

    companion object {
        /**
         * Creates an engine that loads ONNX models from [models] on device storage.
         *
         * [ModelPaths.embeddingModelPath] is always required.
         * [ModelPaths.detectorModelPath] is optional — omit it when detection is done
         * elsewhere (e.g. ML Kit). Calling [detectFaces] without a detector path fails.
         *
         * @param context Any context; application context is retained.
         * @param models Absolute paths to ONNX files.
         * @param config Optional thresholds / threads.
         * @throws FaceEngineException.ModelNotFoundException if a required model file is missing.
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            @Suppress("UNUSED_PARAMETER") context: Context,
            models: ModelPaths,
            config: EngineConfig = EngineConfig(),
        ): FaceRecognitionEngine {
            val modelLoader = OnnxModelLoader()
            modelLoader.requireModelFile(models.embeddingModelPath)
            models.detectorModelPath?.let(modelLoader::requireModelFile)

            val sessionManager = OnnxSessionManager(modelLoader, models, config)
            val detector: FaceDetector = if (models.detectorModelPath != null) {
                Scrfd10GFaceDetector(sessionManager, config)
            } else {
                UnavailableFaceDetector()
            }
            return FaceRecognitionEngine(
                config = config,
                modelPaths = models,
                sessionManager = sessionManager,
                detector = detector,
                aligner = FaceAligner(EngineConfig.ALIGNED_FACE_SIZE),
                embedder = ArcFaceW600kR50Embedder(sessionManager),
                recognizer = FaceRecognizer(config),
            )
        }

        /** Primary face = max area, then max confidence. */
        fun selectPrimaryFace(faces: List<DetectedFace>): DetectedFace {
            require(faces.isNotEmpty()) { "faces must not be empty" }
            return faces.maxWith(
                compareBy<DetectedFace> { it.boundingBox.area }
                    .thenBy { it.confidenceScore },
            )
        }
    }
}
