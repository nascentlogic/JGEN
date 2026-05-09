plugins {`java-base`}

val libs = the<VersionCatalogsExtension>().named("libs")

java {
    toolchain {
        val jdkVersion = libs.findVersion("jdk").get().requiredVersion
        languageVersion.set(JavaLanguageVersion.of(jdkVersion))
    }
}

repositories {  mavenCentral()  }