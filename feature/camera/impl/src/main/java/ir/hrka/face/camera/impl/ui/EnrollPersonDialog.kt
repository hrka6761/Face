package ir.hrka.face.camera.impl.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dialog that collects a person name and shows multi-sample registration progress.
 *
 * @param isEnrolling Whether sample collection / save is in progress.
 * @param enrollProgress Number of templates collected so far.
 * @param enrollTargetCount Desired template count.
 * @param onDismiss Called when the dialog is cancelled.
 * @param onConfirm Called with the trimmed name when the user confirms.
 */
@Composable
fun EnrollPersonDialog(
    isEnrolling: Boolean,
    enrollProgress: Int,
    enrollTargetCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val canSave = name.trim().isNotEmpty() && !isEnrolling

    AlertDialog(
        onDismissRequest = { if (!isEnrolling) onDismiss() },
        title = { Text(if (isEnrolling) "Capturing face samples" else "Register identity") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isEnrolling) {
                    Text(
                        "Hold still and slowly move a little closer/farther. " +
                            "Collecting templates improves recognition at any distance.",
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (enrollTargetCount <= 0) 0f
                            else (enrollProgress.toFloat() / enrollTargetCount).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Samples: $enrollProgress / $enrollTargetCount")
                } else {
                    Text(
                        "After you tap Register, keep your face in the frame for ~2 seconds " +
                            "while the app captures multiple samples.",
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = true,
                        label = { Text("Person name") },
                    )
                }
            }
        },
        confirmButton = {
            if (!isEnrolling) {
                TextButton(
                    onClick = { onConfirm(name.trim()) },
                    enabled = canSave,
                ) {
                    Text("Register")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isEnrolling,
            ) {
                Text("Cancel")
            }
        },
    )
}
