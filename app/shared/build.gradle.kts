import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    id("kiddo.kmp.compose")
}

// The whole app above the features: the shell, navigation host and Koin wiring that Android and iOS both start. iOS
// links it as the KiddoKit framework the Swift shell hosts; core:security is exported for the shell's crypto bridge.
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(project(":core:designsystem"))
        implementation(project(":core:resources"))
        implementation(project(":core:navigation"))
        implementation(project(":core:model"))
        implementation(project(":core:platform"))
        api(project(":core:security"))
        implementation(project(":core:database"))
        implementation(project(":core:ai"))
        implementation(project(":core:mail"))
        implementation(project(":core:web"))
        implementation(project(":feature:onboarding"))
        implementation(project(":feature:lock"))
        implementation(project(":feature:chat"))
        implementation(project(":feature:history"))
        implementation(project(":feature:cvs"))
        implementation(project(":feature:profile"))
        implementation(project(":feature:settings"))
        implementation(project(":feature:models"))
        implementation(project(":feature:email"))
        implementation(project(":feature:applications"))
        implementation(project(":feature:backup"))
        implementation(libs.ktor.client.core)
        implementation(libs.navigation.compose)
        implementation(libs.lifecycle.runtime.compose)
        implementation(libs.lifecycle.viewmodel.compose)
        implementation(libs.koin.core)
        implementation(libs.koin.compose)
        implementation(libs.koin.compose.viewmodel)
    }
    if (HostManager.hostIsMac) {
        listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach { target ->
            target.binaries.framework {
                baseName = "KiddoKit"
                isStatic = true
                export(project(":core:security"))
            }
        }
        sourceSets.iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
    }
}
