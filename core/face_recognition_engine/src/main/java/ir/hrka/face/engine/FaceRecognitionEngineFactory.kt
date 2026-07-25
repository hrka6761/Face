package ir.hrka.face.engine

import android.content.Context

/**
 * Thin alias for [FaceRecognitionEngine.create].
 *
 * Prefer `FaceRecognitionEngine.create(context, models)` at call sites.
 */
object FaceRecognitionEngineFactory {

    /** @see FaceRecognitionEngine.create */
    fun create(
        context: Context,
        models: ModelPaths,
        config: EngineConfig = EngineConfig(),
    ): FaceRecognitionEngine = FaceRecognitionEngine.create(context, models, config)
}
