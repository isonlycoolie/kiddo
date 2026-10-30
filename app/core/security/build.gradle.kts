plugins {
    id("kiddo.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
        implementation(libs.libsodium)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.activity.compose)
        api(libs.androidx.biometric)
    }
}
