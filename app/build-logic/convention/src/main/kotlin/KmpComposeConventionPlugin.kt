import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("kiddo.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            // Compose resources ship as Android assets; the KMP library target packages them only when enabled.
            extensions.configure<KotlinMultiplatformAndroidLibraryTarget>("android") {
                androidResources { enable = true }
            }
            sourceSets.getByName("commonMain").dependencies {
                implementation(lib("compose-runtime"))
                implementation(lib("compose-foundation"))
                implementation(lib("compose-ui"))
                implementation(lib("compose-components-resources"))
                implementation(lib("compose-ui-tooling-preview"))
                implementation(lib("kotlinx-collections-immutable"))
            }
        }
        dependencies {
            add("androidRuntimeClasspath", lib("androidx-compose-ui-tooling"))
        }
    }
}
