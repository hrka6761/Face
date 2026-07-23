plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.hrka.android.hilt)
}

android {
    namespace = "ir.hrka.face.domain"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)
}
