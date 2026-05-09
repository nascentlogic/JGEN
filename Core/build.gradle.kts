plugins {
    id("common-conventions")
    `java-library`
}

group = "io.github.nascentlogic"
version = "0.0.1"


val allPlatforms = listOf(
    "natives-windows",
    "natives-linux",
    "natives-macos",
    "natives-macos-arm64"
)

dependencies {

    api(platform(libs.lwjgl.bom))
    api(libs.lwjgl)
    api(libs.lwjgl.glfw)
    api(libs.lwjgl.opengl)
    api(libs.lwjgl.stb)

    api(libs.joml)
    api(libs.joml.primitives)
    api(libs.tinylog.api)
    api(libs.tinylog.impl)
    api(libs.gson)

    // This ensures your IDE and your Fat JAR have every platform available
    allPlatforms.forEach { platform ->
        runtimeOnly("org.lwjgl:lwjgl::$platform")
        runtimeOnly("org.lwjgl:lwjgl-glfw::$platform")
        runtimeOnly("org.lwjgl:lwjgl-opengl::$platform")
        runtimeOnly("org.lwjgl:lwjgl-stb::$platform")
    }
}