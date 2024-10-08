package ir.hrka.face.presentation.ui.screens.home

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hrka.face.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.jvm.Throws

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val controller = LifecycleCameraController(context).apply {
        setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE)
    }
    private val _flashLightState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val flashLightState: StateFlow<Boolean> = _flashLightState


    fun initCameraPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        previewView.controller = controller
        controller.bindToLifecycle(lifecycleOwner)
        controller.enableTorch(_flashLightState.value)
    }

    fun switchCamera() {
        controller.cameraSelector =
            if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                _flashLightState.value = false
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else
                CameraSelector.DEFAULT_BACK_CAMERA
    }

    @Throws(IllegalStateException::class)
    fun toggleFlashLight() {
        if (controller.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
            if (controller.torchState.value == 0) {
                controller.enableTorch(true)
                _flashLightState.value = true
            } else {
                controller.enableTorch(false)
                _flashLightState.value = false
            }
        else
            throw IllegalStateException(context.getString(R.string.home_view_model_toggle_flash_light_in_front_camera_msg_error))
    }
}