plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:lyrics"))
    implementation(project(":provider:matching"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
