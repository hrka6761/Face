# Module: `build-logic`

Included Gradle build (`includeBuild("build-logic")`) that publishes **convention plugins** for all Face modules.

## Registered plugins

| Plugin ID | Class | Applied to |
|-----------|-------|------------|
| `hrka.android.application` | `AndroidApplicationConventionPlugin` | `:app` |
| `hrka.android.application.compose` | `AndroidApplicationComposeConventionPlugin` | `:app` |
| `hrka.android.flavors` | `AndroidApplicationFlavorsConventionPlugin` | `:app` (`beta` / `prod`) |
| `hrka.android.library` | `AndroidLibraryConventionPlugin` | Library modules |
| `hrka.android.library.compose` | `AndroidLibraryComposeConventionPlugin` | Compose libraries |
| `hrka.android.hilt` | `HiltConventionPlugin` | Hilt modules |
| `hrka.android.room` | `AndroidRoomConventionPlugin` | Room modules |
| `face.feature.api` | `AndroidFeatureApiConventionPlugin` | `feature:*:api` |
| `face.feature.impl` | `AndroidFeatureImplConventionPlugin` | `feature:*:impl` |

## Flavors

| Flavor | ID suffix | Version suffix |
|--------|-----------|----------------|
| `beta` | `.beta` | `-beta` |
| `prod` | — | — |

Face flavors do **not** define network `BASE_URL` fields (the app is fully on-device).

*Last updated: Face convention plugins adapted from Hooshmand.*
