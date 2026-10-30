plugins {
    id("kiddo.kmp.compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(libs.navigation.compose)
        api(libs.kotlinx.serialization.core)
        implementation(project(":core:designsystem"))
    }
}
