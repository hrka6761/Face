# face_recognition_engine

Self-contained offline face recognition (InsightFace buffalo_l + ONNX Runtime).

**Models are not packaged in the APK.** The host app downloads them to device storage
and passes absolute file paths into the engine.

## Quick start

```kotlin
val models = ModelPaths(
    embeddingModelPath = File(filesDir, "models/arcface_w600k_r50.onnx").absolutePath,
    // detectorModelPath optional — omit when detection is done elsewhere (e.g. ML Kit)
)

val engine = FaceRecognitionEngine.create(context, models)
try {
    val template = engine.embed(enrollBitmap).copy(personId = "alice")
    val result = engine.identify(probeBitmap, listOf(template))
} finally {
    engine.close()
}
```

## Required on-device models

| Role | Suggested filename | buffalo_l source |
|---|---|---|
| Detection + 5 landmarks | `scrfd_10g_kps.onnx` | `det_10g.onnx` |
| Embedding (512-D) | `arcface_w600k_r50.onnx` | `w600k_r50.onnx` |

Download buffalo_l from InsightFace model zoo / HuggingFace `public-data/insightface`,
place files somewhere readable by the app (e.g. `context.filesDir`), then pass paths.

## Public API

| API | Purpose |
|---|---|
| `create(context, models)` | Open engine with on-disk model paths |
| `embed(image)` | Detect primary face → align → 512-D embedding |
| `identify(image, gallery)` | Embed + 1:N cosine match |
| `similarity(a, b)` | Cosine score between two embeddings |

## Gradle

```kotlin
implementation(projects.core.faceRecognitionEngine)
```
