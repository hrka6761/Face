package ir.hrka.face.camera.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import ir.hrka.face.camera.api.CameraNavKey
import ir.hrka.face.camera.impl.ui.CameraScreen
import ir.hrka.face.navigation.Navigator

/**
 * Registers the camera destination.
 *
 * @param navigator Unused for now; reserved for future nested navigation.
 */
@Suppress("UNUSED_PARAMETER")
fun EntryProviderScope<NavKey>.cameraEntry(navigator: Navigator) {
    entry<CameraNavKey> {
        CameraScreen()
    }
}
