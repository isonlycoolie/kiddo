import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    id("kiddo.kmp.compose")
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:resources"))
        api(project(":core:model"))
    }
    if (HostManager.hostIsMac) {
        // The iOS edge swipe reaches open sheets through Compose's common back handler.
        sourceSets.iosMain.dependencies { implementation(libs.compose.ui.backhandler) }
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.activity.compose)
    }
}
