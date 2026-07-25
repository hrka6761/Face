package ir.hrka.face.engine.onnx

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import ir.hrka.face.engine.EngineConfig
import ir.hrka.face.engine.FaceEngineException
import ir.hrka.face.engine.ModelPaths
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns a shared [OrtEnvironment] and lazily created [OrtSession] instances
 * loaded from on-device filesystem paths.
 *
 * Sessions are confined behind a lock; callers must not retain session references
 * across [close]. Thread-safe.
 */
internal class OnnxSessionManager(
    private val modelLoader: OnnxModelLoader,
    private val modelPaths: ModelPaths,
    private val config: EngineConfig,
) : AutoCloseable {

    private val closed = AtomicBoolean(false)
    private val lock = Any()

    @Volatile
    private var environment: OrtEnvironment? = null

    @Volatile
    private var detectorSession: OrtSession? = null

    @Volatile
    private var embeddingSession: OrtSession? = null

    /** Returns the SCRFD detector session, creating it on first use. */
    fun detectorSession(): OrtSession = synchronized(lock) {
        ensureOpen()
        val path = modelPaths.detectorModelPath
            ?: throw FaceEngineException.ModelNotFoundException(
                "(no detector model path configured)",
            )
        detectorSession ?: createSession(path).also { detectorSession = it }
    }

    /** Returns the ArcFace embedding session, creating it on first use. */
    fun embeddingSession(): OrtSession = synchronized(lock) {
        ensureOpen()
        embeddingSession ?: createSession(modelPaths.embeddingModelPath).also {
            embeddingSession = it
        }
    }

    /** Shared ORT environment (created lazily). */
    fun environment(): OrtEnvironment = synchronized(lock) {
        ensureOpen()
        environment ?: OrtEnvironment.getEnvironment().also { environment = it }
    }

    /** Runs [block] with exclusive access to ORT sessions. */
    fun <T> withLock(block: OnnxSessionManager.() -> T): T = synchronized(lock) {
        ensureOpen()
        block()
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        synchronized(lock) {
            runCatching { detectorSession?.close() }
            runCatching { embeddingSession?.close() }
            detectorSession = null
            embeddingSession = null
            // OrtEnvironment is process-wide; do not close it here.
            environment = null
        }
    }

    private fun ensureOpen() {
        if (closed.get()) throw FaceEngineException.EngineClosedException()
    }

    private fun createSession(modelPath: String): OrtSession {
        val file = modelLoader.requireModelFile(modelPath)
        val env = environment ?: OrtEnvironment.getEnvironment().also { environment = it }
        return try {
            val options = OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(config.numThreads.coerceAtLeast(1))
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                if (config.enableNnapi) {
                    runCatching { addNnapi() }
                }
            }
            // Load from filesystem path — avoids copying large models into a byte[].
            env.createSession(file.absolutePath, options)
        } catch (e: FaceEngineException) {
            throw e
        } catch (e: OrtException) {
            throw FaceEngineException.ModelLoadException(
                "Failed to create ORT session for ${file.absolutePath}: ${e.message}",
                e,
            )
        } catch (e: RuntimeException) {
            throw FaceEngineException.ModelLoadException(
                "Failed to create ORT session for ${file.absolutePath}: ${e.message}",
                e,
            )
        }
    }
}
