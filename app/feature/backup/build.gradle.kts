plugins {
    id("kiddo.kmp.feature")
}

kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:database"))
        implementation(project(":core:security"))
    }
}
