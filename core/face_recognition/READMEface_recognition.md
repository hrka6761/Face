# `:core:face_recognition`

Standalone Android library for **on-device face embedding and matching** using
**MobileFaceNet** (`mobile_face_net.tflite`, 112×112 → 192-d).

## Package layout

```
ir.hrka.face.recognition/
├── api/
│   ├── FaceEmbedder
│   ├── FaceMatcher
│   ├── FaceRecognitionFactory
│   ├── FaceRecognitionConfig
│   └── FaceRecognitionException
└── internal/
    ├── MobileFaceNetEmbedder
    ├── CosineFaceMatcher
    ├── FaceAligner
    ├── EmbeddingMath
    └── ImageConversion
```

## Setup (host app)

```kotlin
implementation(projects.core.faceRecognition)
```

The TFLite model is bundled in this module’s `assets/`.

## Usage

```kotlin
val embedder = FaceRecognitionFactory.createEmbedder(context)
val matcher = FaceRecognitionFactory.createMatcher()

val embeddings = embedder.embedAll(uprightBitmap, detectedFaces)
val match = matcher.match(embeddings.values.first(), enrolledFaces, threshold = 0.85f)

embedder.close()
```

## Pipeline details

- Correct camera rotation via [ImageConversion].
- **ArcFace-style eye alignment** drawn into an exact 112×112 canvas (scale/rotation normalized).
- **Multi-scale crops** (`0.85× / 1.0× / 1.2×`) averaged into a robust embedding.
- Fresh output buffer per inference; L2-normalized embeddings.
- Cosine similarity with **max-over-gallery** matching (threshold default `0.55`).
- Host apps should enroll **multiple templates** (this app collects ~12 over ~2s).

*Last updated: distance-robust multi-scale alignment + multi-template matching.*
