plugins {
    id("com.android.library")
}

android {
    namespace = "io.github.whoxamxl.aalyrics.platform.media"
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
    api(project(":core:model"))
    testImplementation(kotlin("test"))
}
