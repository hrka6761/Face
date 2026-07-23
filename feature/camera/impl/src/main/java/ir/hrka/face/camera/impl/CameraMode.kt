package ir.hrka.face.camera.impl

/**
 * Guided enrollment pose steps for accurate front + profile registration.
 */
enum class EnrollPoseStep {
    /** Look straight at the camera. */
    Front,

    /** Turn the head so the left side of the face is visible. */
    LeftProfile,

    /** Turn the head so the right side of the face is visible. */
    RightProfile,
    ;

    /** Short instruction shown during guided enrollment. */
    val instruction: String
        get() = when (this) {
            Front -> "Look straight at the camera. Keep your face centered and still."
            LeftProfile -> "Slowly turn your head to the LEFT until the side of your face is clearly visible. Hold still."
            RightProfile -> "Slowly turn your head to the RIGHT until the other side of your face is clearly visible. Hold still."
        }

    /** Short phrase spoken by TTS (user may not see the screen). */
    val spokenInstruction: String
        get() = when (this) {
            Front -> "Look straight at the camera. Keep your face in the oval and hold still."
            LeftProfile -> "Now turn your head slowly to the left, and hold when I say perfect."
            RightProfile -> "Now turn your head slowly to the right, and hold when I say perfect."
        }

    /** Compact step title for progress UI. */
    val title: String
        get() = when (this) {
            Front -> "1/3 · Full face"
            LeftProfile -> "2/3 · Left profile"
            RightProfile -> "3/3 · Right profile"
        }

    /** Next step, or `null` when this is the last. */
    fun next(): EnrollPoseStep? = when (this) {
        Front -> LeftProfile
        LeftProfile -> RightProfile
        RightProfile -> null
    }

    companion object {
        /** First enrollment step. */
        val FIRST: EnrollPoseStep = Front
    }
}

/**
 * Camera feature operating modes.
 */
enum class CameraMode {
    /**
     * Live multi-face recognition: draws a box per face and labels known identities
     * inside each box. Enrollment is not available in this mode.
     */
    Recognition,

    /**
     * Single-face registration / check-in: a fixed center guide frame identifies one
     * person. Unknown faces can be registered with guided front + profile capture;
     * known faces show stored details.
     */
    Register,
}
