package ir.hrka.face

import android.app.Application
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import ir.hrka.download.manager.api.DownloadWorkerFactory

/**
 * Application entry point that enables Hilt and WorkManager for model downloads.
 */
@HiltAndroidApp
class FaceApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(DownloadWorkerFactory())
            .build()
}
