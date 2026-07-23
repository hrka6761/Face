# Face

Android app for **on-device face detection and identification** from the mobile camera.

- **Detection:** Google ML Kit Face Detection (`:core:face_detection`)
- **Identification:** MobileFaceNet TFLite embeddings (`:core:face_recognition`)
- **Persistence:** Room identities (`:core:database` / `:core:data`)

Architecture follows the same modular layout as [Hooshmand](../Hooshmand): `:app`, `core/*`, `feature/*/api|impl`, and convention plugins in `build-logic`.

## Modules

| Module | Role |
|--------|------|
| `:app` | APK shell, Hilt, Navigation 3 wiring |
| `:core:navigation` | Navigation 3 state / navigator |
| `:core:model` | Shared domain models |
| `:core:database` | Room entities / DAOs |
| `:core:data` | Repositories |
| `:core:domain` | Use cases |
| `:core:face_detection` | Reusable ML Kit detector |
| `:core:face_recognition` | Reusable MobileFaceNet embedder / matcher |
| `:feature:splash:{api,impl}` | CAMERA permission gate |
| `:feature:camera:{api,impl}` | Fullscreen camera: recognition + register modes |

## Build

```bash
./gradlew :app:assembleBetaDebug
```

## Development rules

1. Update each module’s `README*.md` when its public API or role changes.
2. Every public type and function must have KDoc.
3. Core reusable modules (`face_detection`, `face_recognition`, `database`) must not depend on `:app` or `feature:*`.
