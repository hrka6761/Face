plugins {
    alias(libs.plugins.hrka.android.library)
}

android {
    namespace = "ir.hrka.face.recognition"

    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    api(projects.core.model)
    implementation(libs.tensorflow)
    implementation(libs.litert.support.api)
    implementation(libs.androidx.camera.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
