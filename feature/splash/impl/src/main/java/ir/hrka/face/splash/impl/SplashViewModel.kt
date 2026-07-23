package ir.hrka.face.splash.impl

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * UI state for the splash permission gate.
 */
sealed class SplashUiState {
    /** Evaluating whether CAMERA permission is already granted. */
    data object Checking : SplashUiState()

    /** Permission missing; UI should request it. */
    data object PermissionRequired : SplashUiState()

    /** Permission granted; UI may navigate to the camera destination. */
    data object ReadyToNavigate : SplashUiState()
}

/**
 * ViewModel for splash permission handling.
 *
 * @param appContext Application context used for permission checks.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Checking)

    /** Cold UI state stream. */
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        refreshPermissionState()
    }

    /**
     * Re-checks CAMERA permission and updates [uiState].
     */
    fun refreshPermissionState() {
        _uiState.value = if (hasCameraPermission()) {
            SplashUiState.ReadyToNavigate
        } else {
            SplashUiState.PermissionRequired
        }
    }

    /**
     * Called after the system permission dialog returns.
     *
     * @param granted Whether CAMERA was granted.
     */
    fun onPermissionResult(granted: Boolean) {
        _uiState.value = if (granted) {
            SplashUiState.ReadyToNavigate
        } else {
            SplashUiState.PermissionRequired
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
}
