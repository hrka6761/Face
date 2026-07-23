# `:feature:camera:impl`

Fullscreen CameraX preview with ML Kit detection, MobileFaceNet identification,
and two operating modes: live recognition and guided registration.

## Contents

| Type | Role |
|------|------|
| [CameraScreen] | Fullscreen UI + mode switcher |
| [CameraViewModel] | Detect / embed / identify / guided enroll |
| [CameraBinder] | CameraX bind / switch / torch |
| [CameraMode] | Recognition vs Register |
| [EnrollPoseStep] / [EnrollPoseGate] | Front + left/right profile enrollment |
| [EnrollVoiceGuide] | Spoken pose guidance (TTS) |
| [EnrollQualityScorer] | Post-scan Bad / Good / Excellent grade |
| [FaceOverlay] | Per-face boxes + in-box identity labels |
| [RegisterOverlay] | Fixed guide frame + register / person details |
| [EnrollGuidanceOverlay] | Visual oval + turn arrows while scanning |
| [EnrollQualityDialog] / [EnrollDetailsDialog] | Quality review + name entry |
| [cameraEntry] | Navigation 3 entry |

## Modes

### Recognition
- Live multi-face detection and identification
- Known faces: green box with name + id **inside** the frame (font scales with box size)
- Unknown faces: red box only (no register action)
- Face count chip sits **4dp** under the mode switcher

### Register
1. Align face → **Register**
2. **Align eyes** on the marked circles → press **Start** (scan does not auto-begin)
3. Guided capture with visual oval/arrows + voice TTS (full face → left → right profile)
4. Press **Test Scan** (does not auto-start): place face at different distances/positions
5. Average match grade:
   - **Bad** → below 75% — must register & test again
   - **Good** → 75–90% — asked to repeat for better accuracy
   - **Excellent** → above 90% — enter person details → save to Room

## Matching

- Cosine threshold `0.68` + best-vs-second-best margin `0.08`
- Temporal vote window 7 / min 3 before showing an identity

[CameraScreen]: src/main/java/ir/hrka/face/camera/impl/ui/CameraScreen.kt
[CameraViewModel]: src/main/java/ir/hrka/face/camera/impl/CameraViewModel.kt
[CameraBinder]: src/main/java/ir/hrka/face/camera/impl/CameraBinder.kt
[CameraMode]: src/main/java/ir/hrka/face/camera/impl/CameraMode.kt
[EnrollPoseStep]: src/main/java/ir/hrka/face/camera/impl/CameraMode.kt
[EnrollPoseGate]: src/main/java/ir/hrka/face/camera/impl/EnrollPoseGate.kt
[EnrollVoiceGuide]: src/main/java/ir/hrka/face/camera/impl/EnrollVoiceGuide.kt
[EnrollQualityScorer]: src/main/java/ir/hrka/face/camera/impl/EnrollQualityScorer.kt
[FaceOverlay]: src/main/java/ir/hrka/face/camera/impl/ui/FaceOverlay.kt
[RegisterOverlay]: src/main/java/ir/hrka/face/camera/impl/ui/RegisterOverlay.kt
[EnrollGuidanceOverlay]: src/main/java/ir/hrka/face/camera/impl/ui/EnrollGuidanceOverlay.kt
[EnrollQualityDialog]: src/main/java/ir/hrka/face/camera/impl/ui/EnrollPersonDialog.kt
[EnrollDetailsDialog]: src/main/java/ir/hrka/face/camera/impl/ui/EnrollPersonDialog.kt
[cameraEntry]: src/main/java/ir/hrka/face/camera/impl/navigation/CameraEntryProvider.kt
