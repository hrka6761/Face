# `:core:model`

Shared domain models for Face (no Android framework dependencies beyond `android.graphics.Rect`).

## Contents

| Type | Role |
|------|------|
| [Person] | Enrolled identity |
| [FaceEmbedding] | L2-normalized embedding vector |
| [DetectedFace] | Detector output (bounds, landmarks, pose) |
| [FaceLandmarkPoint] / [FaceLandmarkType] | Landmark geometry |
| [FaceMatchResult] / [EnrolledFace] | Matching helpers |

## Usage

```kotlin
api(projects.core.model)
```

[Person]: src/main/java/ir/hrka/face/model/Person.kt
[FaceEmbedding]: src/main/java/ir/hrka/face/model/FaceEmbedding.kt
[DetectedFace]: src/main/java/ir/hrka/face/model/DetectedFace.kt
[FaceLandmarkPoint]: src/main/java/ir/hrka/face/model/FaceLandmarkPoint.kt
[FaceLandmarkType]: src/main/java/ir/hrka/face/model/FaceLandmarkPoint.kt
[FaceMatchResult]: src/main/java/ir/hrka/face/model/FaceMatchResult.kt
[EnrolledFace]: src/main/java/ir/hrka/face/model/FaceMatchResult.kt
