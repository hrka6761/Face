# `:app`

APK shell for Face. Wires Hilt, theme, and Navigation 3 entry providers.

## Contents

| Type | Role |
|------|------|
| [FaceApplication] | `@HiltAndroidApp` |
| [MainActivity] | Single activity, edge-to-edge |
| [FaceApp] | `NavDisplay` + feature entries |
| Theme | Material 3 (`ui/theme`) |

## Dependencies

- `:core:navigation`, `:core:domain`, `:core:database`
- `:feature:splash:{api,impl}`
- `:feature:camera:{api,impl}`

[FaceApplication]: src/main/java/ir/hrka/face/FaceApplication.kt
[MainActivity]: src/main/java/ir/hrka/face/MainActivity.kt
[FaceApp]: src/main/java/ir/hrka/face/FaceApp.kt
