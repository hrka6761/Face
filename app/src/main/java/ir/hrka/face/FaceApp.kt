package ir.hrka.face

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import ir.hrka.face.camera.impl.navigation.cameraEntry
import ir.hrka.face.navigation.NavigationState
import ir.hrka.face.navigation.Navigator
import ir.hrka.face.navigation.toEntries
import ir.hrka.face.splash.impl.navigation.splashEntry

/**
 * Root Compose UI that hosts Navigation 3 destinations.
 *
 * @param navigationState Persistent navigation state.
 * @param navigator Navigation event handler.
 * @param modifier Optional modifier.
 */
@Composable
fun FaceApp(
    navigationState: NavigationState,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val entryProvider = entryProvider {
                splashEntry(navigator)
                cameraEntry(navigator)
            }

            NavDisplay(
                entries = navigationState.toEntries(entryProvider),
                onBack = {
                    try {
                        navigator.goBack()
                    } catch (_: IllegalStateException) {
                        // Already at start destination.
                    }
                },
            )
        }
    }
}
