package ir.hrka.face.camera.impl.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import ir.hrka.face.camera.impl.CameraMode
import ir.hrka.face.camera.impl.CameraViewModel
import ir.hrka.face.camera.impl.EnrollPhase
import ir.hrka.face.camera.impl.EnrollQualityGrade
import kotlinx.coroutines.launch

/**
 * Fullscreen camera screen with recognition and register modes.
 *
 * The camera preview stays off until the face recognition engine is ready.
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

    LaunchedEffect(uiState.errorMessage, uiState.isPreparingEngine) {
        val message = uiState.errorMessage ?: return@LaunchedEffect
        // Keep prepare failures on the overlay (with Retry); don't snackbar them away.
        if (uiState.isPreparingEngine || !uiState.engineReady) return@LaunchedEffect
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
            if (uiState.engineReady) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView ->
                            binder.start(previewView)
                        }
                    },
                )

                when (uiState.mode) {
                    CameraMode.Recognition -> {
                        FaceOverlay(
                            faces = uiState.faces,
                            imageWidth = uiState.imageWidth,
                            imageHeight = uiState.imageHeight,
                            mirrorX = uiState.isFrontCamera,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    CameraMode.Register -> {
                        if (uiState.enrollPhase == EnrollPhase.Idle) {
                            RegisterOverlay(
                                faces = uiState.faces,
                                imageWidth = uiState.imageWidth,
                                imageHeight = uiState.imageHeight,
                                mirrorX = uiState.isFrontCamera,
                                onRegisterClick = viewModel::requestEnroll,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                when (uiState.enrollPhase) {
                    EnrollPhase.AlignEyes -> {
                        EnrollEyeAlignOverlay(
                            faces = uiState.faces,
                            imageWidth = uiState.imageWidth,
                            imageHeight = uiState.imageHeight,
                            mirrorX = uiState.isFrontCamera,
                            eyesAligned = uiState.enrollEyesAligned,
                            onEyesAlignedChanged = viewModel::updateEyesAligned,
                            onStart = viewModel::startEnrollScan,
                            onAbort = viewModel::dismissEnroll,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    EnrollPhase.Scanning -> {
                        if (uiState.enrollStep != null) {
                            EnrollGuidanceOverlay(
                                step = uiState.enrollStep!!,
                                faces = uiState.faces,
                                imageWidth = uiState.imageWidth,
                                imageHeight = uiState.imageHeight,
                                mirrorX = uiState.isFrontCamera,
                                hint = uiState.enrollHint,
                                stepProgress = uiState.enrollStepProgress,
                                stepTarget = uiState.enrollStepTarget,
                                overallProgress = uiState.enrollProgress,
                                overallTarget = uiState.enrollTargetCount,
                                poseAligned = uiState.enrollPoseAligned,
                                yawProgress = uiState.enrollYawProgress,
                                onGuideAlignedChanged = viewModel::updateGuideAligned,
                                onAbort = viewModel::dismissEnroll,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    EnrollPhase.ReadyToTest -> {
                        EnrollReadyToTestOverlay(
                            onTestScan = viewModel::startTestScan,
                            onAbort = viewModel::dismissEnroll,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    EnrollPhase.Testing -> {
                        uiState.enrollTestStep?.let { step ->
                            EnrollTestScanOverlay(
                                step = step,
                                faces = uiState.faces,
                                imageWidth = uiState.imageWidth,
                                imageHeight = uiState.imageHeight,
                                mirrorX = uiState.isFrontCamera,
                                hint = uiState.enrollHint,
                                progress = uiState.enrollTestProgress,
                                target = uiState.enrollTestTarget,
                                poseAligned = uiState.enrollPoseAligned,
                                onGuideAlignedChanged = viewModel::updateGuideAligned,
                                onAbort = viewModel::dismissEnroll,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    else -> Unit
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val hideSwitcher = uiState.enrollPhase == EnrollPhase.Scanning ||
                        uiState.enrollPhase == EnrollPhase.AlignEyes ||
                        uiState.enrollPhase == EnrollPhase.Testing
                    if (!hideSwitcher) {
                        ModeSwitcher(
                            mode = uiState.mode,
                            onModeChange = viewModel::setMode,
                        )
                    }

                    if (uiState.mode == CameraMode.Recognition) {
                        Surface(
                            modifier = Modifier.padding(top = 4.dp),
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
                    }
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
            } else {
                EnginePrepOverlay(
                    progress = uiState.modelDownloadProgress,
                    label = uiState.errorMessage ?: uiState.modelDownloadLabel,
                    fileIndex = uiState.modelDownloadFileIndex,
                    totalFiles = uiState.modelDownloadTotalFiles,
                    failed = !uiState.isPreparingEngine && !uiState.engineReady,
                    onRetry = viewModel::retryPrepareEngine,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (uiState.engineReady) {
        when (uiState.enrollPhase) {
            EnrollPhase.QualityReview -> {
                val grade = uiState.enrollQualityGrade ?: EnrollQualityGrade.Bad
                EnrollQualityDialog(
                    grade = grade,
                    scorePercent = (uiState.enrollQualityScore * 100f).toInt().coerceIn(0, 100),
                    onScanAgain = viewModel::retryEnrollScan,
                    onContinueExcellent = viewModel::proceedToEnterDetails,
                    onDismiss = viewModel::dismissEnroll,
                )
            }

            EnrollPhase.EnterDetails -> {
                EnrollDetailsDialog(
                    onDismiss = viewModel::dismissEnroll,
                    onSave = viewModel::saveEnroll,
                )
            }

            EnrollPhase.Idle,
            EnrollPhase.AlignEyes,
            EnrollPhase.Scanning,
            EnrollPhase.ReadyToTest,
            EnrollPhase.Testing,
            -> Unit
        }
    }
}

@Composable
private fun ModeSwitcher(
    mode: CameraMode,
    onModeChange: (CameraMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(CameraMode.Recognition, CameraMode.Register)
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.padding(4.dp),
        ) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = mode == option,
                    onClick = { onModeChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = Color.Transparent,
                        inactiveContentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = when (option) {
                            CameraMode.Recognition -> "Recognize"
                            CameraMode.Register -> "Register"
                        },
                    )
                }
            }
        }
    }
}
