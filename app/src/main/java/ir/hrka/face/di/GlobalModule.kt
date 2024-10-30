package ir.hrka.face.di

import android.content.Context
import android.content.res.AssetManager
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.hrka.face.core.utilities.Constants.FACE_NET_INPUT_IMAGE_SIZE
import ir.hrka.face.core.utilities.Constants.FACE_NET_MODEL_NAME
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class GlobalModule {

    @Provides
    @Singleton
    fun provideFaceDetectorOptions(): FaceDetectorOptions =
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f)
            .build()

    @Provides
    @Singleton
    fun provideFaceDetector(options: FaceDetectorOptions): FaceDetector =
        FaceDetection.getClient(options)

    @Provides
    @Singleton
    fun provideCameraProvider(@ApplicationContext context: Context): ProcessCameraProvider =
        ProcessCameraProvider.getInstance(context).get()

    @Provides
    @Singleton
    fun providePreview(): Preview = Preview.Builder().build()

    @Provides
    @Singleton
    fun provideImageAnalysis(): ImageAnalysis = ImageAnalysis.Builder().build()

    @Provides
    @Singleton
    fun provideInterpreter(@ApplicationContext context: Context): Interpreter =
        Interpreter(loadModelFile(context.assets), Interpreter.Options())

    @Provides
    @Singleton
    fun provideImageProcessor(): ImageProcessor =
        ImageProcessor
            .Builder()
            .add(
                ResizeOp(
                    FACE_NET_INPUT_IMAGE_SIZE,
                    FACE_NET_INPUT_IMAGE_SIZE,
                    ResizeOp.ResizeMethod.BILINEAR
                )
            )
            .add(NormalizeOp(0f, 255f))
            .build()


    private fun loadModelFile(assetManager: AssetManager): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(FACE_NET_MODEL_NAME)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}