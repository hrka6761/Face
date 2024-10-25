package ir.hrka.face.presentation.ui.screens.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.hardware.camera2.CameraMetadata.LENS_FACING_BACK
import android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.face.Face
import ir.hrka.face.R
import ir.hrka.face.core.utilities.Constants.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak", "WrongConstant")
class PreviewController(
    private val context: Context,
    private val viewModel: HomeViewModel
) {

    private lateinit var _previewView: PreviewView
    private lateinit var _lifecycleOwner: LifecycleOwner
    private var _orientation: Int = ORIENTATION_PORTRAIT
    private val _previewSurfaceSize: MutableStateFlow<Pair<Float, Float>> =
        MutableStateFlow(Pair(480f, 640f))
    val previewSurfaceSize: StateFlow<Pair<Float, Float>> = _previewSurfaceSize
    private var camera: Camera? = null
    private var cameraSelector: CameraSelector? = null


    fun bindPreview(previewView: PreviewView, lifecycleOwner: LifecycleOwner, orientation: Int) {
        _previewView = previewView
        _lifecycleOwner = lifecycleOwner
        _orientation = orientation
        viewModel.preview.setSurfaceProvider(previewView.surfaceProvider)
        cameraSelector =
            CameraSelector.Builder().requireLensFacing(viewModel.lensFacing.value).build()
        viewModel.imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
            if (imageProxy.width != 480 && imageProxy.height != 640)
                _previewSurfaceSize.value =
                    Pair(imageProxy.height.toFloat(), imageProxy.width.toFloat())
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
        bindPreview(_previewView, _lifecycleOwner, _orientation)
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

    fun getFaceOverlay(face: Face) =
        FaceOverlay(
            faceLeft = getOverlayLeft(face),
            faceTop = getOverlayTop(face),
            faceWith = getOverlayWidth(face),
            faceHeight = getOverlayHeight(face),
            balanceCircles = getBalanceCircles(face)
        )

    private fun getOverlayLeft(face: Face) =
        if (isImageFlipped())
            previewSurfaceSize.value.first -
                    face.boundingBox.centerX().toFloat() -
                    (face.boundingBox.width().toFloat() / 2)
        else
            face.boundingBox.centerX().toFloat() - (face.boundingBox.width()
                .toFloat() / 2)

    private fun getOverlayTop(face: Face) =
        face.boundingBox.centerY().toFloat() - (face.boundingBox.height().toFloat() / 2)

    private fun getOverlayWidth(face: Face) = face.boundingBox.width().toFloat()

    private fun getOverlayHeight(face: Face) = face.boundingBox.height().toFloat()

    private fun getBalanceCircles(face: Face) =
        BalanceCircles(
            horizontalBalanceCircleX = getHorizontalBalanceCircleX(face),
            horizontalBalanceCircleY = getHorizontalBalanceCircleY(face),
            verticalBalanceCircleX = getVerticalBalanceCircleX(face),
            verticalBalanceCircleY = getVerticalBalanceCircleY(face)
        )

    private fun getHorizontalBalanceCircleX(face: Face): Float {
        val overlayWidth =
            if (_orientation == ORIENTATION_PORTRAIT)
                previewSurfaceSize.value.first
            else
                previewSurfaceSize.value.second

        return if (getFaceHorizontalGravity(face) == FaceGravity.Left)
            overlayWidth - 50f
        else
            50f
    }

    private fun getHorizontalBalanceCircleY(face: Face): Float {
        val overlayHeight =
            if (_orientation == ORIENTATION_PORTRAIT)
                previewSurfaceSize.value.second
            else
                previewSurfaceSize.value.first

        return overlayHeight / 2
    }

    private fun getVerticalBalanceCircleX(face: Face): Float {
        val overlayWidth =
            if (_orientation == ORIENTATION_PORTRAIT)
                previewSurfaceSize.value.first
            else
                previewSurfaceSize.value.second

        return overlayWidth / 2
    }

    private fun getVerticalBalanceCircleY(face: Face): Float {
        val overlayHeight =
            if (_orientation == ORIENTATION_PORTRAIT)
                previewSurfaceSize.value.second
            else
                previewSurfaceSize.value.first

        return if (getFaceVerticalGravity(face) == FaceGravity.Top)
            overlayHeight - 50f
        else
            50f
    }

    private fun getFaceHorizontalGravity(face: Face): FaceGravity {
        val overlayWidth =
            if (_orientation == ORIENTATION_PORTRAIT)
                previewSurfaceSize.value.first
            else
                previewSurfaceSize.value.second

        return if (isImageFlipped())
            if (face.boundingBox.centerX() <= overlayWidth / 2)
                FaceGravity.Right
            else
                FaceGravity.Left
        else
            if (face.boundingBox.centerX() >= overlayWidth / 2)
                FaceGravity.Right
            else
                FaceGravity.Left
    }

    private fun getFaceVerticalGravity(face: Face): FaceGravity {
        val overlayHeight =
            if (_orientation == ORIENTATION_PORTRAIT)
                previewSurfaceSize.value.second
            else
                previewSurfaceSize.value.first

        return if (face.boundingBox.centerY() >= overlayHeight / 2)
            FaceGravity.Bottom
        else
            FaceGravity.Top
    }

    private fun isImageFlipped() = viewModel.lensFacing.value == LENS_FACING_FRONT
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