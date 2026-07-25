plugins {
    alias(libs.plugins.hrka.android.application)
    alias(libs.plugins.hrka.android.app.compose)
    alias(libs.plugins.hrka.android.flavors)
    alias(libs.plugins.hrka.android.hilt)
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.navigation3.ui)

    implementation(projects.core.navigation)
    implementation(projects.core.domain)
    implementation(projects.core.database)
    implementation(projects.core.downloadManager)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(projects.feature.splash.api)
    implementation(projects.feature.splash.impl)
    implementation(projects.feature.camera.api)
    implementation(projects.feature.camera.impl)
}
