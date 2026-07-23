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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.hrka.face.camera.impl.EnrollPoseStep

/**
 * Dialog that collects a person name and guides multi-pose registration progress.
 *
 * @param isEnrolling Whether sample collection / save is in progress.
 * @param enrollProgress Number of templates collected so far.
 * @param enrollTargetCount Desired template count across all poses.
 * @param enrollStep Active guided pose step.
 * @param enrollStepProgress Samples collected for the current pose.
 * @param enrollStepTarget Samples required for the current pose.
 * @param enrollHint Live guidance for head pose.
 * @param onDismiss Called when the dialog is cancelled.
 * @param onConfirm Called with the trimmed name when the user confirms.
 */
@Composable
fun EnrollPersonDialog(
    isEnrolling: Boolean,
    enrollProgress: Int,
    enrollTargetCount: Int,
    enrollStep: EnrollPoseStep?,
    enrollStepProgress: Int,
    enrollStepTarget: Int,
    enrollHint: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val canSave = name.trim().isNotEmpty() && !isEnrolling

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(
                if (isEnrolling) {
                    enrollStep?.title ?: "Capturing face samples"
                } else {
                    "Register identity"
                },
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isEnrolling) {
                    Text(
                        text = enrollStep?.instruction ?: "Follow the on-screen pose guides.",
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(enrollHint.ifBlank { "Hold still…" })
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("This step: $enrollStepProgress / $enrollStepTarget")
                    LinearProgressIndicator(
                        progress = {
                            if (enrollStepTarget <= 0) 0f
                            else (enrollStepProgress.toFloat() / enrollStepTarget).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Overall: $enrollProgress / $enrollTargetCount")
                    LinearProgressIndicator(
                        progress = {
                            if (enrollTargetCount <= 0) 0f
                            else (enrollProgress.toFloat() / enrollTargetCount).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Steps: full face → left profile → right profile. " +
                            "Samples are saved only when your head angle is correct.",
                    )
                } else {
                    Text(
                        "You will be guided through three poses:\n" +
                            "1) Full face (look straight)\n" +
                            "2) Left profile\n" +
                            "3) Right profile\n\n" +
                            "Keep your face inside the frame and follow each instruction carefully.",
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
                    Text("Start")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(if (isEnrolling) "Abort" else "Cancel")
            }
        },
    )
}
