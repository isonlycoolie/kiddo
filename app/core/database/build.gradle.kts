import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    id("kiddo.kmp.library")
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets.commonMain.dependencies {
        api(project(":core:model"))
        api(project(":core:security"))
        api(libs.sqldelight.coroutines)
        implementation(libs.kotlinx.serialization.json)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.sqldelight.android.driver)
    }
    if (HostManager.hostIsMac) {
        sourceSets.iosMain.dependencies { implementation(libs.sqldelight.native.driver) }
    }
    sourceSets.getByName("androidHostTest").dependencies {
        implementation(libs.sqldelight.sqlite.driver)
    }
}

sqldelight {
    databases {
        create("KiddoDatabase") {
            packageName.set("app.kiddo.core.database")
        }
    }
}
