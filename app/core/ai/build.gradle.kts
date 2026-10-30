plugins {
    id("kiddo.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
        api(libs.ktor.client.core)
        implementation(libs.kotlinx.serialization.json)
    }
}
