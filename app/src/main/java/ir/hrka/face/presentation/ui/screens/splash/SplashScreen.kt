package ir.hrka.face.presentation.ui.screens.splash

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import ir.hrka.face.R
import ir.hrka.face.core.utilities.Constants.TAG
import ir.hrka.face.core.utilities.Screen.Home
import ir.hrka.face.presentation.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SplashScreen(activity: MainActivity, navHostController: NavHostController) {

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

        if (!viewModel.hasAllPermissions())
            AlertDialog(
                modifier = Modifier.fillMaxWidth(),
                onDismissRequest = {},
                confirmButton = {
                    TextButton(
                        onClick = {
                            permissionLauncher.launch(viewModel.requiredPermissions)
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
                icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                title = { Text(stringResource(R.string.splash_screen_permission_dialog_title_text)) },
                text = {
                    Text(
                        stringResource(
                            R.string.splash_screen_permission_dialog_description_text,
                            viewModel.getListOfDeniedPermissions()
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 16.dp
            )
    }

    LaunchedEffect(permissionState) {
        if (!viewModel.hasAllPermissions())
            return@LaunchedEffect

        delay(1000)
        withContext(Dispatchers.Main) {
            navHostController.navigate(Home())
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(MainActivity(), rememberNavController())
}