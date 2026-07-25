package ir.hrka.face.engine.onnx

import ir.hrka.face.engine.FaceEngineException
import java.io.File

/**
 * Resolves ONNX models from absolute filesystem paths on device storage.
 *
 * Models are never read from APK assets and are never downloaded by this module.
 */
internal class OnnxModelLoader {

    /**
     * Validates that [path] points to a readable, non-empty file.
     *
     * @return Canonical [File] for the model.
     * @throws FaceEngineException.ModelNotFoundException when missing/unreadable.
     */
    fun requireModelFile(path: String): File {
        val trimmed = path.trim()
        if (trimmed.isEmpty()) {
            throw FaceEngineException.ModelNotFoundException("(empty path)")
        }
        val file = File(trimmed)
        if (!file.exists() || !file.isFile) {
            throw FaceEngineException.ModelNotFoundException(file.absolutePath)
        }
        if (!file.canRead()) {
            throw FaceEngineException.ModelNotFoundException(
                file.absolutePath,
                SecurityException("No read permission for model file"),
            )
        }
        if (file.length() <= 0L) {
            throw FaceEngineException.ModelLoadException(
                "ONNX model file is empty: ${file.absolutePath}",
            )
        }
        return file
    }

    /** Returns `true` when [path] is a readable non-empty file. */
    fun exists(path: String): Boolean = runCatching {
        requireModelFile(path)
        true
    }.getOrDefault(false)
}
