plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.hrka.android.lib.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.hrka.android.hilt)
    alias(libs.plugins.face.feature.impl)
}

android {
    namespace = "ir.hrka.face.camera.impl"
}

dependencies {
    implementation(projects.feature.camera.api)
    implementation(projects.core.domain)
    implementation(projects.core.faceDetection)
    implementation(projects.core.faceRecognitionEngine)
    implementation(projects.core.downloadManager)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)
    implementation(libs.kotlinx.coroutines.android)
}
