plugins {
    id("com.android.library")
}

android {
    namespace = "io.github.whoxamxl.aalyrics.feature.automotive"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:lyrics"))
}
