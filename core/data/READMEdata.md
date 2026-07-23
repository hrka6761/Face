# `:core:data`

Repository layer bridging Room DAOs and domain models.

## Contents

| Type | Visibility | Role |
|------|------------|------|
| [PersonRepository] | public | Identity / embedding persistence contract |
| [DefaultPersonRepository] | `internal` | Room-backed implementation |
| [DataModule] | `internal` | Hilt `@Binds` |
| Mappers | `internal` | Entity ↔ domain |

## Usage

Prefer injecting [PersonRepository] from feature / domain layers:

```kotlin
implementation(projects.core.data)
```

[PersonRepository]: src/main/java/ir/hrka/face/data/repository/PersonRepository.kt
[DefaultPersonRepository]: src/main/java/ir/hrka/face/data/repository/DefaultPersonRepository.kt
[DataModule]: src/main/java/ir/hrka/face/data/di/DataModule.kt
