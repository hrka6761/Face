package ir.hrka.face.detection.internal

import android.graphics.Bitmap
import android.media.Image
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.face.FaceDetectorOptions as MlKitOptions
import ir.hrka.face.detection.api.FaceDetectionResult
import ir.hrka.face.detection.api.FaceDetectorEngine
import ir.hrka.face.detection.api.FaceDetectorOptions
import ir.hrka.face.model.DetectedFace
import ir.hrka.face.model.FaceLandmarkPoint
import ir.hrka.face.model.FaceLandmarkType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * ML Kit implementation of [FaceDetectorEngine].
 *
 * Live camera frames should use [detect] with [ImageProxy] so ML Kit receives
 * `InputImage.fromMediaImage` (YUV_420_888), which is the supported camera path.
 *
 * @param options Public detector options mapped to ML Kit.
 */
internal class MlKitFaceDetectorEngine(
    options: FaceDetectorOptions,
) : FaceDetectorEngine {

    private val detector = FaceDetection.getClient(options.toMlKitOptions())

    override suspend fun detect(imageProxy: ImageProxy): FaceDetectionResult {
        val mediaImage = imageProxy.image
            ?: return FaceDetectionResult.Failure(
                "ImageProxy.image is null (format=${imageProxy.format}). " +
                    "Use YUV_420_888 analysis frames for ML Kit.",
            )

        return detectMediaImage(
            image = mediaImage,
            rotationDegrees = imageProxy.imageInfo.rotationDegrees,
            sensorWidth = imageProxy.width,
            sensorHeight = imageProxy.height,
        )
    }

    override suspend fun detect(image: Image, rotationDegrees: Int): FaceDetectionResult =
        detectMediaImage(
            image = image,
            rotationDegrees = rotationDegrees,
            sensorWidth = image.width,
            sensorHeight = image.height,
        )

    override suspend fun detect(bitmap: Bitmap, rotationDegrees: Int): FaceDetectionResult {
        return try {
            val input = InputImage.fromBitmap(bitmap, rotationDegrees)
            val faces = detector.process(input).await()
            // fromBitmap applies rotationDegrees; boxes are in upright space.
            val (uprightW, uprightH) = uprightSize(
                width = bitmap.width,
                height = bitmap.height,
                rotationDegrees = rotationDegrees,
            )
            FaceDetectionResult.Success(
                faces = faces.mapIndexed { index, face -> face.toDomain(index) },
                imageWidth = uprightW,
                imageHeight = uprightH,
                rotationDegrees = rotationDegrees,
            )
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            FaceDetectionResult.Failure(
                message = t.message ?: "Face detection failed",
                cause = t,
            )
        }
    }

    private suspend fun detectMediaImage(
        image: Image,
        rotationDegrees: Int,
        sensorWidth: Int,
        sensorHeight: Int,
    ): FaceDetectionResult {
        return try {
            val input = InputImage.fromMediaImage(image, rotationDegrees)
            val faces = detector.process(input).await()
            // ML Kit returns boxes in the upright (rotated) coordinate space.
            val (uprightW, uprightH) = uprightSize(
                width = sensorWidth,
                height = sensorHeight,
                rotationDegrees = rotationDegrees,
            )
            FaceDetectionResult.Success(
                faces = faces.mapIndexed { index, face -> face.toDomain(index) },
                imageWidth = uprightW,
                imageHeight = uprightH,
                rotationDegrees = rotationDegrees,
            )
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            if (t.isCancellationLike()) throw CancellationException(t.message, t)
            FaceDetectionResult.Failure(
                message = t.message ?: "Face detection failed",
                cause = t,
            )
        }
    }

    override fun close() {
        detector.close()
    }
}

/**
 * Sensor / bitmap size mapped into the upright space ML Kit uses for bounding boxes.
 */
private fun uprightSize(width: Int, height: Int, rotationDegrees: Int): Pair<Int, Int> {
    return if (rotationDegrees % 180 == 0) {
        width to height
    } else {
        height to width
    }
}

/**
 * Returns `true` when [Throwable] represents a cancelled ML Kit / Tasks operation.
 */
private fun Throwable.isCancellationLike(): Boolean {
    val message = message.orEmpty()
    return message.contains("Task was cancelled", ignoreCase = true) ||
        message.contains("Job was cancelled", ignoreCase = true) ||
        this is java.util.concurrent.CancellationException
}

/**
 * Maps an ML Kit [Face] to the shared domain model.
 *
 * @param fallbackIndex Used when ML Kit does not provide a tracking id.
 */
private fun Face.toDomain(fallbackIndex: Int): DetectedFace {
    val landmarks = buildMap {
        putIfPresent(this@toDomain, FaceLandmark.LEFT_EYE, FaceLandmarkType.LEFT_EYE)
        putIfPresent(this@toDomain, FaceLandmark.RIGHT_EYE, FaceLandmarkType.RIGHT_EYE)
        putIfPresent(this@toDomain, FaceLandmark.NOSE_BASE, FaceLandmarkType.NOSE_BASE)
        putIfPresent(this@toDomain, FaceLandmark.MOUTH_LEFT, FaceLandmarkType.MOUTH_LEFT)
        putIfPresent(this@toDomain, FaceLandmark.MOUTH_RIGHT, FaceLandmarkType.MOUTH_RIGHT)
    }

    return DetectedFace(
        trackingId = trackingId ?: fallbackIndex,
        boundingBox = boundingBox,
        landmarks = landmarks,
        headEulerAngleX = headEulerAngleX,
        headEulerAngleY = headEulerAngleY,
        headEulerAngleZ = headEulerAngleZ,
    )
}

private fun MutableMap<FaceLandmarkType, FaceLandmarkPoint>.putIfPresent(
    face: Face,
    mlKitType: Int,
    type: FaceLandmarkType,
) {
    val landmark = face.getLandmark(mlKitType) ?: return
    put(type, FaceLandmarkPoint(landmark.position.x, landmark.position.y))
}

/**
 * Converts public [FaceDetectorOptions] into ML Kit options.
 */
private fun FaceDetectorOptions.toMlKitOptions(): MlKitOptions {
    val builder = MlKitOptions.Builder()
        .setPerformanceMode(
            when (performanceMode) {
                FaceDetectorOptions.PerformanceMode.ACCURATE -> MlKitOptions.PERFORMANCE_MODE_ACCURATE
                FaceDetectorOptions.PerformanceMode.FAST -> MlKitOptions.PERFORMANCE_MODE_FAST
            },
        )
        .setLandmarkMode(
            when (landmarkMode) {
                FaceDetectorOptions.LandmarkMode.NONE -> MlKitOptions.LANDMARK_MODE_NONE
                FaceDetectorOptions.LandmarkMode.ALL -> MlKitOptions.LANDMARK_MODE_ALL
            },
        )
        .setContourMode(MlKitOptions.CONTOUR_MODE_NONE)
        .setClassificationMode(MlKitOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(minFaceSize)

    if (enableTracking) {
        builder.enableTracking()
    }
    return builder.build()
}
