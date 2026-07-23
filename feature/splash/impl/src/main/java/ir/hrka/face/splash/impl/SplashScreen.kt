package ir.hrka.face.splash.impl

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/**
 * Splash screen that requests CAMERA permission, then navigates to the camera feature.
 *
 * @param onNavigateToCamera Invoked once when permission is granted.
 * @param viewModel Splash ViewModel.
 */
@Composable
fun SplashScreen(
    onNavigateToCamera: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(uiState) {
        when (uiState) {
            SplashUiState.Checking -> Unit
            SplashUiState.PermissionRequired -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            SplashUiState.ReadyToNavigate -> {
                delay(400)
                onNavigateToCamera()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (uiState) {
            SplashUiState.Checking,
            SplashUiState.ReadyToNavigate,
            -> CircularProgressIndicator()

            SplashUiState.PermissionRequired -> {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Camera permission is required for face recognition.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 48.dp),
                    )
                    TextButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        Text("Grant permission")
                    }
                }
            }
        }
    }
}
