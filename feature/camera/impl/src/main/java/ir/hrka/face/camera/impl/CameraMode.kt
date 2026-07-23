package ir.hrka.face.camera.impl

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
     * Time-and-attendance style capture: a fixed center guide frame identifies a single
     * person. Unknown faces can be registered; known faces show stored details.
     */
    Attendance,
}
