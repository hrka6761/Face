package ir.hrka.face.detection.api

import ir.hrka.face.detection.internal.MlKitFaceDetectorEngine

/**
 * Factory for [FaceDetectorEngine] instances.
 *
 * This module does not require Hilt; host apps may wrap creation in DI if desired.
 */
object FaceDetectorFactory {

    /**
     * Creates a new ML Kit-backed face detector.
     *
     * @param options Detector configuration.
     * @return Ready-to-use [FaceDetectorEngine].
     */
    fun create(
        options: FaceDetectorOptions = FaceDetectorOptions(),
    ): FaceDetectorEngine = MlKitFaceDetectorEngine(options)
}
