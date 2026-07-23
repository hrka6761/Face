plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.hrka.android.lib.compose)
}

android {
    namespace = "ir.hrka.face.navigation"
}

dependencies {
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.savedstate.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
