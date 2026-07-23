package ir.hrka.face.camera.impl.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hrka.face.camera.impl.TrackedFaceUi

/**
 * Draws face bounding boxes for recognition mode.
 *
 * Known faces show identity details **inside** the box; font size scales with the box.
 * Unknown faces are outlined in red with no save action.
 *
 * @param faces Tracked faces from the ViewModel.
 * @param imageWidth Analysis image width.
 * @param imageHeight Analysis image height.
 * @param mirrorX Mirror X for front camera.
 * @param modifier Optional modifier.
 */
@Composable
fun FaceOverlay(
    faces: List<TrackedFaceUi>,
    imageWidth: Int,
    imageHeight: Int,
    mirrorX: Boolean,
    modifier: Modifier = Modifier,
) {
    var viewWidth by remember { mutableIntStateOf(0) }
    var viewHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged {
                viewWidth = it.width
                viewHeight = it.height
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            faces.forEach { face ->
                val mapped = FaceCoordinateMapper.mapRect(
                    box = face.boundingBox,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    viewWidth = size.width,
                    viewHeight = size.height,
                    mirrorX = mirrorX,
                )
                drawRect(
                    color = if (face.person != null) Color(0xFF4CAF50) else Color(0xFFFF5252),
                    topLeft = Offset(mapped.left, mapped.top),
                    size = Size(mapped.width(), mapped.height()),
                    style = Stroke(width = 3f),
                )
            }
        }

        if (viewWidth > 0 && viewHeight > 0) {
            faces.forEach { face ->
                val person = face.person ?: return@forEach
                val mapped = FaceCoordinateMapper.mapRect(
                    box = face.boundingBox,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    viewWidth = viewWidth.toFloat(),
                    viewHeight = viewHeight.toFloat(),
                    mirrorX = mirrorX,
                )

                val boxWidthPx = mapped.width().coerceAtLeast(1f)
                val boxHeightPx = mapped.height().coerceAtLeast(1f)
                val fontSizeSp = (minOf(boxWidthPx, boxHeightPx) * 0.09f / density.density)
                    .coerceIn(8f, 22f)

                val xPx = mapped.left.roundToIntSafe()
                val yPx = mapped.top.roundToIntSafe()
                val widthDp = with(density) { boxWidthPx.toDp() }
                val heightDp = with(density) { boxHeightPx.toDp() }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPx, yPx) }
                        .size(widthDp, heightDp)
                        .padding(4.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = "${person.name}\n${person.id.take(8)}",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.15f).sp,
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun Float.roundToIntSafe(): Int =
    if (isNaN()) 0 else toInt()
