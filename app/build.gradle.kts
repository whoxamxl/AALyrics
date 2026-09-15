plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.whoxamxl.aalyrics"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.whoxamxl.aalyrics"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-dev"
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
    implementation(project(":feature:phone"))
    implementation(project(":feature:automotive"))
}
