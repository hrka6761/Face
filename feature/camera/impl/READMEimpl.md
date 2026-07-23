# `:feature:camera:impl`

Fullscreen CameraX preview with ML Kit detection, MobileFaceNet identification,
face-count header, per-face Save buttons, and enroll dialog.

## Contents

| Type | Role |
|------|------|
| [CameraScreen] | Fullscreen UI |
| [CameraViewModel] | Detect / embed / identify / enroll |
| [CameraBinder] | CameraX bind / switch / torch |
| [FaceOverlay] | Boxes + Save / identity labels |
| [EnrollPersonDialog] | Name entry dialog |
| [cameraEntry] | Navigation 3 entry |

## UX

- Top: live face count
- Unknown face: **Save** button beside the box → dialog → Room enroll
- Known face: show `name` + unique `id` instead of Save

[CameraScreen]: src/main/java/ir/hrka/face/camera/impl/ui/CameraScreen.kt
[CameraViewModel]: src/main/java/ir/hrka/face/camera/impl/CameraViewModel.kt
[CameraBinder]: src/main/java/ir/hrka/face/camera/impl/CameraBinder.kt
[FaceOverlay]: src/main/java/ir/hrka/face/camera/impl/ui/FaceOverlay.kt
[EnrollPersonDialog]: src/main/java/ir/hrka/face/camera/impl/ui/EnrollPersonDialog.kt
[cameraEntry]: src/main/java/ir/hrka/face/camera/impl/navigation/CameraEntryProvider.kt
