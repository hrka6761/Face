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
import org.tensorflow.lite.Interpreter
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
        Interpreter(loadModelFile(context.assets))


    private fun loadModelFile(assetManager: AssetManager): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd("mobilefacenet.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
}