package ir.hrka.face.camera.impl.ui

import android.graphics.RectF
import android.os.SystemClock
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.hrka.face.camera.impl.TrackedFaceUi
import ir.hrka.face.model.Person
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * Stable attendance readout shown under the guide frame.
 *
 * @property trackingId Detector tracking id when a live face is aligned.
 * @property person Matched identity when known.
 * @property similarityPercent Match score rounded to whole percent.
 * @property registerFace Live unknown face used for registration, if any.
 */
private data class AttendanceDisplay(
    val trackingId: Int,
    val person: Person?,
    val similarityPercent: Int,
    val registerFace: TrackedFaceUi?,
)

/**
 * Attendance-mode overlay: fixed center guide frame, register action, and person details.
 *
 * - Red frame when no face is aligned, or the aligned face is unknown.
 * - Green frame when a known enrolled person is aligned in the guide.
 * - Register button appears for an unknown face in the guide.
 * - Known person details are shown below the guide.
 *
 * Display state is held briefly across missed frames so person info does not flash.
 *
 * @param faces Tracked faces from the ViewModel.
 * @param imageWidth Analysis image width.
 * @param imageHeight Analysis image height.
 * @param mirrorX Mirror X for front camera.
 * @param onRegisterClick Invoked when the user taps Register for an unknown face in the guide.
 * @param modifier Optional modifier.
 */
