import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AALyrics"

include(":app")
include(":core:model")
include(":core:lyrics")
include(":provider:api")
include(":platform:media")
include(":feature:phone")
include(":feature:automotive")
