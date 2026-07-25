package ir.hrka.face.camera.impl

import android.content.Context
import android.util.Log
import ir.hrka.download.manager.DownloadStorageLocation
import ir.hrka.download.manager.FileCreationMode
import ir.hrka.download.manager.SingleItemDownloadData
import ir.hrka.download.manager.api.DownloadListener
import ir.hrka.download.manager.api.DownloadManager
import ir.hrka.face.engine.ModelPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Ensures the ArcFace ONNX embedding model is present under app internal storage.
 *
 * Detection uses ML Kit ([ir.hrka.face.detection]), so SCRFD is not downloaded.
 * Downloads missing files via [DownloadManager] into `filesDir/face_models/`.
 */
object FaceModelStore {

    const val MODEL_DIRECTORY: String = "face_models"
    const val EMBEDDING_FILE_NAME: String = "arcface_w600k_r50.onnx"

    const val EMBEDDING_URL: String =
        "https://media.githubusercontent.com/media/hrka6761/Face_app_files/refs/heads/main/arcface_w600k_r50.onnx?download=true"

    private const val TAG = "FaceModelStore"

    /**
     * Returns local [ModelPaths] (embedding only), downloading the model if missing.
     *
     * @param onProgress Optional progress callback (may be invoked off the main thread).
     */
    suspend fun ensureModelPaths(
        context: Context,
        onProgress: (ModelDownloadProgress) -> Unit = {},
    ): ModelPaths = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val dir = File(appContext.filesDir, MODEL_DIRECTORY)
        if (!dir.exists() && !dir.mkdirs() && !dir.exists()) {
            error("Could not create model directory: ${dir.absolutePath}")
        }

        val embeddingFile = File(dir, EMBEDDING_FILE_NAME)

        if (!isValidModel(embeddingFile)) {
            Log.i(TAG, "Downloading $EMBEDDING_FILE_NAME…")
            onProgress(
                ModelDownloadProgress(
                    overallProgress = 0f,
                    label = "Downloading face recognition model…",
                    currentFileIndex = 1,
                    totalFiles = 1,
                ),
            )
            downloadToInternal(
                context = appContext,
                url = EMBEDDING_URL,
                fileName = EMBEDDING_FILE_NAME,
                onFileProgress = { fileProgress ->
                    val clamped = if (fileProgress < 0f) -1f else fileProgress.coerceIn(0f, 1f)
                    onProgress(
                        ModelDownloadProgress(
                            overallProgress = clamped,
                            label = "Downloading face recognition model…",
                            currentFileIndex = 1,
                            totalFiles = 1,
                        ),
                    )
                },
            )
        }

        if (!isValidModel(embeddingFile)) {
            error(
                "Face embedding model missing after download. " +
                    "embedding=${embeddingFile.exists()}/${embeddingFile.length()}",
            )
        }

        onProgress(
            ModelDownloadProgress(
                overallProgress = 1f,
                label = "Face recognition model ready",
                currentFileIndex = 1,
                totalFiles = 1,
            ),
        )

        ModelPaths(embeddingModelPath = embeddingFile.absolutePath)
    }

    private fun isValidModel(file: File): Boolean =
        file.exists() && file.isFile && file.length() > 0L

    private suspend fun downloadToInternal(
        context: Context,
        url: String,
        fileName: String,
        onFileProgress: (Float) -> Unit,
    ): String = suspendCancellableCoroutine { cont ->
        val manager = DownloadManager.Builder(context)
            .setSingleItemDownloadData(
                SingleItemDownloadData(
                    url = url,
                    fileName = fileName,
                ),
            )
            .setFileLocation(DownloadStorageLocation.Internal)
            .setDirectories(listOf(MODEL_DIRECTORY))
            .setFileCreationMode(FileCreationMode.Overwrite)
            .setDownloadListener(
                object : DownloadListener {
                    override fun onStartDownload() {
                        Log.d(TAG, "Download started: $fileName")
                        onFileProgress(0f)
                    }

                    override fun onDownloading(
                        receivedBytes: Long,
                        downloadRate: Long,
                        remainingTime: Long,
                        progress: Float,
                        currentPartIndex: Int,
                        totalParts: Int,
                    ) {
                        onFileProgress(progress)
                    }

                    override fun onDownloadSuccess(filePath: String?) {
                        if (cont.isActive) {
                            onFileProgress(1f)
                            val path = filePath
                                ?: File(context.filesDir, "$MODEL_DIRECTORY/$fileName").absolutePath
                            cont.resume(path)
                        }
                    }

                    override fun onDownloadFailed(errorMsg: String?) {
                        if (cont.isActive) {
                            cont.resumeWithException(
                                IllegalStateException(
                                    errorMsg ?: "Failed to download $fileName",
                                ),
                            )
                        }
                    }

                    override fun onDownloadPaused(
                        receivedBytes: Long,
                        downloadRate: Long,
                        remainingTime: Long,
                        progress: Float,
                        currentPartIndex: Int,
                        totalParts: Int,
                    ) = Unit

                    override fun onDownloadCancelled() {
                        if (cont.isActive) {
                            cont.resumeWithException(
                                IllegalStateException("Download cancelled: $fileName"),
                            )
                        }
                    }
                },
            )
            .build()

        manager.startDownload()
        cont.invokeOnCancellation {
            runCatching { manager.stopDownload() }
        }
    }
}
