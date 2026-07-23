package ir.hrka.face.camera.impl.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hrka.face.camera.impl.EnrollTestStep
import ir.hrka.face.camera.impl.TrackedFaceUi

/**
 * Waiting screen after enrollment capture — Test Scan must be started manually.
 */
@Composable
fun EnrollReadyToTestOverlay(
    onTestScan: () -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 96.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Enrollment scan complete",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Next, run a Test Scan. Fit your face and eyes into the oval at " +
                            "different distances, then left and right profile. " +
                            "When aligned, the next step starts automatically.",
                        color = Color.White.copy(alpha = 0.95f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Button(onClick = onTestScan) {
                Text("Test Scan")
            }
            TextButton(onClick = onAbort) {
                Text("Cancel", color = Color.White)
            }
        }
    }
}

/**
 * Live Test Scan guidance with size-scaled face/eye ovals.
 * Advances when the face (and eyes, for front steps) fit the guide.
 */
@Composable
fun EnrollTestScanOverlay(
    step: EnrollTestStep,
    faces: List<TrackedFaceUi>,
    imageWidth: Int,
    imageHeight: Int,
    mirrorX: Boolean,
    hint: String,
    progress: Int,
    target: Int,
    poseAligned: Boolean,
    onGuideAlignedChanged: (Boolean) -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewWidth by remember { mutableIntStateOf(0) }
    var viewHeight by remember { mutableIntStateOf(0) }
    val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
    val ovalFraction = step.ovalFraction

    val guideAligned = remember(
        face, imageWidth, imageHeight, mirrorX, viewWidth, viewHeight, step, poseAligned,
    ) {
        if (face == null || viewWidth <= 0 || imageWidth <= 0) {
            false
        } else {
            val geometryOk = if (step.requiresEyes) {
                EyeGuideLayout.isFrontGuideAligned(
                    face, imageWidth, imageHeight,
                    viewWidth.toFloat(), viewHeight.toFloat(), mirrorX, ovalFraction,
                )
            } else {
                EyeGuideLayout.isProfileGuideAligned(
                    face, imageWidth, imageHeight,
                    viewWidth.toFloat(), viewHeight.toFloat(), mirrorX, ovalFraction,
                )
            }
            geometryOk && poseAligned
        }
    }

    LaunchedEffect(guideAligned) {
        onGuideAlignedChanged(guideAligned)
    }

    val pulse by rememberInfiniteTransition(label = "testPulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "testPulseAlpha",
    )
    val guideColor = if (guideAligned) Color(0xFF4CAF50) else Color(0xFFFFC107)
    val arrowAlpha = if (guideAligned) 0.2f else pulse

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                viewWidth = it.width
                viewHeight = it.height
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (viewWidth <= 0) return@Canvas
            val oval = EyeGuideLayout.ovalRect(size.width, size.height, ovalFraction)
            drawRect(Color.Black.copy(alpha = 0.42f))
            drawOval(
                color = guideColor,
                topLeft = Offset(oval.left, oval.top),
                size = Size(oval.width(), oval.height()),
                style = Stroke(width = 6f),
            )
            if (guideAligned) {
                drawArc(
                    color = guideColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(oval.left - 8f, oval.top - 8f),
                    size = Size(oval.width() + 16f, oval.height() + 16f),
                    style = Stroke(width = 8f, cap = StrokeCap.Round),
                )
            }

            val leftTarget = EyeGuideLayout.leftEyeTarget(size.width, size.height, ovalFraction)
            val rightTarget = EyeGuideLayout.rightEyeTarget(size.width, size.height, ovalFraction)
            val radius = EyeGuideLayout.eyeRadius(size.width, size.height, ovalFraction)

            when (step) {
                EnrollTestStep.FrontClose,
                EnrollTestStep.FrontMedium,
                EnrollTestStep.FrontFar,
                -> {
                    drawEyeTargets(leftTarget, rightTarget, radius, guideColor)
                    face?.let { f ->
                        val (l, r) = EyeGuideLayout.mapEyes(
                            f, imageWidth, imageHeight, size.width, size.height, mirrorX,
                        )
                        l?.let { drawCircle(Color.Cyan, 7f, Offset(it.x, it.y)) }
                        r?.let { drawCircle(Color.Cyan, 7f, Offset(it.x, it.y)) }
                    }
                }

                EnrollTestStep.LeftProfile, EnrollTestStep.RightProfile -> {
                    // Voice "left" → chevron on the left pointing left; same for right.
                    val cueOnLeft = step == EnrollTestStep.LeftProfile
                    drawTurnArrow(
                        center = Offset(
                            x = if (cueOnLeft) {
                                oval.left + oval.width() * 0.14f
                            } else {
                                oval.right - oval.width() * 0.14f
                            },
                            y = oval.centerY(),
                        ),
                        pointingLeft = cueOnLeft,
                        color = guideColor.copy(alpha = arrowAlpha),
                    )
                }
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
                        text = step.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = step.instruction,
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
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.78f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (guideAligned) {
                            "Aligned — capturing, then next step…"
                        } else {
                            hint
                        },
                        color = if (guideAligned) Color(0xFF81C784) else Color.White,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Test samples: $progress / $target",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (target <= 0) 0f
                            else (progress.toFloat() / target).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }
            }
            TextButton(onClick = onAbort) {
                Text("Abort test", color = Color.White)
            }
        }
    }
}
