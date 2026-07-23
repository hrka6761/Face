package ir.hrka.face.splash.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ir.hrka.face.camera.api.CameraNavKey
import ir.hrka.face.navigation.Navigator
import ir.hrka.face.splash.api.SplashNavKey
import ir.hrka.face.splash.impl.SplashScreen

/**
 * Registers the splash destination.
 *
 * @param navigator App navigator used to open the camera screen.
 */
fun EntryProviderScope<NavKey>.splashEntry(navigator: Navigator) {
    entry<SplashNavKey> {
        SplashScreen(
            onNavigateToCamera = { navigator.replaceTopLevel(CameraNavKey) },
        )
    }
}
