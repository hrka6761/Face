pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Face"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":core:navigation")
include(":core:model")
include(":core:data")
include(":core:domain")
include(":core:database")
include(":core:face_detection")
include(":core:face_recognition")

include(":feature:splash:api")
include(":feature:splash:impl")
include(":feature:camera:api")
include(":feature:camera:impl")
