plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal() // This is required to find the shadow plugin artifact
}

dependencies {
    // This feeds the shadow plugin's classes into the compilation environment of your conventions!
    implementation(libs.gradle.plugin.shadow)
}