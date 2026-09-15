plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core:model"))
    api(project(":provider:api"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
