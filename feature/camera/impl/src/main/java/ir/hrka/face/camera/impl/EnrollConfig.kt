package ir.hrka.face.camera.impl

/**
 * Guided enrollment / pose constants previously hosted by MobileFaceNet config.
 */
object EnrollConfig {
    const val ENROLL_SAMPLES_PER_POSE: Int = 4
    const val ENROLL_POSE_STEPS: Int = 3
    const val ENROLL_TARGET_TEMPLATES: Int = ENROLL_SAMPLES_PER_POSE * ENROLL_POSE_STEPS
    const val ENROLL_TIMEOUT_MS: Long = 90_000L

    const val POSE_FRONT_YAW_MAX: Float = 15f
    const val POSE_PROFILE_YAW_MIN: Float = 28f
    const val POSE_PROFILE_YAW_MAX: Float = 65f
}
