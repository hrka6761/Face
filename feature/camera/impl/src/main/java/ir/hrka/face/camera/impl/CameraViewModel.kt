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
import ir.hrka.face.model.DetectedFace
import ir.hrka.face.model.FaceEmbedding
import ir.hrka.face.model.FaceMatchResult
import ir.hrka.face.model.Person
import ir.hrka.face.recognition.api.FaceEmbedder
import ir.hrka.face.recognition.api.FaceRecognitionConfig
import ir.hrka.face.recognition.api.FaceRecognitionFactory
import ir.hrka.face.recognition.internal.ImageConversion
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
 * Pipeline (ML Kit–compatible camera path):
 * 1) Detect on YUV [ImageProxy] via `InputImage.fromMediaImage` (keep proxy open)
 * 2) Publish bounding boxes immediately (overlay must not depend on embedding)
 * 3) Convert a separate upright bitmap for TFLite embed / match / enroll
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
    private val embedderMutex = Mutex()
    private var embedder: FaceEmbedder? = null
    private val frameMutex = Mutex()
    private val isProcessing = AtomicBoolean(false)

    private val identityVotes = mutableMapOf<Int, MutableList<Person?>>()
    private val enrollSamples = mutableListOf<FaceEmbedding>()
    private var enrollTrackingId: Int? = null
    private var enrollName: String? = null
    private var enrollStartedAtMs: Long = 0L
    private var enrollStep: EnrollPoseStep = EnrollPoseStep.FIRST
    private var enrollStepCount: Int = 0
    private var lastEnrollSampleAtMs: Long = 0L

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { ensureEmbedder() }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to load face model")
                    }
                }
        }
    }

    /**
     * Processes a camera frame. Always closes [imageProxy].
     */
    fun processFrame(imageProxy: ImageProxy) {
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

        // Detect with ML Kit's camera API while ImageProxy is still open.
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

                try {
                    recognizeAndPublish(
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
        bitmap: Bitmap,
        faces: List<DetectedFace>,
        imageWidth: Int,
        imageHeight: Int,
    ) {
        val activeEmbedder = runCatching { ensureEmbedder() }.getOrElse { error ->
            Log.e(TAG, "Embedder unavailable", error)
            return
        }

        val embeddings = runCatching {
            activeEmbedder.embedAll(bitmap, faces)
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
                embedder = activeEmbedder,
                bitmap = bitmap,
                faces = faces,
                embeddings = embeddings,
            )
        }.onFailure { error ->
            Log.e(TAG, "Enrollment sample collection failed", error)
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
        embedder: FaceEmbedder,
        bitmap: Bitmap,
        faces: List<DetectedFace>,
        embeddings: Map<Int, FaceEmbedding>,
    ) {
        val trackingId = enrollTrackingId ?: return
        val name = enrollName ?: return
        if (!_uiState.value.isEnrolling) return
        if (bitmap.isRecycled) return

        val face = faces.firstOrNull { it.trackingId == trackingId } ?: return
        val yaw = face.headEulerAngleY
        val isFront = _uiState.value.isFrontCamera
        val step = enrollStep
        val poseOk = yaw != null && EnrollPoseGate.matches(step, yaw, isFront)
        val hint = EnrollPoseGate.hint(step, yaw, isFront)

        _uiState.update {
            it.copy(
                enrollStep = step,
                enrollStepProgress = enrollStepCount,
                enrollStepTarget = FaceRecognitionConfig.ENROLL_SAMPLES_PER_POSE,
                enrollHint = hint,
                enrollProgress = synchronized(enrollSamples) { enrollSamples.size },
                enrollTargetCount = FaceRecognitionConfig.ENROLL_TARGET_TEMPLATES,
            )
        }

        if (!poseOk) return

        val now = System.currentTimeMillis()
        // Space samples so each pose gathers diverse frames, not near-duplicates.
        if (now - lastEnrollSampleAtMs < SAMPLE_INTERVAL_MS) return

        val robust = withContext(Dispatchers.Default) {
            runCatching { embedder.embedRobust(bitmap, face) }.getOrNull()
        } ?: embeddings[trackingId] ?: return

        lastEnrollSampleAtMs = now
        val stepDone: Boolean
        val allDone: Boolean
        synchronized(enrollSamples) {
            enrollSamples += robust
            enrollStepCount += 1
            stepDone = enrollStepCount >= FaceRecognitionConfig.ENROLL_SAMPLES_PER_POSE
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

        _uiState.update {
            it.copy(
                enrollStep = enrollStep,
                enrollStepProgress = enrollStepCount,
                enrollStepTarget = FaceRecognitionConfig.ENROLL_SAMPLES_PER_POSE,
                enrollHint = if (allDone) {
                    "All poses captured — saving…"
                } else if (stepDone) {
                    enrollStep.instruction
                } else {
                    hint
                },
                enrollProgress = synchronized(enrollSamples) { enrollSamples.size },
                enrollTargetCount = FaceRecognitionConfig.ENROLL_TARGET_TEMPLATES,
            )
        }

        if (allDone) {
            finishEnrollment(name)
        }
    }

    private suspend fun finishEnrollment(name: String) {
        val samples = synchronized(enrollSamples) { enrollSamples.toList() }
        val minRequired = FaceRecognitionConfig.ENROLL_TARGET_TEMPLATES
        if (samples.size < minRequired) {
            _uiState.update {
                it.copy(
                    isEnrolling = false,
                    enrollTarget = null,
                    enrollProgress = 0,
                    enrollStep = null,
                    enrollHint = "",
                    errorMessage = "Registration incomplete — capture full face and both profiles",
                )
            }
            clearEnrollmentSession()
            return
        }

        try {
            enrollPerson(name, samples)
            _uiState.update {
                it.copy(
                    enrollTarget = null,
                    isEnrolling = false,
                    enrollProgress = 0,
                    enrollStep = null,
                    enrollHint = "",
                    errorMessage = null,
                )
            }
            identityVotes.clear()
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to save identity", t)
            _uiState.update {
                it.copy(
                    isEnrolling = false,
                    errorMessage = t.message ?: "Failed to save identity",
                )
            }
        } finally {
            clearEnrollmentSession()
        }
    }

    private fun clearEnrollmentSession() {
        enrollTrackingId = null
        enrollName = null
        enrollStartedAtMs = 0L
        enrollStep = EnrollPoseStep.FIRST
        enrollStepCount = 0
        lastEnrollSampleAtMs = 0L
        synchronized(enrollSamples) { enrollSamples.clear() }
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

    private suspend fun ensureEmbedder(): FaceEmbedder {
        embedder?.let { return it }
        return embedderMutex.withLock {
            embedder?.let { return it }
            withContext(Dispatchers.IO) {
                FaceRecognitionFactory.createEmbedder(appContext).also { embedder = it }
            }
        }
    }

    /**
     * Switches between recognition and register modes.
     * Clears any in-progress enrollment when the mode changes.
     */
    fun setMode(mode: CameraMode) {
        if (_uiState.value.mode == mode) return
        clearEnrollmentSession()
        identityVotes.clear()
        _uiState.update {
            it.copy(
                mode = mode,
                enrollTarget = null,
                isEnrolling = false,
                enrollProgress = 0,
                enrollStep = null,
                enrollStepProgress = 0,
                enrollHint = "",
            )
        }
    }

    /**
     * Opens the enrollment dialog for an unknown face.
     * Only valid in [CameraMode.Register].
     */
    fun requestEnroll(face: TrackedFaceUi) {
        if (_uiState.value.mode != CameraMode.Register) return
        if (face.person != null) return
        _uiState.update {
            it.copy(
                enrollTarget = face,
                enrollProgress = 0,
                enrollTargetCount = FaceRecognitionConfig.ENROLL_TARGET_TEMPLATES,
                enrollStep = EnrollPoseStep.FIRST,
                enrollStepProgress = 0,
                enrollStepTarget = FaceRecognitionConfig.ENROLL_SAMPLES_PER_POSE,
                enrollHint = EnrollPoseStep.FIRST.instruction,
            )
        }
    }

    fun dismissEnroll() {
        clearEnrollmentSession()
        _uiState.update {
            it.copy(
                enrollTarget = null,
                isEnrolling = false,
                enrollProgress = 0,
                enrollStep = null,
                enrollStepProgress = 0,
                enrollHint = "",
            )
        }
    }

    fun confirmEnroll(name: String) {
        val target = _uiState.value.enrollTarget ?: return
        enrollTrackingId = target.trackingId
        enrollName = name.trim()
        enrollStartedAtMs = System.currentTimeMillis()
        enrollStep = EnrollPoseStep.FIRST
        enrollStepCount = 0
        lastEnrollSampleAtMs = 0L
        synchronized(enrollSamples) { enrollSamples.clear() }
        _uiState.update {
            it.copy(
                isEnrolling = true,
                enrollProgress = 0,
                enrollTargetCount = FaceRecognitionConfig.ENROLL_TARGET_TEMPLATES,
                enrollStep = EnrollPoseStep.FIRST,
                enrollStepProgress = 0,
                enrollStepTarget = FaceRecognitionConfig.ENROLL_SAMPLES_PER_POSE,
                enrollHint = EnrollPoseStep.FIRST.instruction,
            )
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
        embedder?.close()
        embedder = null
        clearEnrollmentSession()
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
