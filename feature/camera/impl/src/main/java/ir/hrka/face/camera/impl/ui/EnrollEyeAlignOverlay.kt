package ir.hrka.face.camera.impl.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hrka.face.camera.impl.TrackedFaceUi

/**
 * Pre-scan overlay: fixed eye targets + Start button (scan does not begin automatically).
 */
@Composable
fun EnrollEyeAlignOverlay(
    faces: List<TrackedFaceUi>,
    imageWidth: Int,
    imageHeight: Int,
    mirrorX: Boolean,
    eyesAligned: Boolean,
    onEyesAlignedChanged: (Boolean) -> Unit,
    onStart: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewWidth by remember { mutableIntStateOf(0) }
    var viewHeight by remember { mutableIntStateOf(0) }

    val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }

    val aligned = remember(face, imageWidth, imageHeight, mirrorX, viewWidth, viewHeight) {
        if (viewWidth <= 0 || viewHeight <= 0 || face == null || imageWidth <= 0) {
            false
        } else {
            val leftTarget = EyeGuideLayout.leftEyeTarget(viewWidth.toFloat(), viewHeight.toFloat())
            val rightTarget = EyeGuideLayout.rightEyeTarget(viewWidth.toFloat(), viewHeight.toFloat())
            val radius = EyeGuideLayout.eyeRadius(viewWidth.toFloat(), viewHeight.toFloat())

            // Analysis LEFT_EYE is the person's left eye. With a mirrored front preview,
            // that eye appears on the right side of the screen.
            val analysisLeft = face.leftEye
            val analysisRight = face.rightEye
            val viewLeftEye = analysisLeft?.let {
                FaceCoordinateMapper.mapPoint(
                    x = it.x,
                    y = it.y,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    viewWidth = viewWidth.toFloat(),
                    viewHeight = viewHeight.toFloat(),
                    mirrorX = mirrorX,
                )
            }
            val viewRightEye = analysisRight?.let {
                FaceCoordinateMapper.mapPoint(
                    x = it.x,
                    y = it.y,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    viewWidth = viewWidth.toFloat(),
                    viewHeight = viewHeight.toFloat(),
                    mirrorX = mirrorX,
                )
            }

            // After mirroring, person's left eye maps near the screen-right target.
            val leftHit = if (mirrorX) {
                EyeGuideLayout.isEyeOnTarget(viewLeftEye, rightTarget, radius)
            } else {
                EyeGuideLayout.isEyeOnTarget(viewLeftEye, leftTarget, radius)
            }
            val rightHit = if (mirrorX) {
                EyeGuideLayout.isEyeOnTarget(viewRightEye, leftTarget, radius)
            } else {
                EyeGuideLayout.isEyeOnTarget(viewRightEye, rightTarget, radius)
            }
            leftHit && rightHit
        }
    }

    LaunchedEffect(aligned) {
        onEyesAlignedChanged(aligned)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                viewWidth = it.width
                viewHeight = it.height
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (viewWidth <= 0 || viewHeight <= 0) return@Canvas
            val oval = EyeGuideLayout.ovalRect(size.width, size.height)
            val leftTarget = EyeGuideLayout.leftEyeTarget(size.width, size.height)
            val rightTarget = EyeGuideLayout.rightEyeTarget(size.width, size.height)
            val radius = EyeGuideLayout.eyeRadius(size.width, size.height)
            val ring = if (aligned) Color(0xFF4CAF50) else Color(0xFFFFC107)

            drawRect(Color.Black.copy(alpha = 0.40f))
            drawOval(
                color = ring,
                topLeft = Offset(oval.left, oval.top),
                size = Size(oval.width(), oval.height()),
                style = Stroke(width = 5f),
            )

            fun drawEyeTarget(center: Offset, label: String) {
                drawCircle(
                    color = ring.copy(alpha = 0.25f),
                    radius = radius,
                    center = center,
                )
                drawCircle(
                    color = ring,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 4f),
                )
                drawCircle(
                    color = ring,
                    radius = 5f,
                    center = center,
                )
            }

            drawEyeTarget(leftTarget, "L")
            drawEyeTarget(rightTarget, "R")

            // Live eye dots when visible.
            face?.leftEye?.let { eye ->
                val mapped = FaceCoordinateMapper.mapPoint(
                    eye.x, eye.y, imageWidth, imageHeight, size.width, size.height, mirrorX,
                )
                drawCircle(Color.Cyan, radius = 7f, center = Offset(mapped.x, mapped.y))
            }
            face?.rightEye?.let { eye ->
                val mapped = FaceCoordinateMapper.mapPoint(
                    eye.x, eye.y, imageWidth, imageHeight, size.width, size.height, mirrorX,
                )
                drawCircle(Color.Cyan, radius = 7f, center = Offset(mapped.x, mapped.y))
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 72.dp)
                .padding(horizontal = 20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Align your eyes",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Place each eye exactly inside the yellow circles. " +
                            "When both eyes match, press Start to begin the full-face scan.",
                        color = Color.White.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 88.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (aligned || eyesAligned) {
                    "Eyes aligned — ready to start"
                } else {
                    "Move closer/farther until both cyan dots sit in the circles"
                },
                color = if (aligned || eyesAligned) Color(0xFF81C784) else Color.White,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onStart,
                enabled = aligned || eyesAligned,
            ) {
                Text("Start")
            }
            TextButton(onClick = onAbort) {
                Text("Cancel", color = Color.White)
            }
        }
    }
}
