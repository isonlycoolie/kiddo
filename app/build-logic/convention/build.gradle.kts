plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("quality") {
            id = "kiddo.quality"
            implementationClass = "QualityConventionPlugin"
        }
        register("kmpLibrary") {
            id = "kiddo.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "kiddo.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("kmpFeature") {
            id = "kiddo.kmp.feature"
            implementationClass = "KmpFeatureConventionPlugin"
        }
        register("androidApp") {
            id = "kiddo.android.app"
            implementationClass = "AndroidAppConventionPlugin"
        }
    }
}
