package ir.hrka.face.camera.impl

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hrka.face.detection.api.FaceDetectionResult
import ir.hrka.face.detection.api.FaceDetectorEngine
import ir.hrka.face.detection.api.FaceDetectorFactory
import ir.hrka.face.detection.api.FaceDetectorOptions
import ir.hrka.face.domain.EnrollPersonUseCase
import ir.hrka.face.domain.IdentifyFacesUseCase
import ir.hrka.face.engine.FaceRecognitionEngine
import ir.hrka.face.model.DetectedFace
import ir.hrka.face.model.FaceEmbedding
import ir.hrka.face.model.FaceLandmarkType
import ir.hrka.face.model.FaceMatchResult
import ir.hrka.face.model.Person
import android.graphics.PointF
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Orchestrates detection, embedding, identification, and multi-template enrollment.
 *
 * Pipeline:
 * 1) Detect on YUV [ImageProxy] via ML Kit (`:core:face_detection`) while proxy is open
 * 2) Publish bounding boxes immediately (overlay must not depend on embedding)
 * 3) Convert an upright bitmap and embed / match / enroll via [FaceRecognitionEngine] (ArcFace)
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val identifyFaces: IdentifyFacesUseCase,
    private val enrollPerson: EnrollPersonUseCase,
) : ViewModel() {

    private val appContext = context.applicationContext
    private val detector: FaceDetectorEngine = FaceDetectorFactory.create(
        FaceDetectorOptions(
            performanceMode = FaceDetectorOptions.PerformanceMode.FAST,
            landmarkMode = FaceDetectorOptions.LandmarkMode.ALL,
            minFaceSize = 0.05f,
            enableTracking = true,
        ),
    )
    private val engineMutex = Mutex()
    private var recognitionEngine: FaceRecognitionEngine? = null
    private val frameMutex = Mutex()
    private val isProcessing = AtomicBoolean(false)

    private val identityVotes = mutableMapOf<Int, MutableList<Person?>>()
    private val enrollSamples = mutableListOf<FaceEmbedding>()
    private val testProbes = mutableListOf<FaceEmbedding>()
    private var enrollTrackingId: Int? = null
    private var enrollStartedAtMs: Long = 0L
    private var enrollStep: EnrollPoseStep = EnrollPoseStep.FIRST
    private var enrollStepCount: Int = 0
    private var lastEnrollSampleAtMs: Long = 0L
    private var testStep: EnrollTestStep = EnrollTestStep.FIRST
    private var testStepCount: Int = 0
    private var lastTestSampleAtMs: Long = 0L
    private val voiceGuide = EnrollVoiceGuide(appContext)

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            prepareEngine()
        }
    }

    /**
     * Processes a camera frame. Always closes [imageProxy].
     * Frames are ignored until [CameraUiState.engineReady] is true.
     */
    fun processFrame(imageProxy: ImageProxy) {
        if (!_uiState.value.engineReady) {
            imageProxy.close()
            return
        }
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                frameMutex.withLock {
                    analyzeFrame(imageProxy)
                }
            } catch (_: CancellationException) {
                // Expected on teardown / rebind.
            } catch (t: Throwable) {
                Log.e(TAG, "Frame analysis failed", t)
            } finally {
                imageProxy.close()
                isProcessing.set(false)
            }
        }
    }

    private suspend fun analyzeFrame(imageProxy: ImageProxy) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        // Snapshot bitmap BEFORE ML Kit so embedding still has valid pixel data afterward.
        val uprightBitmap = ImageConversion.imageProxyToUprightBitmap(
            imageProxy = imageProxy,
            rotationDegrees = rotationDegrees,
        )

        // Detect with ML Kit while ImageProxy is still open.
        val detection = detector.detect(imageProxy)
        when (detection) {
            is FaceDetectionResult.Failure -> {
                if (detection.isCancellationMessage()) return
                Log.w(TAG, "Detection failed: ${detection.message}", detection.cause)
                _uiState.update { it.copy(faces = emptyList(), errorMessage = null) }
                return
            }

            is FaceDetectionResult.Success -> {
                if (detection.faces.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            faces = emptyList(),
                            imageWidth = detection.imageWidth,
                            imageHeight = detection.imageHeight,
                            errorMessage = null,
                        )
                    }
                    return
                }

                Log.d(
                    TAG,
                    "Detected ${detection.faces.size} face(s) " +
                        "${detection.imageWidth}x${detection.imageHeight} rot=$rotationDegrees",
                )

                // Publish boxes immediately — must not depend on embedding / matching.
                publishTrackedFaces(
                    faces = detection.faces,
                    imageWidth = detection.imageWidth,
                    imageHeight = detection.imageHeight,
                    embeddings = emptyMap(),
                    matches = emptyMap(),
                )

                if (uprightBitmap == null) {
                    Log.w(
                        TAG,
                        "Bitmap conversion failed " +
                            "(format=${imageProxy.format}, " +
                            "${imageProxy.width}x${imageProxy.height}, rot=$rotationDegrees). " +
                            "Boxes still shown.",
                    )
                    return
                }

                val engine = runCatching { ensureEngine() }.getOrElse { error ->
                    Log.e(TAG, "Recognition engine unavailable", error)
                    return
                }

                try {
                    recognizeAndPublish(
                        engine = engine,
                        bitmap = uprightBitmap,
                        faces = detection.faces,
                        imageWidth = detection.imageWidth,
                        imageHeight = detection.imageHeight,
                    )
                } finally {
                    if (!uprightBitmap.isRecycled) {
                        uprightBitmap.recycle()
                    }
                }
            }
        }
    }

    private suspend fun recognizeAndPublish(
        engine: FaceRecognitionEngine,
        bitmap: Bitmap,
        faces: List<DetectedFace>,
        imageWidth: Int,
        imageHeight: Int,
    ) {
        val embeddings = runCatching {
            embedAll(engine, bitmap, faces)
        }.getOrElse { error ->
            Log.e(TAG, "Embedding failed", error)
            emptyMap()
        }

        val matches = if (embeddings.isEmpty()) {
            emptyMap()
        } else {
            runCatching { identifyFaces(embeddings) }
                .getOrElse { error ->
                    Log.e(TAG, "Identify failed", error)
                    emptyMap()
                }
        }

        runCatching {
            maybeCollectEnrollmentSamples(
                engine = engine,
                bitmap = bitmap,
                faces = faces,
                embeddings = embeddings,
            )
        }.onFailure { error ->
            Log.e(TAG, "Enrollment sample collection failed", error)
        }

        runCatching {
            maybeCollectTestSamples(
                engine = engine,
                bitmap = bitmap,
                faces = faces,
                embeddings = embeddings,
            )
        }.onFailure { error ->
            Log.e(TAG, "Test sample collection failed", error)
        }

        publishTrackedFaces(
            faces = faces,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            embeddings = embeddings,
            matches = matches,
        )
    }

    private fun publishTrackedFaces(
        faces: List<DetectedFace>,
        imageWidth: Int,
        imageHeight: Int,
        embeddings: Map<Int, FaceEmbedding>,
        matches: Map<Int, FaceMatchResult>,
    ) {
        // Box-only publishes (empty matches) happen before embedding finishes.
        // Preserve prior identity so register / labels do not flash every frame.
        val boxOnlyUpdate = matches.isEmpty() && embeddings.isEmpty()
        val previousById = _uiState.value.faces.associateBy { it.trackingId }

        val tracked = faces.map { face ->
            val previous = previousById[face.trackingId]
            val raw = matches[face.trackingId]
            val smoothed = if (boxOnlyUpdate) {
                previous?.person
            } else {
                smoothIdentity(face.trackingId, raw?.person)
            }
            TrackedFaceUi(
                trackingId = face.trackingId,
                boundingBox = face.boundingBox,
                person = smoothed,
                embedding = embeddings[face.trackingId] ?: previous?.embedding,
                similarity = if (boxOnlyUpdate) {
                    previous?.similarity ?: 0f
                } else {
                    raw?.similarity ?: 0f
                },
                headEulerAngleY = face.headEulerAngleY ?: previous?.headEulerAngleY,
                leftEye = face.landmarks[FaceLandmarkType.LEFT_EYE]
                    ?.let { PointF(it.x, it.y) }
                    ?: previous?.leftEye,
                rightEye = face.landmarks[FaceLandmarkType.RIGHT_EYE]
                    ?.let { PointF(it.x, it.y) }
                    ?: previous?.rightEye,
            )
        }

        _uiState.update {
            it.copy(
                faces = tracked,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                errorMessage = null,
                enrollProgress = synchronized(enrollSamples) { enrollSamples.size },
            )
        }
    }

    private suspend fun maybeCollectEnrollmentSamples(
        engine: FaceRecognitionEngine,
        bitmap: Bitmap,
        faces: List<DetectedFace>,
        embeddings: Map<Int, FaceEmbedding>,
    ) {
        val trackingId = enrollTrackingId ?: return
        if (_uiState.value.enrollPhase != EnrollPhase.Scanning) return
        if (!_uiState.value.isEnrolling) return
        if (bitmap.isRecycled) return

        val face = faces.firstOrNull { it.trackingId == trackingId }
            ?: faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            ?: return
        if (face.trackingId != trackingId) {
            enrollTrackingId = face.trackingId
        }

        val yaw = face.headEulerAngleY
        val isFront = _uiState.value.isFrontCamera
        val step = enrollStep
        val poseOk = yaw != null && EnrollPoseGate.matches(step, yaw, isFront)
        val hint = EnrollPoseGate.hint(step, yaw, isFront)
        val progress = EnrollPoseGate.progress(step, yaw, isFront)

        voiceGuide.announceStep(step)
        val spoken = EnrollPoseGate.spokenHint(step, yaw, isFront)
        if (spoken != null) {
            voiceGuide.speakHint(spoken, force = poseOk)
        }

        _uiState.update {
            it.copy(
                enrollStep = step,
                enrollStepProgress = enrollStepCount,
                enrollStepTarget = EnrollConfig.ENROLL_SAMPLES_PER_POSE,
                enrollHint = hint,
                enrollPoseAligned = poseOk,
                enrollYawProgress = progress,
                enrollProgress = synchronized(enrollSamples) { enrollSamples.size },
                enrollTargetCount = EnrollConfig.ENROLL_TARGET_TEMPLATES,
            )
        }

        if (!poseOk) return
        if (!_uiState.value.enrollGuideAligned) {
            _uiState.update {
                it.copy(
                    enrollHint = when (step) {
                        EnrollPoseStep.Front ->
                            "Fit your face in the oval and place both eyes on the circles."
                        else ->
                            "Turn to the correct profile and fit your face inside the oval."
                    },
                )
            }
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastEnrollSampleAtMs < SAMPLE_INTERVAL_MS) return

        val robust = withContext(Dispatchers.Default) {
            runCatching {
                embedOne(engine, bitmap, face)
            }.getOrNull()
        } ?: embeddings[face.trackingId] ?: return

        lastEnrollSampleAtMs = now
        val stepDone: Boolean
        val allDone: Boolean
        synchronized(enrollSamples) {
            enrollSamples += robust
            enrollStepCount += 1
            stepDone = enrollStepCount >= EnrollConfig.ENROLL_SAMPLES_PER_POSE
            if (stepDone) {
                val next = step.next()
                if (next != null) {
                    enrollStep = next
                    enrollStepCount = 0
                    allDone = false
                } else {
                    allDone = true
                }
            } else {
                allDone = false
            }
        }

        if (stepDone && !allDone) {
            voiceGuide.announceStep(enrollStep)
            _uiState.update { it.copy(enrollGuideAligned = false) }
        }

        _uiState.update {
            it.copy(
                enrollStep = enrollStep,
                enrollStepProgress = enrollStepCount,
                enrollStepTarget = EnrollConfig.ENROLL_SAMPLES_PER_POSE,
                enrollHint = if (allDone) {
                    "Scan complete — checking quality…"
                } else if (stepDone) {
                    enrollStep.instruction
                } else {
                    hint
                },
                enrollPoseAligned = poseOk,
                enrollYawProgress = if (allDone) 1f else progress,
                enrollProgress = synchronized(enrollSamples) { enrollSamples.size },
                enrollTargetCount = EnrollConfig.ENROLL_TARGET_TEMPLATES,
            )
        }

        if (allDone) {
            completeScanAndReviewQuality()
        }
    }

    private fun completeScanAndReviewQuality() {
        val samples = synchronized(enrollSamples) { enrollSamples.toList() }
        val minRequired = EnrollConfig.ENROLL_TARGET_TEMPLATES
        if (samples.size < minRequired) {
            _uiState.update {
                it.copy(
                    isEnrolling = false,
                    enrollPhase = EnrollPhase.Idle,
                    enrollTarget = null,
                    enrollProgress = 0,
                    enrollStep = null,
                    enrollHint = "",
                    enrollPoseAligned = false,
                    enrollYawProgress = 0f,
                    errorMessage = "Registration incomplete — capture full face and both profiles",
                )
            }
            clearEnrollmentSession(keepSamples = false)
            return
        }

        voiceGuide.speak(
            "Enrollment scan complete. Press Test Scan to check match quality at different distances.",
            flush = true,
        )
        _uiState.update {
            it.copy(
                isEnrolling = false,
                enrollPhase = EnrollPhase.ReadyToTest,
                enrollHint = "Press Test Scan to verify match at different distances and positions.",
                enrollPoseAligned = false,
                enrollQualityGrade = null,
                enrollQualityScore = 0f,
                enrollTestStep = null,
                enrollTestProgress = 0,
                enrollTestTarget = EnrollTestStep.TARGET_SAMPLES,
            )
        }
    }

    private suspend fun maybeCollectTestSamples(
        engine: FaceRecognitionEngine,
        bitmap: Bitmap,
        faces: List<DetectedFace>,
        embeddings: Map<Int, FaceEmbedding>,
    ) {
        if (_uiState.value.enrollPhase != EnrollPhase.Testing) return
        if (bitmap.isRecycled) return

        val trackingId = enrollTrackingId
        val face = faces.firstOrNull { it.trackingId == trackingId }
            ?: faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            ?: return
        if (trackingId == null || face.trackingId != trackingId) {
            enrollTrackingId = face.trackingId
        }

        val yaw = face.headEulerAngleY
        val isFront = _uiState.value.isFrontCamera
        val poseStep = testStep.matchingPoseStep() ?: EnrollPoseStep.Front
        val poseOk = yaw != null && EnrollPoseGate.matches(poseStep, yaw, isFront)
        val guideOk = _uiState.value.enrollGuideAligned
        val hint = when {
            !poseOk -> EnrollPoseGate.hint(poseStep, yaw, isFront)
            !guideOk && testStep.requiresEyes ->
                "Fit your face in the oval and place both eyes on the circles."
            !guideOk ->
                "Fit your face inside the oval for this profile."
            else -> "Aligned — capturing…"
        }

        _uiState.update {
            it.copy(
                enrollTestStep = testStep,
                enrollHint = hint,
                enrollPoseAligned = poseOk,
                enrollTestProgress = synchronized(testProbes) { testProbes.size },
                enrollTestTarget = EnrollTestStep.TARGET_SAMPLES,
            )
        }

        if (!poseOk || !guideOk) return

        val now = System.currentTimeMillis()
        if (now - lastTestSampleAtMs < SAMPLE_INTERVAL_MS) return

        val robust = withContext(Dispatchers.Default) {
            runCatching {
                embedOne(engine, bitmap, face)
            }.getOrNull()
        } ?: embeddings[face.trackingId] ?: return

        lastTestSampleAtMs = now
        val stepDone: Boolean
        val allDone: Boolean
        synchronized(testProbes) {
            testProbes += robust
            testStepCount += 1
            stepDone = testStepCount >= EnrollTestStep.SAMPLES_PER_STEP
            if (stepDone) {
                val next = testStep.next()
                if (next != null) {
                    testStep = next
                    testStepCount = 0
                    allDone = false
                } else {
                    allDone = true
                }
            } else {
                allDone = false
            }
        }

        if (stepDone && !allDone) {
            voiceGuide.speak(testStep.spokenInstruction, flush = true)
            // Require a fresh alignment for the next oval size / pose.
            _uiState.update { it.copy(enrollGuideAligned = false) }
        }

        _uiState.update {
            it.copy(
                enrollTestStep = testStep,
                enrollHint = if (allDone) {
                    "Test complete — computing average match…"
                } else {
                    testStep.instruction
                },
                enrollTestProgress = synchronized(testProbes) { testProbes.size },
                enrollTestTarget = EnrollTestStep.TARGET_SAMPLES,
            )
        }

        if (allDone) {
            finishTestScan()
        }
    }

    private fun finishTestScan() {
        val gallery = synchronized(enrollSamples) { enrollSamples.toList() }
        val probes = synchronized(testProbes) { testProbes.toList() }
        val (score, grade) = EnrollQualityScorer.evaluateProbes(gallery, probes)
        val percent = (score * 100f).toInt().coerceIn(0, 100)
        voiceGuide.announceQuality(grade, percent)

        _uiState.update {
            it.copy(
                enrollPhase = EnrollPhase.QualityReview,
                isEnrolling = false,
                enrollQualityGrade = grade,
                enrollQualityScore = score,
                enrollHint = "Average match: ${grade.label} ($percent%)",
                enrollTestStep = null,
                enrollPoseAligned = false,
            )
        }
    }

    private suspend fun persistEnrollment(name: String) {
        val samples = synchronized(enrollSamples) { enrollSamples.toList() }
        if (samples.size < EnrollConfig.ENROLL_TARGET_TEMPLATES) {
            _uiState.update {
                it.copy(errorMessage = "No scan data to save — please scan again")
            }
            return
        }

        try {
            enrollPerson(name, samples)
            voiceGuide.speak("Registration complete.")
            _uiState.update {
                it.copy(
                    enrollTarget = null,
                    enrollPhase = EnrollPhase.Idle,
                    isEnrolling = false,
                    enrollProgress = 0,
                    enrollStep = null,
                    enrollHint = "",
                    enrollQualityGrade = null,
                    enrollQualityScore = 0f,
                    enrollPoseAligned = false,
                    enrollYawProgress = 0f,
                    enrollEyesAligned = false,
                    enrollTestStep = null,
                    enrollTestProgress = 0,
                    errorMessage = null,
                )
            }
            identityVotes.clear()
            clearEnrollmentSession(keepSamples = false)
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to save identity", t)
            _uiState.update {
                it.copy(errorMessage = t.message ?: "Failed to save identity")
            }
        }
    }

    private fun clearEnrollmentSession(keepSamples: Boolean = false) {
        enrollTrackingId = null
        enrollStartedAtMs = 0L
        enrollStep = EnrollPoseStep.FIRST
        enrollStepCount = 0
        lastEnrollSampleAtMs = 0L
        testStep = EnrollTestStep.FIRST
        testStepCount = 0
        lastTestSampleAtMs = 0L
        voiceGuide.resetStepMemory()
        voiceGuide.stop()
        synchronized(testProbes) { testProbes.clear() }
        if (!keepSamples) {
            synchronized(enrollSamples) { enrollSamples.clear() }
        }
    }

    private fun beginScan(trackingId: Int) {
        enrollTrackingId = trackingId
        enrollStartedAtMs = System.currentTimeMillis()
        enrollStep = EnrollPoseStep.FIRST
        enrollStepCount = 0
        lastEnrollSampleAtMs = 0L
        synchronized(testProbes) { testProbes.clear() }
        // Keep existing enrollSamples only when retesting; full rescan clears them.
        voiceGuide.resetStepMemory()
        voiceGuide.announceStep(EnrollPoseStep.FIRST)
        _uiState.update {
            it.copy(
                enrollPhase = EnrollPhase.Scanning,
                isEnrolling = true,
                enrollProgress = synchronized(enrollSamples) { enrollSamples.size },
                enrollTargetCount = EnrollConfig.ENROLL_TARGET_TEMPLATES,
                enrollStep = EnrollPoseStep.FIRST,
                enrollStepProgress = 0,
                enrollStepTarget = EnrollConfig.ENROLL_SAMPLES_PER_POSE,
                enrollHint = EnrollPoseStep.FIRST.instruction,
                enrollPoseAligned = false,
                enrollYawProgress = 0f,
                enrollEyesAligned = false,
                enrollQualityGrade = null,
                enrollQualityScore = 0f,
                enrollTestStep = null,
                enrollTestProgress = 0,
            )
        }
    }

    /**
     * Temporal smoothing of identity labels.
     *
     * Uses [MutableList] (not [ArrayDeque]) because unknown faces pass `null` votes and
     * Java's [ArrayDeque] throws [NullPointerException] on null elements — which previously
     * crashed every successful detection before the UI could update.
     */
    private fun smoothIdentity(trackingId: Int, candidate: Person?): Person? {
        val votes = identityVotes.getOrPut(trackingId) { mutableListOf() }
        votes += candidate
        while (votes.size > VOTE_WINDOW) {
            votes.removeAt(0)
        }

        val counts = votes.filterNotNull().groupingBy { it.id }.eachCount()
        val best = counts.maxByOrNull { it.value }
        return if (best != null && best.value >= VOTE_MIN) {
            votes.firstOrNull { it?.id == best.key } ?: candidate
        } else {
            // Do not sticky-hold identities below the vote threshold — avoids false IDs.
            null
        }
    }

    private suspend fun prepareEngine() {
        _uiState.update {
            it.copy(
                isPreparingEngine = true,
                engineReady = false,
                modelDownloadProgress = -1f,
                modelDownloadLabel = "Checking face models…",
                modelDownloadFileIndex = 0,
                modelDownloadTotalFiles = 0,
            )
        }
        runCatching { ensureEngine() }
            .onSuccess {
                _uiState.update { state ->
                    state.copy(
                        isPreparingEngine = false,
                        engineReady = true,
                        modelDownloadProgress = 1f,
                        modelDownloadLabel = "Face engine ready",
                        errorMessage = null,
                    )
                }
            }
            .onFailure { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "Failed to prepare face engine", error)
                _uiState.update {
                    it.copy(
                        isPreparingEngine = false,
                        engineReady = false,
                        modelDownloadProgress = 0f,
                        modelDownloadLabel = "Face engine unavailable",
                        errorMessage = error.message
                            ?: "Failed to download or load face models",
                    )
                }
            }
    }

    private suspend fun ensureEngine(): FaceRecognitionEngine {
        recognitionEngine?.let { return it }
        return engineMutex.withLock {
            recognitionEngine?.let { return it }
            withContext(Dispatchers.IO) {
                val paths = FaceModelStore.ensureModelPaths(appContext) { progress ->
                    _uiState.update { state ->
                        state.copy(
                            isPreparingEngine = true,
                            engineReady = false,
                            modelDownloadProgress = progress.overallProgress,
                            modelDownloadLabel = progress.label,
                            modelDownloadFileIndex = progress.currentFileIndex,
                            modelDownloadTotalFiles = progress.totalFiles,
                        )
                    }
                }
                _uiState.update { state ->
                    state.copy(
                        modelDownloadProgress = -1f,
                        modelDownloadLabel = "Starting face engine…",
                    )
                }
                FaceRecognitionEngine.create(appContext, paths).also {
                    recognitionEngine = it
                }
            }
        }
    }

    /** Retries model download / engine creation after a failure. */
    fun retryPrepareEngine() {
        if (_uiState.value.isPreparingEngine || _uiState.value.engineReady) return
        viewModelScope.launch(Dispatchers.IO) {
            prepareEngine()
        }
    }

    private suspend fun embedOne(
        engine: FaceRecognitionEngine,
        bitmap: Bitmap,
        face: DetectedFace,
    ): FaceEmbedding? {
        val engineFace = EngineFaceMapper.toEngineFace(face) ?: return null
        return runCatching {
            EngineFaceMapper.toDomainEmbedding(engine.embed(bitmap, engineFace))
        }.getOrNull()
    }

    private suspend fun embedAll(
        engine: FaceRecognitionEngine,
        bitmap: Bitmap,
        faces: List<DetectedFace>,
    ): Map<Int, FaceEmbedding> {
        val out = LinkedHashMap<Int, FaceEmbedding>(faces.size)
        for (face in faces) {
            val embedding = embedOne(engine, bitmap, face) ?: continue
            out[face.trackingId] = embedding
        }
        return out
    }

    /**
     * Switches between recognition and register modes.
     * Clears any in-progress enrollment when the mode changes.
     */
    fun setMode(mode: CameraMode) {
        if (_uiState.value.mode == mode) return
        clearEnrollmentSession(keepSamples = false)
        identityVotes.clear()
        _uiState.update {
            it.copy(
                mode = mode,
                enrollTarget = null,
                enrollPhase = EnrollPhase.Idle,
                isEnrolling = false,
                enrollProgress = 0,
                enrollStep = null,
                enrollStepProgress = 0,
                enrollHint = "",
                enrollPoseAligned = false,
                enrollYawProgress = 0f,
                enrollQualityGrade = null,
                enrollQualityScore = 0f,
                enrollEyesAligned = false,
                enrollTestStep = null,
                enrollTestProgress = 0,
            )
        }
    }

    /**
     * Opens eye-alignment guidance for an unknown face.
     * Scanning does **not** start until the user presses Start.
     * Only valid in [CameraMode.Register].
     */
    fun requestEnroll(face: TrackedFaceUi) {
        if (_uiState.value.mode != CameraMode.Register) return
        if (face.person != null) return
        enrollTrackingId = face.trackingId
        synchronized(enrollSamples) { enrollSamples.clear() }
        synchronized(testProbes) { testProbes.clear() }
        voiceGuide.speak(
            "Place your eyes exactly on the two circles, then press Start.",
            flush = true,
        )
        _uiState.update {
            it.copy(
                enrollTarget = face,
                enrollPhase = EnrollPhase.AlignEyes,
                isEnrolling = false,
                enrollProgress = 0,
                enrollTargetCount = EnrollConfig.ENROLL_TARGET_TEMPLATES,
                enrollStep = null,
                enrollStepProgress = 0,
                enrollHint = "Place each eye inside its circle, then press Start.",
                enrollPoseAligned = false,
                enrollYawProgress = 0f,
                enrollEyesAligned = false,
                enrollQualityGrade = null,
                enrollQualityScore = 0f,
                enrollTestStep = null,
                enrollTestProgress = 0,
                enrollTestTarget = EnrollTestStep.TARGET_SAMPLES,
            )
        }
    }

    /**
     * Updates whether both eyes currently sit on the on-screen targets.
     */
    fun updateEyesAligned(aligned: Boolean) {
        if (_uiState.value.enrollPhase != EnrollPhase.AlignEyes) return
        if (_uiState.value.enrollEyesAligned == aligned) return
        _uiState.update {
            it.copy(
                enrollEyesAligned = aligned,
                enrollHint = if (aligned) {
                    "Eyes aligned — press Start to begin the full-face scan."
                } else {
                    "Place each eye inside its circle, then press Start."
                },
            )
        }
    }

    /**
     * Updates whether the face/eyes currently fit the active scan or test oval guide.
     */
    fun updateGuideAligned(aligned: Boolean) {
        val phase = _uiState.value.enrollPhase
        if (phase != EnrollPhase.Scanning && phase != EnrollPhase.Testing) return
        if (_uiState.value.enrollGuideAligned == aligned) return
        _uiState.update { it.copy(enrollGuideAligned = aligned) }
    }

    /**
     * Starts the guided front + profile enrollment scan after eye alignment.
     */
    fun startEnrollScan() {
        if (_uiState.value.enrollPhase != EnrollPhase.AlignEyes) return
        if (!_uiState.value.enrollEyesAligned) {
            voiceGuide.speak("Align your eyes on the circles first.", flush = true)
            return
        }
        val trackingId = enrollTrackingId
            ?: _uiState.value.enrollTarget?.trackingId
            ?: _uiState.value.faces.firstOrNull()?.trackingId
            ?: return
        synchronized(enrollSamples) { enrollSamples.clear() }
        beginScan(trackingId)
    }

    /**
     * Cancels registration and discards any scanned samples.
     */
    fun dismissEnroll() {
        clearEnrollmentSession(keepSamples = false)
        _uiState.update {
            it.copy(
                enrollTarget = null,
                enrollPhase = EnrollPhase.Idle,
                isEnrolling = false,
                enrollProgress = 0,
                enrollStep = null,
                enrollStepProgress = 0,
                enrollHint = "",
                enrollPoseAligned = false,
                enrollYawProgress = 0f,
                enrollEyesAligned = false,
                enrollQualityGrade = null,
                enrollQualityScore = 0f,
                enrollTestStep = null,
                enrollTestProgress = 0,
            )
        }
    }

    /**
     * Restarts from eye alignment after a Bad or Good quality result.
     */
    fun retryEnrollScan() {
        val face = _uiState.value.enrollTarget
            ?: _uiState.value.faces.firstOrNull()
            ?: return
        requestEnroll(face.copy(person = null))
    }

    /**
     * Starts the manual Test Scan (does not run automatically after enrollment).
     */
    fun startTestScan() {
        if (_uiState.value.enrollPhase != EnrollPhase.ReadyToTest) return
        if (synchronized(enrollSamples) { enrollSamples.size } < EnrollConfig.ENROLL_TARGET_TEMPLATES) {
            _uiState.update { it.copy(errorMessage = "Enrollment scan incomplete — scan again first") }
            return
        }
        testStep = EnrollTestStep.FIRST
        testStepCount = 0
        lastTestSampleAtMs = 0L
        synchronized(testProbes) { testProbes.clear() }
        voiceGuide.speak(EnrollTestStep.FIRST.spokenInstruction, flush = true)
        _uiState.update {
            it.copy(
                enrollPhase = EnrollPhase.Testing,
                isEnrolling = false,
                enrollTestStep = EnrollTestStep.FIRST,
                enrollTestProgress = 0,
                enrollTestTarget = EnrollTestStep.TARGET_SAMPLES,
                enrollHint = EnrollTestStep.FIRST.instruction,
                enrollPoseAligned = false,
                enrollQualityGrade = null,
                enrollQualityScore = 0f,
            )
        }
    }

    /**
     * Moves from an Excellent quality review to the person-details step.
     */
    fun proceedToEnterDetails() {
        if (_uiState.value.enrollQualityGrade != EnrollQualityGrade.Excellent) return
        _uiState.update { it.copy(enrollPhase = EnrollPhase.EnterDetails) }
    }

    /**
     * Saves the scanned templates with the provided person name.
     */
    fun saveEnroll(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (_uiState.value.enrollPhase != EnrollPhase.EnterDetails) return
        viewModelScope.launch(Dispatchers.IO) {
            persistEnrollment(trimmed)
        }
    }

    fun onCameraFlagsChanged(isFrontCamera: Boolean, isTorchOn: Boolean) {
        _uiState.update {
            it.copy(isFrontCamera = isFrontCamera, isTorchOn = isTorchOn)
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        detector.close()
        recognitionEngine?.close()
        recognitionEngine = null
        voiceGuide.release()
        clearEnrollmentSession(keepSamples = false)
        super.onCleared()
    }

    companion object {
        private const val TAG = "FaceCameraVM"
        private const val VOTE_WINDOW = 7
        private const val VOTE_MIN = 3
        private const val SAMPLE_INTERVAL_MS = 350L
    }
}

private fun FaceDetectionResult.Failure.isCancellationMessage(): Boolean {
    val text = message
    return text.contains("Task was cancelled", ignoreCase = true) ||
        text.contains("Job was cancelled", ignoreCase = true)
}

