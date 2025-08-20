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
import ir.hrka.face.core.utilities.FaceEmbeddingGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.math.sqrt

@SuppressLint("StaticFieldLeak", "WrongConstant")
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val faceDetector: FaceDetector,
    val cameraProvider: ProcessCameraProvider,
    val preview: Preview,
    val imageAnalysis: ImageAnalysis,
    private val faceEmbeddingGenerator: FaceEmbeddingGenerator
) : ViewModel() {

    private val _flashLightState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val flashLightState: StateFlow<Boolean> = _flashLightState
    private val _detectedFaces: MutableStateFlow<List<Face>> = MutableStateFlow(listOf())
    val detectedFaces: StateFlow<List<Face>> = _detectedFaces
    private val _lensFacing: MutableStateFlow<Int> = MutableStateFlow(LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing
    private val _surfaceSize: MutableStateFlow<Pair<Float, Float>> =
        MutableStateFlow(Pair(480f, 640f))
    val surfaceSize: StateFlow<Pair<Float, Float>> = _surfaceSize
    private lateinit var hamidrezaEmbeddings: DoubleArray

    fun setFlashlightState(state: Boolean) {
        _flashLightState.value = state
    }

    fun setLensFacing(lens: Int) {
        _lensFacing.value = lens
    }

    fun setSurfaceSize(x: Int, y: Int) {
        if (x != 480 && y != 640)
            _surfaceSize.value = Pair(x.toFloat(), y.toFloat())
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
                    if (faces.isNotEmpty()) {
                        val embeddings = faceEmbeddingGenerator.generate(
                            imageProxy = imageProxy,
                            boundingBox = faces[0].boundingBox
                        )

                        embeddings?.get(0)?.let {
                            val emb =  it.map { i -> i.toDouble() }.toDoubleArray()
                            if (!this::hamidrezaEmbeddings.isInitialized) {
                                hamidrezaEmbeddings =emb
                                Log.i(TAG, "hamidrezaEmbeddings -> ${hamidrezaEmbeddings.contentToString()}")
                            }else {
//                                Log.i(TAG, "embeddings -> ${emb.contentToString()}")
                                val res = (cosineSimilarity(emb, hamidrezaEmbeddings) * 100).toInt()
                                if (res >= 90) Log.i(TAG, "result: $res")
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.i(TAG, "Face detection ailed: ${e.message}")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    fun cosineSimilarity(a: DoubleArray, b: DoubleArray): Double {
        require(a.size == b.size) { "Vectors must have the same size" }

        var dot = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        return if (normA == 0.0 || normB == 0.0) {
            0.0 // avoid division by zero
        } else {
            dot / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
        }
    }
}