@Composable
fun AttendanceOverlay(
    faces: List<TrackedFaceUi>,
    imageWidth: Int,
    imageHeight: Int,
    mirrorX: Boolean,
    onRegisterClick: (TrackedFaceUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewWidth by remember { mutableIntStateOf(0) }
    var viewHeight by remember { mutableIntStateOf(0) }

    val guide = remember(viewWidth, viewHeight) {
        if (viewWidth <= 0 || viewHeight <= 0) {
            RectF()
        } else {
            AttendanceGuide.guideRectInView(viewWidth.toFloat(), viewHeight.toFloat())
        }
    }

    val liveAligned = remember(faces, imageWidth, imageHeight, mirrorX, viewWidth, viewHeight, guide) {
        if (viewWidth <= 0 || viewHeight <= 0 || guide.isEmpty) {
            null
        } else {
            AttendanceGuide.findAlignedFace(
                faces = faces,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                viewWidth = viewWidth.toFloat(),
                viewHeight = viewHeight.toFloat(),
                mirrorX = mirrorX,
                guide = guide,
            )
        }
    }

    var display by remember { mutableStateOf<AttendanceDisplay?>(null) }
    val holdClock = remember { HoldClock() }

    SideEffect {
        val aligned = liveAligned
        if (aligned != null) {
            holdClock.lastGoodAtElapsedMs = SystemClock.elapsedRealtime()
            val merged = mergeAttendanceDisplay(previous = display, live = aligned)
            if (merged != display) {
                display = merged
            }
        }
    }

    LaunchedEffect(liveAligned == null) {
        if (liveAligned != null) return@LaunchedEffect
        while (display != null) {
            val remaining = HOLD_MS - (SystemClock.elapsedRealtime() - holdClock.lastGoodAtElapsedMs)
            if (remaining <= 0L) {
                display = null
                break
            }
            delay(remaining)
        }
    }

    val frameColor = when {
        display?.person != null -> Color(0xFF4CAF50)
        else -> Color(0xFFFF5252)
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
            if (guide.isEmpty) return@Canvas
            drawRoundRect(
                color = frameColor,
                topLeft = Offset(guide.left, guide.top),
                size = Size(guide.width(), guide.height()),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 6f),
            )
            // Corner accents for a kiosk-style guide.
            val accent = minOf(guide.width(), guide.height()) * 0.18f
            val stroke = 8f
            // Top-left
            drawLine(frameColor, Offset(guide.left, guide.top), Offset(guide.left + accent, guide.top), stroke)
            drawLine(frameColor, Offset(guide.left, guide.top), Offset(guide.left, guide.top + accent), stroke)
            // Top-right
            drawLine(frameColor, Offset(guide.right, guide.top), Offset(guide.right - accent, guide.top), stroke)
            drawLine(frameColor, Offset(guide.right, guide.top), Offset(guide.right, guide.top + accent), stroke)
            // Bottom-left
            drawLine(frameColor, Offset(guide.left, guide.bottom), Offset(guide.left + accent, guide.bottom), stroke)
            drawLine(frameColor, Offset(guide.left, guide.bottom), Offset(guide.left, guide.bottom - accent), stroke)
            // Bottom-right
            drawLine(frameColor, Offset(guide.right, guide.bottom), Offset(guide.right - accent, guide.bottom), stroke)
            drawLine(frameColor, Offset(guide.right, guide.bottom), Offset(guide.right, guide.bottom - accent), stroke)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val current = display
            when {
                current?.person != null -> {
                    PersonDetailsCard(
                        person = current.person,
                        similarityPercent = current.similarityPercent,
                    )
                }

                current != null -> {
                    Text(
                        text = "Face not registered",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = {
                            val face = current.registerFace
                                ?: liveAligned?.takeIf { it.person == null }
                                ?: return@Button
                            onRegisterClick(face)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Register")
                    }
                }

                else -> {
                    Text(
                        text = "Place your face in the frame",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Merges a live aligned face into sticky display state without flickering.
 *
 * - Keeps a known person if the same track briefly reports unknown.
 * - Only refreshes match % when it changes by at least 1 point.
 */
private fun mergeAttendanceDisplay(
    previous: AttendanceDisplay?,
    live: TrackedFaceUi,
): AttendanceDisplay {
    val livePercent = (live.similarity * 100f).toInt().coerceIn(0, 100)

    // Same track briefly lost identity — keep showing the known person.
    if (previous?.person != null &&
        live.person == null &&
        previous.trackingId == live.trackingId
    ) {
        return previous
    }

    // Same known person — avoid rewriting the card every frame.
    if (previous?.person != null &&
        live.person != null &&
        previous.person.id == live.person.id &&
        abs(previous.similarityPercent - livePercent) < 1
    ) {
        return previous
    }

    return AttendanceDisplay(
        trackingId = live.trackingId,
        person = live.person,
        similarityPercent = livePercent,
        registerFace = if (live.person == null) live else null,
    )
}

@Composable
private fun PersonDetailsCard(
    person: Person,
    similarityPercent: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.72f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = person.name,
                color = Color(0xFF4CAF50),
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            DetailRow(label = "ID", value = person.id)
            DetailRow(label = "Enrolled", value = formatEpoch(person.createdAt))
            DetailRow(label = "Updated", value = formatEpoch(person.updatedAt))
            if (similarityPercent > 0) {
                DetailRow(label = "Match", value = "$similarityPercent%")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Text(
        text = "$label: $value",
        color = Color.White.copy(alpha = 0.92f),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

private fun formatEpoch(epochMs: Long): String {
    if (epochMs <= 0L) return "—"
    return DATE_FORMAT.format(Date(epochMs))
}

private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

private const val HOLD_MS = 700L

private class HoldClock {
    var lastGoodAtElapsedMs: Long = 0L
}

/**
 * Geometry helpers for the attendance guide frame.
 */
object AttendanceGuide {
    /** Guide size as a fraction of the shorter view side. */
    const val GUIDE_FRACTION = 0.58f

    /**
     * Builds a centered square guide rectangle in view coordinates.
     */
    fun guideRectInView(viewWidth: Float, viewHeight: Float): RectF {
        val side = minOf(viewWidth, viewHeight) * GUIDE_FRACTION
        val left = (viewWidth - side) / 2f
        val top = (viewHeight - side) / 2f
        return RectF(left, top, left + side, top + side)
    }

    /**
     * Picks the face whose mapped bounds best align with [guide], or `null` if none qualify.
     */
    fun findAlignedFace(
        faces: List<TrackedFaceUi>,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Float,
        viewHeight: Float,
        mirrorX: Boolean,
        guide: RectF,
    ): TrackedFaceUi? {
        if (faces.isEmpty() || guide.isEmpty) return null

        var best: TrackedFaceUi? = null
        var bestScore = Float.NEGATIVE_INFINITY

        faces.forEach { face ->
            val mapped = FaceCoordinateMapper.mapRect(
                box = face.boundingBox,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                viewWidth = viewWidth,
                viewHeight = viewHeight,
                mirrorX = mirrorX,
            )
            if (!isAligned(mapped, guide)) return@forEach

            val cx = mapped.centerX()
            val cy = mapped.centerY()
            val dx = (cx - guide.centerX()) / guide.width()
            val dy = (cy - guide.centerY()) / guide.height()
            val centrality = 1f - (dx * dx + dy * dy)
            val coverage = (mapped.width() * mapped.height()) /
                (guide.width() * guide.height()).coerceAtLeast(1f)
            val score = centrality + coverage.coerceIn(0f, 1.5f) * 0.35f
            if (score > bestScore) {
                bestScore = score
                best = face
            }
        }
        return best
    }

    /**
     * A face is aligned when its center is inside a slightly expanded guide and it covers
     * enough of the guide. Expansion reduces flicker when the box jitters near the border.
     */
    fun isAligned(faceInView: RectF, guide: RectF): Boolean {
        if (faceInView.isEmpty || guide.isEmpty) return false
        val padX = guide.width() * 0.08f
        val padY = guide.height() * 0.08f
        val loose = RectF(
            guide.left - padX,
            guide.top - padY,
            guide.right + padX,
            guide.bottom + padY,
        )
        val centerInside = loose.contains(faceInView.centerX(), faceInView.centerY())
        if (!centerInside) return false
        val faceArea = faceInView.width() * faceInView.height()
        val guideArea = guide.width() * guide.height()
        return faceArea >= guideArea * 0.10f
    }
}
