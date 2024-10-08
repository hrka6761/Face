package ir.hrka.face.presentation.ui.screens.home


import androidx.camera.view.PreviewView
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import ir.hrka.face.R
import ir.hrka.face.presentation.MainActivity
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(activity: MainActivity, navHostController: NavHostController) {

    val viewModel: HomeViewModel = hiltViewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val flashLightState by viewModel.flashLightState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }


    ConstraintLayout(
        modifier = Modifier.fillMaxSize()
    ) {
        val (preview, switchBtn, flashBtn, snackBar) = createRefs()

        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .constrainAs(preview) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            factory = {
                PreviewView(it).apply {
                    viewModel.initCameraPreview(this, lifecycleOwner)
                }
            }
        )

        FloatingActionButton(
            modifier = Modifier
                .width(50.dp)
                .height(50.dp)
                .alpha(0.5f)
                .constrainAs(switchBtn) {
                    bottom.linkTo(parent.bottom, margin = 16.dp)
                    end.linkTo(parent.end, margin = 16.dp)
                },
            shape = RoundedCornerShape(50),
            onClick = {
                viewModel.switchCamera()
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.camera_switch),
                contentDescription = "Camera switch"
            )
        }

        FloatingActionButton(
            modifier = Modifier
                .width(40.dp)
                .height(40.dp)
                .alpha(if (flashLightState) 1f else 0.5f)
                .constrainAs(flashBtn) {
                    bottom.linkTo(switchBtn.top, margin = 16.dp)
                    end.linkTo(parent.end, margin = 21.dp)
                },
            shape = RoundedCornerShape(50),
            onClick = {
                try {
                    viewModel.toggleFlashLight()
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
                painter = painterResource(R.drawable.flash_light),
                contentDescription = "Flash light"
            )
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


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(MainActivity(), rememberNavController())
}