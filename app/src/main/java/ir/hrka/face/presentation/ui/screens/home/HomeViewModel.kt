package ir.hrka.face.presentation.ui.screens.home

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hrka.face.R
import ir.hrka.face.core.utilities.Constants.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("StaticFieldLeak", "WrongConstant")
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cameraProvider: ProcessCameraProvider,
    private val preview: Preview,
    private val imageAnalysis: ImageAnalysis,
    private val faceDetector: FaceDetector
) : ViewModel() {

    private lateinit var _previewView: PreviewView
    private lateinit var _lifecycleOwner: LifecycleOwner
    private val _flashLightState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val flashLightState: StateFlow<Boolean> = _flashLightState
    private var camera: Camera? = null
    private var cameraSelector: CameraSelector? = null
    private var lensFacing = LENS_FACING_BACK


    @OptIn(ExperimentalGetImage::class)
    fun bindPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        _previewView = previewView
        _lifecycleOwner = lifecycleOwner
        preview.setSurfaceProvider(previewView.surfaceProvider)
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy: ImageProxy ->
            val mediaImage = imageProxy.image

            if (mediaImage != null) {
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                faceDetector
                    .process(image)
                    .addOnSuccessListener { faces ->
                        Log.i(TAG, "Success: ${faces.size}")
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        Log.i(TAG, "Fail: ${e.message}")
                    }
            }
        }
        camera =
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector!!, preview, imageAnalysis)
        setFlashLightState()
    }

    fun unbindPreview() {
        if (camera != null) {
            cameraProvider.unbind(preview, imageAnalysis)
            camera = null
            cameraSelector = null
        }
    }

    fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            turnOffFlashLight()
            CameraSelector.LENS_FACING_FRONT
        } else
            CameraSelector.LENS_FACING_BACK

        unbindPreview()
        bindPreview(_previewView, _lifecycleOwner)
    }

    @Throws(IllegalStateException::class)
    fun toggleFlashLight() {
        if (lensFacing == LENS_FACING_BACK)
            if (_flashLightState.value)
                turnOffFlashLight()
            else
                turnOnFlashLight()
        else
            throw IllegalStateException(context.getString(R.string.home_view_model_toggle_flash_light_in_front_camera_msg_error))
    }


    private fun setFlashLightState() {
        CoroutineScope(Dispatchers.Main).launch {
            _flashLightState.collect { state ->
                camera?.cameraControl?.enableTorch(state)
            }
        }
    }

    private fun turnOnFlashLight() {
        camera?.cameraControl?.enableTorch(true)
        _flashLightState.value = true
    }

    private fun turnOffFlashLight() {
        camera?.cameraControl?.enableTorch(false)
        _flashLightState.value = false
    }
}