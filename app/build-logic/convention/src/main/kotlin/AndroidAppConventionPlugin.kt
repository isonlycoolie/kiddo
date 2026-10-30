import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("kiddo.quality")

        extensions.configure<ApplicationExtension> {
            namespace = "app.kiddo.app"
            compileSdk = version("android-compileSdk").toInt()
            defaultConfig {
                applicationId = "app.kiddo"
                minSdk = version("android-minSdk").toInt()
                targetSdk = version("android-targetSdk").toInt()
                versionCode = 1
                versionName = "0.1.0"
            }
            buildFeatures { compose = true }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            buildTypes {
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                }
            }
            testOptions {
                unitTests.isIncludeAndroidResources = true
                // Robolectric on an 8 GB machine next to the emulator: one small test JVM.
                unitTests.all {
                    it.maxHeapSize = "1g"
                    it.maxParallelForks = 1
                    it.jvmArgs("-XX:+UseSerialGC")
                }
            }
        }
    }
}
