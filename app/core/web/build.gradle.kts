plugins {
    id("kiddo.kmp.library")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
        api(libs.ktor.client.core)
        implementation(libs.ksoup)
    }
    sourceSets.commonTest.dependencies {
        implementation(libs.ktor.client.mock)
        implementation(libs.kotlinx.coroutines.test)
    }
}
