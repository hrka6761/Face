package ir.hrka.face.presentation.ui.screens.home

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import ir.hrka.face.R
import ir.hrka.face.core.utilities.Constants.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak", "WrongConstant")
class PreviewController(
    private val context: Context,
    private val viewModel: HomeViewModel
) {

    private lateinit var _previewView: PreviewView
    private lateinit var _lifecycleOwner: LifecycleOwner
    private var camera: Camera? = null
    private var cameraSelector: CameraSelector? = null


    fun bindPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        _previewView = previewView
        _lifecycleOwner = lifecycleOwner
        viewModel.preview.setSurfaceProvider(previewView.surfaceProvider)
        cameraSelector =
            CameraSelector.Builder().requireLensFacing(viewModel.lensFacing.value).build()
        viewModel.imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            viewModel.setSurfaceSize(imageProxy.height, imageProxy.width)
            viewModel.detectFaces(imageProxy)
        }
        camera =
            viewModel.cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector!!,
                viewModel.preview,
                viewModel.imageAnalysis
            )
        setFlashLightState()
    }

    fun unbindPreview() {
        if (camera != null) {
            viewModel.cameraProvider.unbind(viewModel.preview, viewModel.imageAnalysis)
            camera = null
            cameraSelector = null
        }
    }

    fun switchCamera() {
        if (viewModel.lensFacing.value == LENS_FACING_BACK) {
            turnOffFlashLight()
            viewModel.setLensFacing(LENS_FACING_FRONT)
        } else
            viewModel.setLensFacing(LENS_FACING_BACK)

        unbindPreview()
        bindPreview(_previewView, _lifecycleOwner)
    }

    @Throws(IllegalStateException::class)
    fun toggleFlashLight() {
        if (viewModel.lensFacing.value == LENS_FACING_BACK)
            if (viewModel.flashLightState.value)
                turnOffFlashLight()
            else
                turnOnFlashLight()
        else
            throw IllegalStateException(context.getString(R.string.home_view_model_toggle_flash_light_in_front_camera_msg_error))
    }


    private fun setFlashLightState() {
        CoroutineScope(Dispatchers.Main).launch {
            viewModel.flashLightState.collect { state ->
                camera?.cameraControl?.enableTorch(state)
            }
        }
    }

    private fun turnOnFlashLight() {
        camera?.cameraControl?.enableTorch(true)
        viewModel.setFlashlightState(true)
    }

    private fun turnOffFlashLight() {
        camera?.cameraControl?.enableTorch(false)
        viewModel.setFlashlightState(false)
    }
}

data class FaceOverlay(
    val faceLeft: Float,
    val faceTop: Float,
    val faceWith: Float,
    val faceHeight: Float,
    val balanceCircles: BalanceCircles
)

data class BalanceCircles(
    val horizontalBalanceCircleX: Float,
    val horizontalBalanceCircleY: Float,
    val verticalBalanceCircleX: Float,
    val verticalBalanceCircleY: Float
)

enum class FaceGravity {
    Left, Right, Top, Bottom
}