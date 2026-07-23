# `:core:navigation`

Navigation 3 helpers shared by Face features (adapted from Hooshmand).

## Contents

| Type | Role |
|------|------|
| [NavigationState] / [rememberNavigationState] | Multi-stack nav state |
| [Navigator] | Forward / back / replaceTopLevel |
| [toEntries] | Convert state to `NavDisplay` entries |

## Usage

```kotlin
implementation(projects.core.navigation)
```

Wired automatically into `feature:*:impl` via the `face.feature.impl` convention plugin.

[NavigationState]: src/main/java/ir/hrka/face/navigation/NavigationState.kt
[Navigator]: src/main/java/ir/hrka/face/navigation/Navigator.kt
