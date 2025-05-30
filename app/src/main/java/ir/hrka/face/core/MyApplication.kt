package ir.hrka.face.core

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        System.setProperty("kotlinx.coroutines.debug", "on")
    }
}