package ir.hrka.face.camera.impl

/**
 * High-level phases of the guided registration flow.
 */
enum class EnrollPhase {
    Idle,
    AlignEyes,
    Scanning,
    ReadyToTest,
    Testing,
    QualityReview,
    EnterDetails,
}

/**
 * Self-check grade thresholds: Bad &lt; 75%, Good 75–90%, Excellent &gt; 90%.
 */
enum class EnrollQualityGrade {
    Bad,
    Good,
    Excellent,
    ;

    val label: String
        get() = when (this) {
            Bad -> "Bad"
            Good -> "Good"
            Excellent -> "Excellent"
        }
}

/**
 * Manual Test Scan steps: full-face at three distances, then both profiles.
 *
 * Oval size shrinks for the far step so the user must move away to fit.
 */
enum class EnrollTestStep {
    FrontClose,
    FrontMedium,
    FrontFar,
    LeftProfile,
    RightProfile,
    ;

    val title: String
        get() = when (this) {
            FrontClose -> "1/5 · Closer"
            FrontMedium -> "2/5 · Normal distance"
            FrontFar -> "3/5 · Farther"
            LeftProfile -> "4/5 · Left profile"
            RightProfile -> "5/5 · Right profile"
        }

    val instruction: String
        get() = when (this) {
            FrontClose ->
                "Move CLOSER until your face and both eyes fill the large oval, then hold."
            FrontMedium ->
                "Move to a NORMAL distance so your face and eyes fit the oval, then hold."
            FrontFar ->
                "Move FARTHER until your face and eyes fit inside the smaller oval, then hold."
            LeftProfile ->
                "Turn your head to the LEFT (profile) and fit your face inside the oval, then hold."
            RightProfile ->
                "Turn your head to the RIGHT (profile) and fit your face inside the oval, then hold."
        }

    val spokenInstruction: String
        get() = when (this) {
            FrontClose -> "Move closer until your face fills the oval, then hold still."
            FrontMedium -> "Move to a normal distance and fit your face in the oval."
            FrontFar -> "Move farther until your face fits the smaller oval."
            LeftProfile -> "Turn your head to the left and fit your face in the oval."
            RightProfile -> "Turn your head to the right and fit your face in the oval."
        }

    /**
     * Guide oval size as a fraction of the shorter screen side.
     * Far uses a smaller oval so the user must step back.
     */
    val ovalFraction: Float
        get() = when (this) {
            FrontClose -> 0.78f
            FrontMedium -> 0.58f
            FrontFar -> 0.36f
            LeftProfile, RightProfile -> 0.55f
        }

    val requiresFrontYaw: Boolean
        get() = this == FrontClose || this == FrontMedium || this == FrontFar

    val requiresEyes: Boolean
        get() = requiresFrontYaw

    fun matchingPoseStep(): EnrollPoseStep? = when (this) {
        FrontClose, FrontMedium, FrontFar -> EnrollPoseStep.Front
        LeftProfile -> EnrollPoseStep.LeftProfile
        RightProfile -> EnrollPoseStep.RightProfile
    }

    fun next(): EnrollTestStep? = when (this) {
        FrontClose -> FrontMedium
        FrontMedium -> FrontFar
        FrontFar -> LeftProfile
        LeftProfile -> RightProfile
        RightProfile -> null
    }

    companion object {
        val FIRST: EnrollTestStep = FrontClose
        const val SAMPLES_PER_STEP: Int = 2
        val STEP_COUNT: Int = entries.size
        val TARGET_SAMPLES: Int = SAMPLES_PER_STEP * STEP_COUNT
    }
}
