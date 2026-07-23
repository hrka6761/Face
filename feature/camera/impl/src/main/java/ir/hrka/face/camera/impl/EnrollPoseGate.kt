package ir.hrka.face.camera.impl

import ir.hrka.face.recognition.api.FaceRecognitionConfig
import kotlin.math.abs

/**
 * Yaw-based pose gating for guided front / profile enrollment.
 *
 * ML Kit: positive [yawDegrees] means the face looks toward the right of the camera image,
 * which corresponds to the subject turning their head to their **left**.
 */
object EnrollPoseGate {

    /**
     * Returns whether [yawDegrees] satisfies [step].
     *
     * @param step Active enrollment pose step.
     * @param yawDegrees Head yaw from the detector (analysis-image space).
     * @param isFrontCamera Unused for yaw sign — kept for API compatibility with callers.
     */
    fun matches(
        step: EnrollPoseStep,
        yawDegrees: Float,
        isFrontCamera: Boolean,
    ): Boolean {
        // Do not negate for front camera: preview mirroring is visual-only; ML Kit yaw
        // already matches the subject's physical turn direction in analysis space.
        @Suppress("UNUSED_PARAMETER")
        val ignored = isFrontCamera
        val yaw = yawDegrees
        return when (step) {
            EnrollPoseStep.Front ->
                abs(yaw) <= FaceRecognitionConfig.POSE_FRONT_YAW_MAX

            // Subject turns LEFT → face looks to camera-right → positive yaw.
            EnrollPoseStep.LeftProfile ->
                yaw >= FaceRecognitionConfig.POSE_PROFILE_YAW_MIN &&
                    yaw <= FaceRecognitionConfig.POSE_PROFILE_YAW_MAX

            // Subject turns RIGHT → face looks to camera-left → negative yaw.
            EnrollPoseStep.RightProfile ->
                yaw <= -FaceRecognitionConfig.POSE_PROFILE_YAW_MIN &&
                    yaw >= -FaceRecognitionConfig.POSE_PROFILE_YAW_MAX
        }
    }

    fun hint(
        step: EnrollPoseStep,
        yawDegrees: Float?,
        isFrontCamera: Boolean,
    ): String {
        if (yawDegrees == null) {
            return "Hold your face in the frame so the camera can read your head angle."
        }
        if (matches(step, yawDegrees, isFrontCamera)) {
            return "Perfect — hold still while samples are captured…"
        }
        val yaw = yawDegrees
        return when (step) {
            EnrollPoseStep.Front -> when {
                yaw > FaceRecognitionConfig.POSE_FRONT_YAW_MAX ->
                    "Turn a little toward the camera (too far left)."
                yaw < -FaceRecognitionConfig.POSE_FRONT_YAW_MAX ->
                    "Turn a little toward the camera (too far right)."
                else -> step.instruction
            }

            EnrollPoseStep.LeftProfile -> when {
                yaw < FaceRecognitionConfig.POSE_PROFILE_YAW_MIN ->
                    "Keep turning LEFT — show more of the left side of your face."
                yaw > FaceRecognitionConfig.POSE_PROFILE_YAW_MAX ->
                    "Turn back a little — too far past the left profile."
                else -> step.instruction
            }

            EnrollPoseStep.RightProfile -> when {
                yaw > -FaceRecognitionConfig.POSE_PROFILE_YAW_MIN ->
                    "Keep turning RIGHT — show more of the right side of your face."
                yaw < -FaceRecognitionConfig.POSE_PROFILE_YAW_MAX ->
                    "Turn back a little — too far past the right profile."
                else -> step.instruction
            }
        }
    }

    fun spokenHint(
        step: EnrollPoseStep,
        yawDegrees: Float?,
        isFrontCamera: Boolean,
    ): String? {
        if (yawDegrees == null) return "Keep your face in the oval."
        if (matches(step, yawDegrees, isFrontCamera)) return "Perfect. Hold still."
        val yaw = yawDegrees
        return when (step) {
            EnrollPoseStep.Front -> when {
                abs(yaw) > FaceRecognitionConfig.POSE_FRONT_YAW_MAX -> "Look straight ahead."
                else -> null
            }
            EnrollPoseStep.LeftProfile -> when {
                yaw < FaceRecognitionConfig.POSE_PROFILE_YAW_MIN -> "Turn more to the left."
                yaw > FaceRecognitionConfig.POSE_PROFILE_YAW_MAX -> "Turn back a little."
                else -> null
            }
            EnrollPoseStep.RightProfile -> when {
                yaw > -FaceRecognitionConfig.POSE_PROFILE_YAW_MIN -> "Turn more to the right."
                yaw < -FaceRecognitionConfig.POSE_PROFILE_YAW_MAX -> "Turn back a little."
                else -> null
            }
        }
    }

    fun progress(
        step: EnrollPoseStep,
        yawDegrees: Float?,
        isFrontCamera: Boolean,
    ): Float {
        if (yawDegrees == null) return 0f
        if (matches(step, yawDegrees, isFrontCamera)) return 1f
        val yaw = yawDegrees
        return when (step) {
            EnrollPoseStep.Front -> {
                val max = FaceRecognitionConfig.POSE_FRONT_YAW_MAX
                (1f - (abs(yaw) / (max * 2.5f))).coerceIn(0f, 0.95f)
            }
            EnrollPoseStep.LeftProfile -> {
                val target = (FaceRecognitionConfig.POSE_PROFILE_YAW_MIN +
                    FaceRecognitionConfig.POSE_PROFILE_YAW_MAX) / 2f
                (1f - abs(yaw - target) / target).coerceIn(0f, 0.95f)
            }
            EnrollPoseStep.RightProfile -> {
                val target = -(FaceRecognitionConfig.POSE_PROFILE_YAW_MIN +
                    FaceRecognitionConfig.POSE_PROFILE_YAW_MAX) / 2f
                (1f - abs(yaw - target) / abs(target)).coerceIn(0f, 0.95f)
            }
        }
    }
}
