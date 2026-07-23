package ir.hrka.face.detection.api

/**
 * Configuration for creating a [FaceDetectorEngine].
 *
 * @property performanceMode Prefer accuracy or speed.
 * @property landmarkMode Whether facial landmarks are computed (needed for alignment).
 * @property minFaceSize Minimum face size as a fraction of the image width (`0..1`).
 * @property enableTracking Whether ML Kit should assign tracking ids across frames.
 */
data class FaceDetectorOptions(
    val performanceMode: PerformanceMode = PerformanceMode.ACCURATE,
    val landmarkMode: LandmarkMode = LandmarkMode.ALL,
    val minFaceSize: Float = 0.08f,
    val enableTracking: Boolean = true,
) {
    /**
     * Detector performance trade-off.
     */
    enum class PerformanceMode {
        /** Higher quality, slower. */
        ACCURATE,

        /** Faster, lower quality. */
        FAST,
    }

    /**
     * Landmark computation mode.
     */
    enum class LandmarkMode {
        /** No landmarks. */
        NONE,

        /** All supported landmarks. */
        ALL,
    }
}
