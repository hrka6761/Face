package ir.hrka.face.camera.impl

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.TorchState
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Binds a [LifecycleCameraController] to [PreviewView] for preview + face analysis.
 *
 * Prefer binding to the host [androidx.activity.ComponentActivity] lifecycle (not a
 * transient Navigation entry lifecycle) so the camera stays active while the screen is visible.
 *
 * @param appContext Application context used to create the controller.
 * @property lifecycleOwner Lifecycle that owns the camera binding (typically the Activity).
 * @property viewModel Camera feature ViewModel that receives analysis frames.
 */
class CameraBinder(
    appContext: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: CameraViewModel,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val controller = LifecycleCameraController(appContext.applicationContext)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val started = AtomicBoolean(false)
    private var torchRequested = false

    init {
        controller.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        // Preview is always bound by CameraController; keep analysis enabled (capture unused).
        controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        controller.imageAnalysisBackpressureStrategy =
            ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        // Keep default YUV_420_888 — required by ML Kit InputImage.fromMediaImage().
        // Do NOT switch to RGBA_8888; that breaks the media-image detection path.
        controller.imageAnalysisOutputImageFormat =
            ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
    }

    /**
     * Whether the front camera is currently selected.
     */
    val isFrontCamera: Boolean
        get() = controller.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA

    /**
     * Whether torch is currently enabled.
     */
    val isTorchOn: Boolean
        get() {
            val live = controller.cameraInfo?.torchState?.value
            return live == TorchState.ON || (live == null && torchRequested)
        }

    /**
     * Attaches [previewView] to the controller and starts the camera if needed.
     *
     * Safe to call multiple times with the same view.
     *
     * @param previewView Host preview surface.
     */
    fun start(previewView: PreviewView) {
        previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        previewView.controller = controller

        if (started.compareAndSet(false, true)) {
            controller.setImageAnalysisAnalyzer(analysisExecutor) { imageProxy ->
                viewModel.processFrame(imageProxy)
            }
            controller.bindToLifecycle(lifecycleOwner)
        }
        publishFlags()
    }

    /**
     * Switches between front and back cameras.
     */
    fun switchCamera() {
        if (!isFrontCamera) {
            torchRequested = false
            runCatching { controller.enableTorch(false) }
        }
        controller.cameraSelector = if (isFrontCamera) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        publishFlags()
    }

    /**
     * Toggles torch when supported (typically back camera only).
     *
     * @return `false` when torch is unavailable.
     */
    fun toggleTorch(): Boolean {
        if (isFrontCamera) return false
        val hasFlash = controller.cameraInfo?.hasFlashUnit() == true
        if (!hasFlash) return false
        val enable = !isTorchOn
        return runCatching {
            controller.enableTorch(enable)
            torchRequested = enable
            publishFlags()
            true
        }.getOrDefault(false)
    }

    /**
     * Stops analysis and unbinds the camera. Call when the screen leaves composition.
     */
    fun release() {
        if (started.compareAndSet(true, false)) {
            runCatching { controller.clearImageAnalysisAnalyzer() }
            runCatching { controller.unbind() }
        }
        if (!analysisExecutor.isShutdown) {
            analysisExecutor.shutdown()
        }
    }

    private fun publishFlags() {
        mainHandler.post {
            viewModel.onCameraFlagsChanged(
                isFrontCamera = isFrontCamera,
                isTorchOn = isTorchOn,
            )
        }
    }
}
