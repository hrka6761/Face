package ir.hrka.face.presentation.ui.screens.splash

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _permissionState: MutableStateFlow<Map<String, Boolean?>> = MutableStateFlow(mapOf(Pair(CAMERA, null)))
    val permissionState: StateFlow<Map<String, Boolean?>> = _permissionState


    fun hasAllPermissions(permissions: Array<String>): Boolean {

        permissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                return false
        }

        return true
    }

    fun getListOfDeniedPermissions(permissions: Array<String>) : String {

        return buildString {
            append("\n")
            permissions.forEach { permission ->
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                    append("\n* " + permission.split(".").last())
            }
        }
    }

    fun setPermissionState(state: Map<String, Boolean>) {

        _permissionState.value = state
    }
}