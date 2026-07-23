package ir.hrka.face.camera.impl.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.hrka.face.camera.impl.CameraBinder
import ir.hrka.face.camera.impl.CameraViewModel
import kotlinx.coroutines.launch

/**
 * Fullscreen camera screen with face overlays, face count, and enroll dialog.
 *
 * @param viewModel Camera feature ViewModel.
 */
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Bind to the Activity lifecycle — Navigation entry lifecycles can leave CameraX preview black.
    val binder = remember(activity, viewModel) {
        val owner = requireNotNull(activity) { "CameraScreen requires a ComponentActivity host" }
        CameraBinder(
            appContext = context.applicationContext,
            lifecycleOwner = owner,
            viewModel = viewModel,
        )
    }

    BackHandler(enabled = true) {
        activity?.finish()
    }

    DisposableEffect(binder) {
        onDispose { binder.release() }
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        if (message.contains("Task was cancelled", ignoreCase = true) ||
            message.contains("Job was cancelled", ignoreCase = true)
        ) {
            viewModel.consumeError()
            return@LaunchedEffect
        }
        snackbarHostState.showSnackbar(message)
        viewModel.consumeError()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        binder.start(previewView)
                    }
                },
            )

            FaceOverlay(
                faces = uiState.faces,
                imageWidth = uiState.imageWidth,
                imageHeight = uiState.imageHeight,
                mirrorX = uiState.isFrontCamera,
                onSaveClick = viewModel::requestEnroll,
                modifier = Modifier.fillMaxSize(),
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                color = Color.Black.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = "Faces: ${uiState.faceCount}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            FloatingActionButton(
                onClick = { binder.switchCamera() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp),
            ) {
                Icon(Icons.Default.Cameraswitch, contentDescription = "Switch camera")
            }

            FloatingActionButton(
                onClick = {
                    if (!binder.toggleTorch()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Torch is not available on this camera")
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
            ) {
                Icon(
                    imageVector = if (uiState.isTorchOn) {
                        Icons.Default.FlashOn
                    } else {
                        Icons.Default.FlashOff
                    },
                    contentDescription = "Toggle torch",
                )
            }
        }
    }

    uiState.enrollTarget?.let {
        EnrollPersonDialog(
            isEnrolling = uiState.isEnrolling,
            enrollProgress = uiState.enrollProgress,
            enrollTargetCount = uiState.enrollTargetCount,
            onDismiss = viewModel::dismissEnroll,
            onConfirm = viewModel::confirmEnroll,
        )
    }
}
