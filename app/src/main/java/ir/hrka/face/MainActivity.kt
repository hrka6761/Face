package ir.hrka.face

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import ir.hrka.face.navigation.Navigator
import ir.hrka.face.navigation.rememberNavigationState
import ir.hrka.face.splash.api.SplashNavKey
import ir.hrka.face.camera.api.CameraNavKey
import ir.hrka.face.ui.theme.FaceTheme

/**
 * Single-activity entry point for the Face app.
 *
 * Hosts Navigation 3 with splash (permission) and camera destinations.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FaceTheme {
                val navigationState = rememberNavigationState(
                    startDestination = SplashNavKey,
                    topLevelDestinations = setOf(SplashNavKey, CameraNavKey),
                )
                val navigator = remember(navigationState) { Navigator(navigationState) }
                FaceApp(
                    navigationState = navigationState,
                    navigator = navigator,
                )
            }
        }
    }
}
