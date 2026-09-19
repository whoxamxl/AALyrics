import java.util.Properties

plugins {
    id("com.android.application")
}

// Values stay in environment/ignored local files and generated build output only.
val petitLyricsLocal = Properties().apply {
    val source = rootProject.file("secrets.properties.local")
    if (source.isFile) source.inputStream().use { load(it) }
}
fun javaStringLiteral(value: String): String = "\"" + value
    .replace("\\", "\\\\").replace("\"", "\\\"")
    .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t") + "\""

val releaseStorePath = providers.environmentVariable("AALYRICS_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("AALYRICS_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("AALYRICS_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("AALYRICS_RELEASE_KEY_PASSWORD").orNull
val releaseSigningConfigured = listOf(
    releaseStorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

val releaseTaskRequested = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}
if (releaseTaskRequested && !releaseSigningConfigured) {
    error(
        "Release signing is not configured. Set AALYRICS_RELEASE_STORE_FILE, " +
            "AALYRICS_RELEASE_STORE_PASSWORD, AALYRICS_RELEASE_KEY_ALIAS, and " +
            "AALYRICS_RELEASE_KEY_PASSWORD.",
    )
}

val configuredVersionCode = providers.environmentVariable("AALYRICS_VERSION_CODE")
    .orNull
    ?.toIntOrNull()
    ?.takeIf { it > 0 }
    ?: 1
val configuredVersionName = providers.environmentVariable("AALYRICS_VERSION_NAME")
    .orNull
    ?.takeIf { it.isNotBlank() }
    ?: "0.1.0-dev"

android {
    buildFeatures { buildConfig = true }
    namespace = "io.github.whoxamxl.aalyrics"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.whoxamxl.aalyrics"
        minSdk = 26
        targetSdk = 36
        versionCode = configuredVersionCode
        versionName = configuredVersionName

        listOf(
            "PETITLYRICS_USER_ID", "PETITLYRICS_APP_NAME",
            "PETITLYRICS_PKG_NAME", "PETITLYRICS_CLIENT_APP_ID",
        ).forEach { key ->
            val value = providers.environmentVariable(key).orNull ?: petitLyricsLocal.getProperty(key, "")
            buildConfigField("String", key, javaStringLiteral(value))
        }
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = file(requireNotNull(releaseStorePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:lyrics"))
    implementation(project(":platform:media"))
    implementation(project(":provider:api"))
    implementation(project(":provider:selection"))
    implementation(project(":provider:lrclib"))
    implementation(project(":provider:petitlyrics"))
    implementation(project(":provider:musixmatch"))
    implementation(project(":provider:synclrc"))
    implementation(project(":ui:phone"))
    implementation(project(":ui:automotive"))
    implementation(project(":translation:api"))
    implementation(project(":translation:mlkit"))
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("androidx.car.app:app:1.7.0")
    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}
