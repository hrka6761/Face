plugins {
    alias(libs.plugins.hrka.android.library)
}

android {
    namespace = "ir.hrka.face.detection"
}

dependencies {
    api(projects.core.model)
    implementation(libs.face.detection)
    implementation(libs.androidx.camera.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
}
