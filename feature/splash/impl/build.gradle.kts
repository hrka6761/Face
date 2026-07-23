plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.hrka.android.lib.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.hrka.android.hilt)
    alias(libs.plugins.face.feature.impl)
}

android {
    namespace = "ir.hrka.face.splash.impl"
}

dependencies {
    implementation(projects.feature.splash.api)
    implementation(projects.feature.camera.api)
}
