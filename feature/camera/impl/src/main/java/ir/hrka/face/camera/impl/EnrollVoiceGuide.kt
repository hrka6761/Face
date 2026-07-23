package ir.hrka.face.camera.impl

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Spoken guidance for enrollment when the user cannot watch the screen (profile turns).
 */
class EnrollVoiceGuide(
    context: Context,
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private val ready = AtomicBoolean(false)
    private var lastHintAtMs: Long = 0L
    private var lastSpokenStep: EnrollPoseStep? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TextToSpeech init failed: $status")
            ready.set(false)
            return
        }
        val engine = tts ?: return
        val result = engine.setLanguage(Locale.US)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "TTS language unavailable")
            ready.set(false)
            return
        }
        engine.setSpeechRate(0.95f)
        ready.set(true)
    }

    /**
     * Announces a pose step when it becomes active.
     */
    fun announceStep(step: EnrollPoseStep) {
        if (lastSpokenStep == step) return
        lastSpokenStep = step
        speak(step.spokenInstruction, flush = true)
    }

    /**
     * Speaks a short corrective hint, rate-limited so it does not spam.
     */
    fun speakHint(text: String, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastHintAtMs < HINT_INTERVAL_MS) return
        lastHintAtMs = now
        speak(text, flush = false)
    }

    /**
     * Speaks the scan quality result.
     */
    fun announceQuality(grade: EnrollQualityGrade, percent: Int) {
        val message = when (grade) {
            EnrollQualityGrade.Bad ->
                "Scan quality bad, $percent percent. Please scan again."
            EnrollQualityGrade.Good ->
                "Scan quality good, $percent percent. Please scan again for better accuracy."
            EnrollQualityGrade.Excellent ->
                "Scan quality excellent, $percent percent. Enter your details to finish registration."
        }
        speak(message, flush = true)
    }

    fun speak(text: String, flush: Boolean = true) {
        if (!ready.get()) return
        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, mode, null, "face-enroll")
    }

    fun stop() {
        tts?.stop()
    }

    fun release() {
        ready.set(false)
        tts?.stop()
        tts?.shutdown()
        tts = null
        lastSpokenStep = null
    }

    fun resetStepMemory() {
        lastSpokenStep = null
        lastHintAtMs = 0L
    }

    companion object {
        private const val TAG = "EnrollVoiceGuide"
        private const val HINT_INTERVAL_MS = 3200L
    }
}
