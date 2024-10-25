package ir.hrka.face.presentation.ui.screens.home

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import ir.hrka.face.R
import ir.hrka.face.core.utilities.Constants.TAG
import ir.hrka.face.presentation.MainActivity
import kotlinx.coroutines.launch

@SuppressLint("SwitchIntDef")
@Composable
fun HomeScreen(activity: MainActivity, navHostController: NavHostController) {

    val configuration = LocalConfiguration.current
    val viewModel: HomeViewModel = hiltViewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewController = PreviewController(activity, viewModel)


    when (configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> PortraitScreen(
            viewModel,
            previewController,
            lifecycleOwner
        )

        Configuration.ORIENTATION_LANDSCAPE -> LandscapeScreen(
            viewModel,
            previewController,
            lifecycleOwner
        )
    }


    BackHandler(
        enabled = true
    ) {
        activity.finish()
    }

    DisposableEffect(Unit) {
        onDispose {
            previewController.unbindPreview()
        }
    }
}

@Composable
fun PortraitScreen(
    viewModel: HomeViewModel,
    previewController: PreviewController,
    lifecycleOwner: LifecycleOwner
) {
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {

        val flashLightState by viewModel.flashLightState.collectAsState()
        val previewSurfaceSize by previewController.previewSurfaceSize.collectAsState()
        val detectedFaces by viewModel.detectedFaces.collectAsState()
        val scope = rememberCoroutineScope()
        val snackBarHostState = remember { SnackbarHostState() }
        val (preview, overlay, controlBtn, snackBar) = createRefs()


        AndroidView(
            modifier = Modifier
                .width(previewSurfaceSize.first.toDp())
                .height(previewSurfaceSize.second.toDp())
                .constrainAs(preview) {
                    top.linkTo(parent.top, margin = 48.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            factory = {
                PreviewView(it).apply {
                    previewController.bindPreview(
                        this,
                        lifecycleOwner,
                        Configuration.ORIENTATION_PORTRAIT
                    )
                }
            }
        )

        Canvas(
            modifier = Modifier
                .width(previewSurfaceSize.first.toDp())
                .height(previewSurfaceSize.second.toDp())
                .constrainAs(overlay) {
                    top.linkTo(parent.top, margin = 48.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ) {
            drawLine(
                color = Color.Gray,
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color.Gray,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )

            repeat(detectedFaces.size) {
                val faceOverlay = previewController.getFaceOverlay(detectedFaces[it])

                drawRect(
                    color = Color.Red,
                    topLeft = Offset(faceOverlay.faceLeft, faceOverlay.faceTop),
                    size = Size(faceOverlay.faceWith, faceOverlay.faceHeight),
                    style = Stroke(width = 5f)
                )

                drawCircle(
                    color = Color.Yellow,
                    radius = 10f,
                    center = Offset(
                        faceOverlay.balanceCircles.horizontalBalanceCircleX,
                        faceOverlay.balanceCircles.horizontalBalanceCircleY
                    )
                )

                drawCircle(
                    color = Color.Blue,
                    radius = 10f,
                    center = Offset(
                        faceOverlay.balanceCircles.verticalBalanceCircleX,
                        faceOverlay.balanceCircles.verticalBalanceCircleY
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .constrainAs(controlBtn) {
                    top.linkTo(overlay.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            FloatingActionButton(
                modifier = Modifier
                    .width(50.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                onClick = {
                    previewController.switchCamera()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.camera_switch),
                    contentDescription = "Camera switch"
                )
            }

            FloatingActionButton(
                modifier = Modifier
                    .width(50.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                onClick = {
                    try {
                        previewController.toggleFlashLight()
                    } catch (e: IllegalStateException) {
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = e.message.toString(),
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(if (flashLightState) R.drawable.flashlight_off else R.drawable.flashlight_on),
                    contentDescription = "Flash light"
                )
            }
        }

        SnackbarHost(
            modifier = Modifier
                .constrainAs(snackBar) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth(),
            hostState = snackBarHostState
        )
    }
}

@Composable
fun LandscapeScreen(
    viewModel: HomeViewModel,
    previewController: PreviewController,
    lifecycleOwner: LifecycleOwner
) {
    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {

        val flashLightState by viewModel.flashLightState.collectAsState()
        val previewSurfaceSize by previewController.previewSurfaceSize.collectAsState()
        val detectedFaces by viewModel.detectedFaces.collectAsState()
        val scope = rememberCoroutineScope()
        val snackBarHostState = remember { SnackbarHostState() }
        val (preview, overlay, snackBar, controlBtn) = createRefs()


        AndroidView(
            modifier = Modifier
                .width(previewSurfaceSize.second.toDp())
                .height(previewSurfaceSize.first.toDp())
                .constrainAs(preview) {
                    top.linkTo(parent.top, margin = 48.dp)
                    start.linkTo(parent.start, margin = 48.dp)
                },
            factory = {
                PreviewView(it).apply {
                    previewController.bindPreview(
                        this,
                        lifecycleOwner,
                        Configuration.ORIENTATION_LANDSCAPE
                    )
                }
            }
        )

        Canvas(
            modifier = Modifier
                .width(previewSurfaceSize.second.toDp())
                .height(previewSurfaceSize.first.toDp())
                .constrainAs(overlay) {
                    top.linkTo(parent.top, margin = 48.dp)
                    start.linkTo(parent.start, margin = 48.dp)
                }
        ) {
            drawLine(
                color = Color.Gray,
                start = Offset(size.width / 2, 0f),
                end = Offset(size.width / 2, size.height),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )

            drawLine(
                color = Color.Gray,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )

            repeat(detectedFaces.size) {
                val faceOverlay = previewController.getFaceOverlay(detectedFaces[it])

                drawRect(
                    color = Color.Red,
                    topLeft = Offset(faceOverlay.faceLeft, faceOverlay.faceTop),
                    size = Size(faceOverlay.faceWith, faceOverlay.faceHeight),
                    style = Stroke(width = 5f)
                )

                drawCircle(
                    color = Color.Yellow,
                    radius = 10f,
                    center = Offset(
                        faceOverlay.balanceCircles.horizontalBalanceCircleX,
                        faceOverlay.balanceCircles.horizontalBalanceCircleY
                    )
                )

                drawCircle(
                    color = Color.Blue,
                    radius = 10f,
                    center = Offset(
                        faceOverlay.balanceCircles.verticalBalanceCircleX,
                        faceOverlay.balanceCircles.verticalBalanceCircleY
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .constrainAs(controlBtn) {
                    top.linkTo(overlay.bottom)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start, margin = 48.dp)
                    end.linkTo(overlay.end)
                },
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            FloatingActionButton(
                modifier = Modifier
                    .width(50.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                onClick = {
                    previewController.switchCamera()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.camera_switch),
                    contentDescription = "Camera switch"
                )
            }

            FloatingActionButton(
                modifier = Modifier
                    .width(50.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(50),
                onClick = {
                    try {
                        previewController.toggleFlashLight()
                    } catch (e: IllegalStateException) {
                        scope.launch {
                            snackBarHostState.showSnackbar(
                                message = e.message.toString(),
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(if (flashLightState) R.drawable.flashlight_off else R.drawable.flashlight_on),
                    contentDescription = "Flash light"
                )
            }
        }

        SnackbarHost(
            modifier = Modifier
                .constrainAs(snackBar) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth(),
            hostState = snackBarHostState
        )
    }
}


@Composable
fun Float.toDp(): Dp {
    val density = LocalDensity.current
    return with(density) { this@toDp.toDp() }
}


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(MainActivity(), rememberNavController())
}