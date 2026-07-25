package ir.hrka.face.engine

import android.graphics.PointF
import ir.hrka.face.engine.detection.Scrfd10GFaceDetector
import ir.hrka.face.engine.model.BoundingBox
import ir.hrka.face.engine.model.DetectedFace
import ir.hrka.face.engine.model.FaceLandmarks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure unit tests for SCRFD decode helpers (anchors / NMS / IoU).
 *
 * TODO(model): add instrumented tests that run `scrfd_10g_kps.onnx` on a sample image
 * once a detector model file is available on device storage via ModelPaths.
 */
class ScrfdFaceDetectorTest {

    @Test
    fun buildAnchorCenters_matchesStrideGrid() {
        val anchors = Scrfd10GFaceDetector.buildAnchorCenters(
            height = 2,
            width = 2,
            stride = 8,
            numAnchors = 1,
        )
        // Centers at (4,4), (12,4), (4,12), (12,12)
        assertEquals(8, anchors.size)
        assertEquals(4f, anchors[0], 0f)
        assertEquals(4f, anchors[1], 0f)
        assertEquals(12f, anchors[2], 0f)
        assertEquals(4f, anchors[3], 0f)
    }

    @Test
    fun distanceToBbox_decodesRelativeDistances() {
        val box = Scrfd10GFaceDetector.distanceToBbox(
            anchorX = 100f,
            anchorY = 100f,
            distLeft = 10f,
            distTop = 20f,
            distRight = 30f,
            distBottom = 40f,
        )
        assertEquals(90f, box.left, 0f)
        assertEquals(80f, box.top, 0f)
        assertEquals(130f, box.right, 0f)
        assertEquals(140f, box.bottom, 0f)
    }

    @Test
    fun nms_suppressesOverlappingLowerScore() {
        val high = face(box = BoundingBox(0f, 0f, 100f, 100f), score = 0.9f)
        val overlap = face(box = BoundingBox(10f, 10f, 110f, 110f), score = 0.8f)
        val far = face(box = BoundingBox(300f, 300f, 400f, 400f), score = 0.7f)

        val kept = Scrfd10GFaceDetector.nms(listOf(overlap, high, far), iouThreshold = 0.3f)
        assertEquals(2, kept.size)
        assertEquals(0.9f, kept[0].confidenceScore, 0f)
        assertTrue(kept.any { it.confidenceScore == 0.7f })
    }

    @Test
    fun iou_ofIdenticalBoxes_isOne() {
        val box = BoundingBox(0f, 0f, 50f, 50f)
        assertEquals(1f, Scrfd10GFaceDetector.iou(box, box), 1e-5f)
    }

    private fun face(box: BoundingBox, score: Float): DetectedFace {
        val p = PointF(box.left, box.top)
        return DetectedFace(
            boundingBox = box,
            confidenceScore = score,
            landmarks = FaceLandmarks(p, p, p, p, p),
        )
    }
}
