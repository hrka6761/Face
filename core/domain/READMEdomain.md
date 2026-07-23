# `:core:domain`

Use cases for enrollment and identification.

## Contents

| Type | Role |
|------|------|
| [EnrollPersonUseCase] | Save name + embedding |
| [IdentifyFacesUseCase] | Match embeddings to enrolled persons |
| [ObservePersonsUseCase] | Observe enrolled identities |

## Usage

```kotlin
implementation(projects.core.domain)
```

Inject use cases into `@HiltViewModel` classes.

[EnrollPersonUseCase]: src/main/java/ir/hrka/face/domain/EnrollPersonUseCase.kt
[IdentifyFacesUseCase]: src/main/java/ir/hrka/face/domain/IdentifyFacesUseCase.kt
[ObservePersonsUseCase]: src/main/java/ir/hrka/face/domain/ObservePersonsUseCase.kt
