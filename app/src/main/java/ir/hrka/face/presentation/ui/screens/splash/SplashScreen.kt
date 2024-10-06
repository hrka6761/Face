package ir.hrka.face.presentation.ui.screens.splash

import android.Manifest.permission.CAMERA
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import ir.hrka.face.R
import ir.hrka.face.core.utilities.Constants.TAG
import ir.hrka.face.core.utilities.Screen.Main
import ir.hrka.face.presentation.MainActivity
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(activity: MainActivity, navHostController: NavHostController) {

    val requiredPermissions = arrayOf(CAMERA)
    val viewModel: SplashViewModel = hiltViewModel()
    val permissionState by viewModel.permissionState.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        RequestMultiplePermissions()
    ) { result ->
        viewModel.setPermissionState(result)
    }


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .width(50.dp)
                .height(50.dp)
        )

        if (!viewModel.hasAllPermissions(requiredPermissions))
            AlertDialog(
                modifier = Modifier.fillMaxWidth(),
                onDismissRequest = {},
                confirmButton = {
                    TextButton(
                        onClick = {
                            permissionLauncher.launch(requiredPermissions)
                        }
                    ) { Text(text = stringResource(R.string.splash_screen_permission_dialog_allow_text_button)) }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            activity.finish()
                        }
                    ) { Text(text = stringResource(R.string.splash_screen_permission_dialog_deny_text_button)) }
                },
                icon = { Icons.Default.Notifications },
                title = { Text(stringResource(R.string.splash_screen_permission_dialog_title_text)) },
                text = {
                    Text(
                        stringResource(
                            R.string.splash_screen_permission_dialog_description_text,
                            viewModel.getListOfDeniedPermissions(requiredPermissions)
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 10.dp
            )
        else {
            LaunchedEffect(Unit) {
                delay(1000)
                navHostController.navigate(Main())
            }
        }
    }

    LaunchedEffect(permissionState) {
        requiredPermissions.forEach { permission ->
            if (permissionState[permission] != true)
                return@LaunchedEffect
        }

        delay(1000)
        navHostController.navigate(Main())
    }
}


@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(LocalContext.current as MainActivity, rememberNavController())
}