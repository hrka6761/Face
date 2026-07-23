package ir.hrka.face.camera.impl.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
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
import ir.hrka.face.camera.impl.EnrollQualityGrade

/**
 * Post-scan quality review: Bad / Good / Excellent with next actions.
 *
 * @param grade Quality grade from the self-check.
 * @param scorePercent Rounded quality percent.
 * @param onScanAgain Restart the guided scan.
 * @param onContinueExcellent Continue to person details (Excellent only).
 * @param onDismiss Abort registration.
 */
@Composable
fun EnrollQualityDialog(
    grade: EnrollQualityGrade,
    scorePercent: Int,
    onScanAgain: () -> Unit,
    onContinueExcellent: () -> Unit,
    onDismiss: () -> Unit,
) {
    val body = when (grade) {
        EnrollQualityGrade.Bad ->
            "Average match is Bad ($scorePercent%). " +
                "Please repeat registration and Test Scan."
        EnrollQualityGrade.Good ->
            "Average match is Good ($scorePercent%). " +
                "Please scan and Test Scan again for better accuracy."
        EnrollQualityGrade.Excellent ->
            "Average match is Excellent ($scorePercent%). " +
                "You can enter the person's details and finish registration."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Average match: ${grade.label}") },
        text = {
            Column {
                Text(body)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (grade) {
                        EnrollQualityGrade.Bad -> "Action required: register and test again."
                        EnrollQualityGrade.Good -> "Recommended: register and test again for higher accuracy."
                        EnrollQualityGrade.Excellent -> "Ready to register."
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        confirmButton = {
            when (grade) {
                EnrollQualityGrade.Excellent -> {
                    TextButton(onClick = onContinueExcellent) {
                        Text("Enter details")
                    }
                }
                EnrollQualityGrade.Bad, EnrollQualityGrade.Good -> {
                    TextButton(onClick = onScanAgain) {
                        Text("Scan again")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Final step: collect person details after an excellent scan.
 *
 * @param onDismiss Cancels registration and discards the scan.
 * @param onSave Saves the identity with the entered name.
 */
@Composable
fun EnrollDetailsDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val canSave = name.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Person details") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Face scan accepted. Enter the person's name to save this identity.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Person name") },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim()) },
                enabled = canSave,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
