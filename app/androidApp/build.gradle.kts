plugins {
    id("kiddo.android.app")
}

android {
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":core:resources"))
    implementation(project(":core:model"))
    implementation(project(":core:platform"))
    implementation(project(":core:security"))
    implementation(project(":core:database"))
    implementation(libs.ktor.client.okhttp)
    implementation(libs.okio)
    implementation(libs.compose.components.resources)
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.robolectric)
}
