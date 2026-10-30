import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.catalog: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.lib(alias: String): Provider<MinimalExternalModuleDependency> = catalog.findLibrary(alias).get()

internal fun Project.version(alias: String): String = catalog.findVersion(alias).get().requiredVersion

// Namespace from the module path, so `:core:designsystem` becomes `app.kiddo.core.designsystem`.
internal val Project.kiddoNamespace: String
    get() = "app.kiddo" + path.replace(':', '.').lowercase()
