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
include(":provider:selection")
include(":platform:media")
include(":feature:phone")
include(":feature:automotive")
include(":provider:matching")
include(":provider:lrc")
include(":provider:lrclib")
