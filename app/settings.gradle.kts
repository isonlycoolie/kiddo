rootProject.name = "kiddo"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":androidApp")
include(":shared")
include(":core:designsystem")
include(":core:resources")
include(":core:navigation")
include(":core:model")
include(":core:platform")
include(":core:security")
include(":core:database")
include(":core:ai")
include(":core:mail")
include(":core:web")
include(":core:testing")
include(":feature:onboarding")
include(":feature:lock")
include(":feature:chat")
include(":feature:history")
include(":feature:cvs")
include(":feature:profile")
include(":feature:settings")
include(":feature:models")
include(":feature:email")
include(":feature:applications")
include(":feature:backup")
