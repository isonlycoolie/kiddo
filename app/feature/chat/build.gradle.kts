plugins {
    id("kiddo.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:database"))
        implementation(project(":core:security"))
        implementation(project(":core:ai"))
        implementation(project(":core:mail"))
        implementation(project(":core:web"))
        implementation(libs.kotlinx.serialization.json)
    }
}
