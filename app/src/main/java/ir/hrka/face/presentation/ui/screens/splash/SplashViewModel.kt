package ir.hrka.face.presentation.ui.screens.splash

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hrka.face.core.utilities.Constants.TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val requiredPermissions = arrayOf(CAMERA)
    private val _permissionState: MutableStateFlow<Map<String, Boolean?>> =
        MutableStateFlow(initPermissionState())
    val permissionState: StateFlow<Map<String, Boolean?>> = _permissionState


    fun hasAllPermissions(): Boolean {
        requiredPermissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            )
                return false
        }

        return true
    }

    fun getListOfDeniedPermissions(): String =
        buildString {
            append("\n")
            requiredPermissions.forEach { permission ->
                if (ContextCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                )
                    append("\n* " + permission.split(".").last())
            }
        }


    fun setPermissionState(state: Map<String, Boolean>) {
        _permissionState.value = state
    }


    private fun initPermissionState(): Map<String, Boolean?> {
        val state = mutableMapOf<String, Boolean?>()

        requiredPermissions.forEach { permission ->
            state[permission] = null
        }

        return state
    }
}