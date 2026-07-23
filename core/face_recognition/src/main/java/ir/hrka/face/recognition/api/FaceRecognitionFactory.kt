package ir.hrka.face.recognition.api

import android.content.Context
import ir.hrka.face.recognition.internal.MobileFaceNetEmbedder
import ir.hrka.face.recognition.internal.CosineFaceMatcher

/**
 * Factory for face recognition components.
 *
 * No Hilt required; host apps may wrap creation in DI.
 */
object FaceRecognitionFactory {

    /**
     * Creates a MobileFaceNet [FaceEmbedder] that loads the model from module assets.
     *
     * @param context Any context; application context is used internally.
     * @param config Recognition configuration.
     * @throws FaceRecognitionException if the model cannot be loaded.
     */
    fun createEmbedder(
        context: Context,
        config: FaceRecognitionConfig = FaceRecognitionConfig(),
    ): FaceEmbedder = MobileFaceNetEmbedder(context.applicationContext, config)

    /**
     * Creates a cosine-similarity [FaceMatcher].
     */
    fun createMatcher(): FaceMatcher = CosineFaceMatcher()
}
