package ir.hrka.face.presentation.ui.screens.home

import android.annotation.SuppressLint
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.hrka.face.core.utilities.Constants.TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@SuppressLint("StaticFieldLeak", "WrongConstant")
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val faceDetector: FaceDetector,
    val cameraProvider: ProcessCameraProvider,
    val preview: Preview,
    val imageAnalysis: ImageAnalysis,
) : ViewModel() {

    private val _flashLightState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val flashLightState: StateFlow<Boolean> = _flashLightState
    private val _detectedFaces: MutableStateFlow<List<Face>> = MutableStateFlow(listOf())
    val detectedFaces: StateFlow<List<Face>> = _detectedFaces
    private val _lensFacing: MutableStateFlow<Int> = MutableStateFlow(LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing


    fun setFlashlightState(state: Boolean) {
        _flashLightState.value = state
    }

    fun setLensFacing(lens: Int) {
        _lensFacing.value = lens
    }

    @OptIn(ExperimentalGetImage::class)
    fun detectFaces(imageProxy: ImageProxy) {
        imageProxy.image?.let { mediaImage ->
            val image =
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            faceDetector
                .process(image)
                .addOnSuccessListener { faces ->
                    _detectedFaces.value = faces
                    Log.i(TAG, "${faces.map { it.boundingBox.width() }}")
                }
                .addOnFailureListener { e ->
                    Log.i(TAG, "Fail: ${e.message}")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}