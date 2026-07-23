# `:core:face_detection`

Standalone Android library for **on-device face detection** via Google ML Kit.
No Face-app UI dependencies — drop it into any CameraX / bitmap pipeline.

## Package layout

```
ir.hrka.face.detection/
├── api/                      Public façade
│   ├── FaceDetectorEngine
│   ├── FaceDetectorFactory
│   ├── FaceDetectorOptions
│   └── FaceDetectionResult
└── internal/
    └── MlKitFaceDetectorEngine
```

## Setup (host app)

```kotlin
// settings.gradle.kts
include(":core:face_detection")

// module build.gradle.kts
implementation(projects.core.faceDetection)
```

## Usage

```kotlin
val detector = FaceDetectorFactory.create(
    FaceDetectorOptions(
        performanceMode = FaceDetectorOptions.PerformanceMode.ACCURATE,
        landmarkMode = FaceDetectorOptions.LandmarkMode.ALL,
        enableTracking = true,
    ),
)

try {
    when (val result = detector.detect(imageProxy)) {
        is FaceDetectionResult.Success -> result.faces
        is FaceDetectionResult.Failure -> error(result.message)
    }
} finally {
    imageProxy.close()
    detector.close()
}
```

## Notes

- Callers own `ImageProxy.close()`.
- Landmarks are enabled by default so recognition modules can align faces.
- Returns domain models from `:core:model`.

*Last updated: initial reusable ML Kit detector module.*
