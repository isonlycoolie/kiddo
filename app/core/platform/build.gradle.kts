plugins {
    id("kiddo.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.activity.compose)
    }
}
