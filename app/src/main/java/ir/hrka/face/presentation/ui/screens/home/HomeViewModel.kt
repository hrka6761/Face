package ir.hrka.face.presentation.ui.screens.home

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hrka.face.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("StaticFieldLeak", "WrongConstant")
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private lateinit var _previewView: PreviewView
    private lateinit var _lifecycleOwner: LifecycleOwner
    private val _flashLightState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val flashLightState: StateFlow<Boolean> = _flashLightState
    private val cameraProvider = ProcessCameraProvider.getInstance(context).get()
    private var preview = Preview.Builder().build()
    private var camera: Camera? = null
    private var cameraSelector: CameraSelector? = null
    private var lensFacing = LENS_FACING_BACK


    fun bindPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        _previewView = previewView
        _lifecycleOwner = lifecycleOwner
        preview.setSurfaceProvider(previewView.surfaceProvider)
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector!!, preview)

        CoroutineScope(Dispatchers.Main).launch {
            _flashLightState.collect { state ->
                camera?.cameraControl?.enableTorch(state)
            }
        }
    }

    fun unbindPreview() {
        if (camera != null) {
            cameraProvider.unbind(preview)
            camera = null
            cameraSelector = null
        }
    }

    fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            camera?.cameraControl?.enableTorch(false)
            _flashLightState.value = false
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        unbindPreview()
        bindPreview(_previewView, _lifecycleOwner)
    }

    @Throws(IllegalStateException::class)
    fun toggleFlashLight() {
        if (lensFacing == LENS_FACING_BACK)
            if (_flashLightState.value) {
                camera?.cameraControl?.enableTorch(false)
                _flashLightState.value = false
            } else {
                camera?.cameraControl?.enableTorch(true)
                _flashLightState.value = true
            }
        else
            throw IllegalStateException(context.getString(R.string.home_view_model_toggle_flash_light_in_front_camera_msg_error))
    }
}