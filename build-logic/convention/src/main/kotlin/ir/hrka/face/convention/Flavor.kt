package ir.hrka.face.convention

import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor
import org.gradle.kotlin.dsl.invoke

/**
 * Product-flavor dimension used by the Face app.
 */
enum class FlavorDimension {
    /** Distinguishes beta vs production installs. */
    Environment,
}

/**
 * App product flavors (no network base URLs — Face is fully on-device).
 *
 * @property dimension Flavor dimension membership.
 * @property applicationIdSuffix Optional applicationId suffix.
 * @property versionNameSuffix Optional versionName suffix.
 */
enum class Flavor(
    val dimension: FlavorDimension,
    val applicationIdSuffix: String? = null,
    val versionNameSuffix: String? = null,
) {
    /** Internal / test distribution. */
    Beta(
        dimension = FlavorDimension.Environment,
        applicationIdSuffix = ".beta",
        versionNameSuffix = "-beta",
    ),

    /** Production distribution. */
    Prod(
        dimension = FlavorDimension.Environment,
    ),
}

/**
 * Registers [Flavor] entries on an Android [CommonExtension].
 *
 * @param commonExtension Application or library extension to configure.
 * @param flavorConfigurationBlock Optional per-flavor customization.
 */
fun configureFlavors(
    commonExtension: CommonExtension,
    flavorConfigurationBlock: ProductFlavor.(flavor: Flavor) -> Unit = {},
) {
    commonExtension.apply {
        FlavorDimension.entries.forEach { dimension ->
            flavorDimensions += dimension.name
        }

        productFlavors {
            Flavor.entries.forEach { flavor ->
                create(flavor.name.lowercase()) {
                    dimension = flavor.dimension.name
                    flavorConfigurationBlock(this, flavor)
                    if (this is ApplicationProductFlavor) {
                        if (flavor.applicationIdSuffix != null) {
                            applicationIdSuffix = flavor.applicationIdSuffix
                        }
                        if (flavor.versionNameSuffix != null) {
                            versionNameSuffix = flavor.versionNameSuffix
                        }
                    }
                }
            }
        }
    }
}
