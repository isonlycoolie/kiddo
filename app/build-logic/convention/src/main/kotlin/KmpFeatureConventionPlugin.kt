import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class KmpFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("kiddo.kmp.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(project(":core:designsystem"))
                implementation(project(":core:resources"))
                implementation(project(":core:navigation"))
                implementation(project(":core:model"))
                implementation(project(":core:platform"))
                implementation(lib("lifecycle-viewmodel-compose"))
                implementation(lib("lifecycle-runtime-compose"))
                implementation(lib("navigation-compose"))
                implementation(lib("koin-core"))
                implementation(lib("koin-compose"))
                implementation(lib("koin-compose-viewmodel"))
            }
            sourceSets.getByName("commonTest").dependencies {
                implementation(project(":core:testing"))
            }
        }
    }
}
