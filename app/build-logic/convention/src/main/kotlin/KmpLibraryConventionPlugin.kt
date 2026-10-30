import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.konan.target.HostManager

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")
        pluginManager.apply("kiddo.quality")

        extensions.configure<KotlinMultiplatformExtension> {
            extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                namespace = kiddoNamespace
                compileSdk = version("android-compileSdk").toInt()
                minSdk = version("android-minSdk").toInt()
                withHostTest { isIncludeAndroidResources = true }
                compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
            }
            // iOS shares this code from its own milestone; Kotlin/Native for Apple targets only builds on a Mac.
            if (HostManager.hostIsMac) {
                iosArm64()
                iosSimulatorArm64()
                iosX64()
            }
            sourceSets.getByName("commonMain").dependencies {
                implementation(lib("kotlinx-coroutines-core"))
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(kotlin("test"))
                implementation(lib("kotlinx-coroutines-test"))
                implementation(lib("turbine"))
            }
        }
    }
}
