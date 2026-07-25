package ir.hrka.face.camera.impl

/**
 * Progress of ensuring on-disk face models / engine startup.
 *
 * @property overallProgress Overall completion in `0f..1f`, or `-1f` when indeterminate.
 * @property label User-facing status text.
 * @property currentFileIndex 1-based index of the active download step (0 when idle/loading).
 * @property totalFiles Total download steps for this prepare pass.
 */
data class ModelDownloadProgress(
    val overallProgress: Float,
    val label: String,
    val currentFileIndex: Int = 0,
    val totalFiles: Int = 0,
)
