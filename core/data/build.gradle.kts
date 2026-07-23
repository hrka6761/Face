plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.hrka.android.hilt)
}

android {
    namespace = "ir.hrka.face.data"
}

dependencies {
    api(projects.core.model)
    api(projects.core.database)

    implementation(libs.kotlinx.coroutines.android)
}
