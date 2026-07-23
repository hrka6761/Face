package ir.hrka.face.camera.impl

import ir.hrka.face.recognition.api.FaceRecognitionConfig
import kotlin.math.abs

/**
 * Yaw-based pose gating for guided front / profile enrollment.
 */
object EnrollPoseGate {

    /**
     * Returns whether [yawDegrees] satisfies [step].
     *
     * @param step Active enrollment pose step.
     * @param yawDegrees Head yaw from the detector (analysis-image space).
     * @param isFrontCamera When true, yaw is mirrored so on-screen turn directions match.
     */
    fun matches(
        step: EnrollPoseStep,
        yawDegrees: Float,
        isFrontCamera: Boolean,
    ): Boolean {
        val yaw = if (isFrontCamera) -yawDegrees else yawDegrees
        return when (step) {
            EnrollPoseStep.Front ->
                abs(yaw) <= FaceRecognitionConfig.POSE_FRONT_YAW_MAX

            EnrollPoseStep.LeftProfile ->
                yaw >= FaceRecognitionConfig.POSE_PROFILE_YAW_MIN &&
                    yaw <= FaceRecognitionConfig.POSE_PROFILE_YAW_MAX

            EnrollPoseStep.RightProfile ->
                yaw <= -FaceRecognitionConfig.POSE_PROFILE_YAW_MIN &&
                    yaw >= -FaceRecognitionConfig.POSE_PROFILE_YAW_MAX
        }
    }

    /**
     * Builds a live hint when the pose is not yet accepted.
     */
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
        val yaw = if (isFrontCamera) -yawDegrees else yawDegrees
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
}
