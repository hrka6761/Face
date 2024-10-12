package ir.hrka.face.di

import android.content.Context
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class GlobalModule {

    @Provides
    @Singleton
    fun provideCameraProvider(@ApplicationContext context: Context): ProcessCameraProvider =
        ProcessCameraProvider.getInstance(context).get()


    @Provides
    @Singleton
    fun getPreview(): Preview = Preview.Builder().build()
}