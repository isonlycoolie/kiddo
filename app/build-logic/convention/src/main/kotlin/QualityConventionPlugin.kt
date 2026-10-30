import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        pluginManager.apply("com.diffplug.spotless")

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom(rootProject.file("config/detekt/detekt.yml"))
            source.setFrom(fileTree("src") { include("**/*.kt"); exclude("**/generated/**") })
        }
        dependencies { add("detektPlugins", lib("compose-rules-detekt")) }

        extensions.configure<SpotlessExtension> {
            kotlin {
                target("src/**/*.kt")
                targetExclude("**/build/**", "**/generated/**")
                ktlint(version("ktlint")).setEditorConfigPath(rootProject.file(".editorconfig"))
            }
            kotlinGradle {
                target("*.gradle.kts")
                ktlint(version("ktlint"))
            }
        }
    }
}
