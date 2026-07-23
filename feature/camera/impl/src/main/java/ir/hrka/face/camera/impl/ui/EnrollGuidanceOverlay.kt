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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hrka.face.camera.impl.EnrollPoseStep
import ir.hrka.face.camera.impl.TrackedFaceUi

/**
 * Fullscreen visual guidance while capturing front + profile enrollment samples.
 */
@Composable
fun EnrollGuidanceOverlay(
    step: EnrollPoseStep,
    faces: List<TrackedFaceUi>,
    imageWidth: Int,
    imageHeight: Int,
    mirrorX: Boolean,
    hint: String,
    stepProgress: Int,
    stepTarget: Int,
    overallProgress: Int,
    overallTarget: Int,
    poseAligned: Boolean,
    yawProgress: Float,
    onGuideAlignedChanged: (Boolean) -> Unit,
    onAbort: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewWidth by remember { mutableIntStateOf(0) }
    var viewHeight by remember { mutableIntStateOf(0) }
    val ovalFraction = 0.58f
    val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }

    val guideAligned = remember(
        face, imageWidth, imageHeight, mirrorX, viewWidth, viewHeight, step, poseAligned,
    ) {
        if (face == null || viewWidth <= 0 || imageWidth <= 0) {
            false
        } else {
            val geometryOk = when (step) {
                EnrollPoseStep.Front -> EyeGuideLayout.isFrontGuideAligned(
                    face, imageWidth, imageHeight,
                    viewWidth.toFloat(), viewHeight.toFloat(), mirrorX, ovalFraction,
                )
                EnrollPoseStep.LeftProfile, EnrollPoseStep.RightProfile ->
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

    val pulse by rememberInfiniteTransition(label = "enrollPulse").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
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
            drawArc(
                color = guideColor.copy(alpha = 0.9f),
                startAngle = -90f,
                sweepAngle = yawProgress.coerceIn(0f, 1f) * 360f,
                useCenter = false,
                topLeft = Offset(oval.left - 10f, oval.top - 10f),
                size = Size(oval.width() + 20f, oval.height() + 20f),
                style = Stroke(width = 10f, cap = StrokeCap.Round),
            )

            val leftTarget = EyeGuideLayout.leftEyeTarget(size.width, size.height, ovalFraction)
            val rightTarget = EyeGuideLayout.rightEyeTarget(size.width, size.height, ovalFraction)
            val radius = EyeGuideLayout.eyeRadius(size.width, size.height, ovalFraction)

            when (step) {
                EnrollPoseStep.Front -> {
                    drawEyeTargets(leftTarget, rightTarget, radius, guideColor)
                    face?.let { f ->
                        val (l, r) = EyeGuideLayout.mapEyes(
                            f, imageWidth, imageHeight, size.width, size.height, mirrorX,
                        )
                        l?.let { drawCircle(Color.Cyan, 7f, Offset(it.x, it.y)) }
                        r?.let { drawCircle(Color.Cyan, 7f, Offset(it.x, it.y)) }
                    }
                }
                EnrollPoseStep.LeftProfile, EnrollPoseStep.RightProfile -> {
                    // Voice "left" → chevron on the left pointing left; same for right.
                    val cueOnLeft = step == EnrollPoseStep.LeftProfile
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
                color = Color.Black.copy(alpha = 0.72f),
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (guideAligned) {
                            "Aligned — capturing…"
                        } else {
                            hint
                        },
                        color = if (guideAligned) Color(0xFF81C784) else Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "This pose: $stepProgress / $stepTarget",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (stepTarget <= 0) 0f
                            else (stepProgress.toFloat() / stepTarget).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Overall: $overallProgress / $overallTarget",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (overallTarget <= 0) 0f
                            else (overallProgress.toFloat() / overallTarget).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                }
            }

            TextButton(onClick = onAbort) {
                Text("Abort scan", color = Color.White)
            }
        }
    }
}

internal fun DrawScope.drawEyeTargets(
    left: Offset,
    right: Offset,
    radius: Float,
    color: Color,
) {
    listOf(left, right).forEach { center ->
        drawCircle(color.copy(alpha = 0.22f), radius, center)
        drawCircle(color, radius, center, style = Stroke(width = 3.5f))
        drawCircle(color, 4f, center)
    }
}

internal fun DrawScope.drawTurnArrow(
    center: Offset,
    pointingLeft: Boolean,
    color: Color,
) {
    val length = size.minDimension * 0.16f
    val shaftHalf = length * 0.08f
    val head = length * 0.42f
    val tipX = if (pointingLeft) center.x - length * 0.5f else center.x + length * 0.5f
    val baseX = if (pointingLeft) center.x + length * 0.15f else center.x - length * 0.15f
    val shaftEndX = if (pointingLeft) tipX + head else tipX - head

    // Shaft
    drawPath(
        path = Path().apply {
            moveTo(baseX, center.y - shaftHalf)
            lineTo(shaftEndX, center.y - shaftHalf)
            lineTo(shaftEndX, center.y + shaftHalf)
            lineTo(baseX, center.y + shaftHalf)
            close()
        },
        color = color,
    )
    // Head (tip faces the spoken turn direction)
    drawPath(
        path = Path().apply {
            moveTo(tipX, center.y)
            lineTo(shaftEndX, center.y - head * 0.75f)
            lineTo(shaftEndX, center.y + head * 0.75f)
            close()
        },
        color = color,
    )
}
