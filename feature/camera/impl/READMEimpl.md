# `:feature:camera:impl`

Fullscreen CameraX preview with ML Kit detection, MobileFaceNet identification,
and two operating modes: live recognition and attendance registration.

## Contents

| Type | Role |
|------|------|
| [CameraScreen] | Fullscreen UI + mode switcher |
| [CameraViewModel] | Detect / embed / identify / enroll |
| [CameraBinder] | CameraX bind / switch / torch |
| [CameraMode] | Recognition vs Attendance |
| [FaceOverlay] | Per-face boxes + in-box identity labels |
| [AttendanceOverlay] | Fixed guide frame + register / person details |
| [EnrollPersonDialog] | Name entry dialog (attendance only) |
| [cameraEntry] | Navigation 3 entry |

## Modes

### Recognition
- Live multi-face detection and identification
- Known faces: green box with name + id **inside** the frame (font scales with box size)
- Unknown faces: red box only (no register action)

### Attendance
- Fixed center guide frame (kiosk / time-clock style)
- Face aligned + known → green frame + person details under the guide
- Face aligned + unknown → red frame + **Register** button → dialog → Room enroll
- No face in guide → red frame + “Place your face in the frame”

[CameraScreen]: src/main/java/ir/hrka/face/camera/impl/ui/CameraScreen.kt
[CameraViewModel]: src/main/java/ir/hrka/face/camera/impl/CameraViewModel.kt
[CameraBinder]: src/main/java/ir/hrka/face/camera/impl/CameraBinder.kt
[CameraMode]: src/main/java/ir/hrka/face/camera/impl/CameraMode.kt
[FaceOverlay]: src/main/java/ir/hrka/face/camera/impl/ui/FaceOverlay.kt
[AttendanceOverlay]: src/main/java/ir/hrka/face/camera/impl/ui/AttendanceOverlay.kt
[EnrollPersonDialog]: src/main/java/ir/hrka/face/camera/impl/ui/EnrollPersonDialog.kt
[cameraEntry]: src/main/java/ir/hrka/face/camera/impl/navigation/CameraEntryProvider.kt
