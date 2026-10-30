plugins {
    id("kiddo.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
        api(libs.ktor.client.core)
    }
}
