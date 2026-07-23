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
| [FaceOverlay] | Per-face boxes + in-box identity labels |
| [RegisterOverlay] | Fixed guide frame + register / person details |
| [EnrollPersonDialog] | Guided multi-pose name entry dialog |
| [cameraEntry] | Navigation 3 entry |

## Modes

### Recognition
- Live multi-face detection and identification
- Known faces: green box with name + id **inside** the frame (font scales with box size)
- Unknown faces: red box only (no register action)
- Face count chip sits **4dp** under the mode switcher

### Register
- Fixed center guide frame
- Face aligned + known → green frame + person details
- Face aligned + unknown → red frame + **Register** → guided enroll:
  1. Full face
  2. Left profile
  3. Right profile
- Samples are accepted only when head yaw matches the active step

## Matching

- Cosine threshold `0.68` + best-vs-second-best margin `0.08`
- Temporal vote window 7 / min 3 before showing an identity

[CameraScreen]: src/main/java/ir/hrka/face/camera/impl/ui/CameraScreen.kt
[CameraViewModel]: src/main/java/ir/hrka/face/camera/impl/CameraViewModel.kt
[CameraBinder]: src/main/java/ir/hrka/face/camera/impl/CameraBinder.kt
[CameraMode]: src/main/java/ir/hrka/face/camera/impl/CameraMode.kt
[EnrollPoseStep]: src/main/java/ir/hrka/face/camera/impl/CameraMode.kt
[EnrollPoseGate]: src/main/java/ir/hrka/face/camera/impl/EnrollPoseGate.kt
[FaceOverlay]: src/main/java/ir/hrka/face/camera/impl/ui/FaceOverlay.kt
[RegisterOverlay]: src/main/java/ir/hrka/face/camera/impl/ui/RegisterOverlay.kt
[EnrollPersonDialog]: src/main/java/ir/hrka/face/camera/impl/ui/EnrollPersonDialog.kt
[cameraEntry]: src/main/java/ir/hrka/face/camera/impl/navigation/CameraEntryProvider.kt
