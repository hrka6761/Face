package ir.hrka.face.presentation

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ir.hrka.face.core.utilities.Screen.Splash
import ir.hrka.face.core.utilities.Screen.Main
import ir.hrka.face.presentation.ui.screens.main.MainScreen
import ir.hrka.face.presentation.ui.screens.splash.SplashScreen
import ir.hrka.face.presentation.ui.theme.FaceTheme

@Composable
fun AppContent() {
    val navHostController = rememberNavController()
    val activity = (LocalContext.current as Activity)

    FaceTheme {
        NavHost(
            modifier = Modifier.fillMaxSize(),
            navController = navHostController,
            startDestination = Splash()
        ) {
            composable(route = Splash()) {
                SplashScreen(navHostController)
            }
            composable(route = Main()) {
                MainScreen(navHostController)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun AppContentPreview() {
    FaceTheme {
        AppContent()
    }
}