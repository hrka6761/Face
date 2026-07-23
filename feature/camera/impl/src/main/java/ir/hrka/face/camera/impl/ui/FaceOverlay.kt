package ir.hrka.face.camera.impl.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hrka.face.camera.impl.TrackedFaceUi

/**
 * Draws face bounding boxes and per-face Save / identity labels.
 *
 * @param faces Tracked faces from the ViewModel.
 * @param imageWidth Analysis image width.
 * @param imageHeight Analysis image height.
 * @param mirrorX Mirror X for front camera.
 * @param onSaveClick Invoked when the user taps Save on an unknown face.
 * @param modifier Optional modifier.
 */
@Composable
fun FaceOverlay(
    faces: List<TrackedFaceUi>,
    imageWidth: Int,
    imageHeight: Int,
    mirrorX: Boolean,
    onSaveClick: (TrackedFaceUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewWidth by remember { mutableIntStateOf(0) }
    var viewHeight by remember { mutableIntStateOf(0) }

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
                val mapped = FaceCoordinateMapper.mapRect(
                    box = face.boundingBox,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    viewWidth = viewWidth.toFloat(),
                    viewHeight = viewHeight.toFloat(),
                    mirrorX = mirrorX,
                )

                val xPx = mapped.right.roundToIntSafe().coerceIn(0, (viewWidth - 8).coerceAtLeast(0))
                val yPx = mapped.top.roundToIntSafe().coerceAtLeast(0)

                Box(
                    modifier = Modifier.offset { IntOffset(xPx, yPx) },
                ) {
                    if (face.person != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                text = "${face.person.name}\n${face.person.id}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            )
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { onSaveClick(face) },
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

private fun Float.roundToIntSafe(): Int =
    if (isNaN()) 0 else toInt()
