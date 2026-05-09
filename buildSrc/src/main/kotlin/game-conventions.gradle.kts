import io.github.nascentlogic.jgen.buildsrc.GameBuildSettings
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


plugins {
    id("common-conventions")
    id("com.gradleup.shadow")
    application
}

val gameSettings = extensions.create<GameBuildSettings>("gameSettings")

val generatedMetadataDir = layout.buildDirectory.dir("generated/game-module-metadata")
val exportDir = layout.buildDirectory.dir("distribution/export")
val archiveDir = layout.buildDirectory.dir("distribution/archives")
val crossPlatformDir = layout.buildDirectory.dir("distribution/cross-platform")
val win64DistDir = layout.buildDirectory.dir("distribution/win64")

val jpackagePath = provider {
    val toolchainService = extensions.getByType<JavaToolchainService>()
    val launcher = toolchainService.launcherFor(java.toolchain).get()
    val binDir = launcher.metadata.installationPath.dir("bin")
    val isWindows = org.gradle.internal.os.OperatingSystem.current().isWindows
    val extension = if (isWindows) ".exe" else ""
    binDir.file("jpackage$extension").asFile.absolutePath
}

// --- Argument Merging Logic ---
val requiredJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
val combinedJvmArgs: Provider<List<String>> = gameSettings.jvmArguments.map { userArgs ->
    requiredJvmArgs + userArgs
}

// --- Project Configuration ---
afterEvaluate {
    project.group = gameSettings.projectGroup.get()
    project.version = gameSettings.fullVersionString.get()
    project.description = gameSettings.projectDescription.get()
}

dependencies {
    implementation(project(":Core"))
}

application {
    mainClass.set(gameSettings.entryClass)
}

tasks.withType<JavaExec> {
    doFirst {
        // Resolve and apply JVM Arguments
        val resolvedArgs = combinedJvmArgs.get()
        jvmArgs(*resolvedArgs.toTypedArray())
        // Resolve and apply Program Arguments
        val pArgs = gameSettings.programArguments.get()
        if (pArgs.isNotEmpty()) {
            args(pArgs)
        }
        println("\n--- [DEVELOPMENT RUN] ---")
        println("JVM Arguments:")
        allJvmArgs.forEach { println("  - $it") }
        if (pArgs.isNotEmpty()) {
            println("Program Arguments:")
            pArgs.forEach { println("  - $it") }
        }
        println("-------------------------\n")
    }
}

// --- Metadata Generation ---

val generateGameModuleMetadata = tasks.register("generateGameModuleMetadata") {
    group = "build"
    description = "Generates game-module.properties under META-INF."

    inputs.property("entryClass", gameSettings.entryClass)
    inputs.property("gameName", gameSettings.gameName)
    inputs.property("companyName", gameSettings.companyName)
    inputs.property("major", gameSettings.versionMajor)
    inputs.property("minor", gameSettings.versionMinor)
    inputs.property("patch", gameSettings.versionPatch)
    inputs.property("versionClassifier",gameSettings.versionClassifier)
    inputs.property("internalLogEnabled", gameSettings.internalLogEnabled)

    outputs.dir(generatedMetadataDir)

    doLast {

        val metaInfDir = generatedMetadataDir.get().asFile.resolve("META-INF")
        val propsFile = metaInfDir.resolve("game-module.properties")
        metaInfDir.mkdirs()

        propsFile.writeText("""
            game.name=${gameSettings.gameName.get()}
            game.company.name=${gameSettings.companyName.get()}
            game.entry.class=${gameSettings.entryClass.get()}
            game.version.major=${gameSettings.versionMajor.get()}
            game.version.minor=${gameSettings.versionMinor.get()}
            game.version.patch=${gameSettings.versionPatch.get()}
            game.version.classifier=${gameSettings.versionClassifier.get()}
            game.version.full=${gameSettings.fullVersionString.get()}
            game.internal.log.enabled=${gameSettings.internalLogEnabled.get()}
        """.trimIndent())
        logger.info("Metadata generated at: ${propsFile.absolutePath}")
    }
}

sourceSets.main.get().resources.srcDir(generatedMetadataDir)

tasks.processResources {
    dependsOn(generateGameModuleMetadata)
}

// --- Distribution Tasks ---

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set(gameSettings.gameName)
    archiveClassifier.set("all")
    // This ensures that the tiny-log writers from the library
    // and your writer from the engine are combined into one file.
    mergeServiceFiles() // research this
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Implementation-Title" to gameSettings.gameName.get(),
            "Implementation-Version" to project.version.toString(),
            "Multi-Release" to "true"
        )
    }
}

val exportGameTask: TaskProvider<Sync> = tasks.register<Sync>("exportGame") {
    group = "distribution"
    description = "Stages game for distributions"
    dependsOn("shadowJar")
    into(exportDir)

    from(tasks.named("shadowJar")) {
        rename { filename ->
            if (filename.contains("-all.jar")) "${gameSettings.gameName.get()}.jar" else filename
        }
    }

    val assetPaths = gameSettings.assetDirectories.get()
    assetPaths.forEach { path ->
        val source = projectDir.resolve(path)
        if (source.exists()) {
            if (source.isDirectory) from(source) { into(path) }
            else from(source)
        }
    }

    doLast {
        assetPaths.forEach { path ->
            val source = projectDir.resolve(path)
            if (source.exists() && source.isDirectory) {
                val targetFolder = exportDir.get().asFile.resolve(path)
                if (!targetFolder.exists()) {
                    targetFolder.mkdirs()
                    println("Created missing directory: $path")
                }
            }
        }
        println("Export staging complete: ${exportDir.get().asFile.absolutePath}")
    }
}

val packageCrossPlatform: TaskProvider<Sync> = tasks.register<Sync>("packageCrossPlatform") {
    group = "distribution"
    description = "Packages the cross-platform distribution and generates run instructions."
    dependsOn(exportGameTask)
    val gameName = gameSettings.gameName.get()
    into(crossPlatformDir.get().dir(gameName))
    from(exportDir)

    doLast {
        val targetDir = crossPlatformDir.get().dir(gameName).asFile
        val runFile = targetDir.resolve("how-to-run.txt")
        val instructions = """
            ==================================================
            How to Run $gameName (Cross-Platform Edition)
            ==================================================
            
            Prerequisites:
            - You must have Java 25 or newer installed on your system.
            
            Instructions:
            1. Open a terminal, console, or command prompt.
            2. Navigate to this directory.
            3. Run the appropriate command for your operating system below:
            
            Windows (PowerShell):
            java --enable-native-access=ALL-UNNAMED -jar ./${gameName}.jar
            
            Windows (Command Prompt):
            java --enable-native-access=ALL-UNNAMED -jar ${gameName}.jar
            
            macOS / Linux:
            java --enable-native-access=ALL-UNNAMED -jar ${gameName}.jar
            
            Note: The '--enable-native-access' flag is required by the engine to access hardware and graphics libraries safely.
        """.trimIndent()

        runFile.writeText(instructions)
        println("Cross-platform package created at: ${targetDir.absolutePath}")
        println("Generated: ${runFile.name}")
    }
}

val packageWin64: TaskProvider<Task> = tasks.register("packageWin64") {
    group = "distribution"
    description = "Packages the Windows 64-bit distribution using jpackage."
    dependsOn(exportGameTask)
    doLast {
        val inputDir = exportDir.get().asFile
        val outputDir = win64DistDir.get().asFile
        val appImageDir = outputDir.resolve(gameSettings.gameName.get())

        // Since jpackage isn't a Sync task, we wipe the target manually
        if (appImageDir.exists()) {
            println("Cleaning up previous build: ${appImageDir.absolutePath}")
            try {
                appImageDir.walkBottomUp().forEach { file ->
                    file.setWritable(true)
                    if (!file.delete()) {
                        throw Exception("Failed to delete file: ${file.absolutePath}")
                    }
                }
            } catch (e: Exception) {
                throw GradleException("CRITICAL: Could not delete existing win64 folder. Is the game still running?", e)
            }
        }

        val commandList = mutableListOf (
            jpackagePath.get(),
            "--type", "app-image",
            "--dest", outputDir.absolutePath,
            "--name", gameSettings.gameName.get(),
            "--input", inputDir.absolutePath,
            "--main-jar", "${gameSettings.gameName.get()}.jar",
            "--main-class", gameSettings.entryClass.get()
        )

        // Inject JVM Arguments (Required + User: JVM args)
        // Each argument MUST be preceded by '--java-options'
        val resolvedJvmArgs = combinedJvmArgs.get()
        resolvedJvmArgs.forEach { arg ->
            commandList.add("--java-options")
            commandList.add(arg)
        }

        commandList.add("--win-console") // remove in prod

        val pArgs = gameSettings.programArguments.get()
        if (pArgs.isNotEmpty()) {
            commandList.add("--arguments")
            commandList.add(pArgs.joinToString(" "))
        }

        println("\n--- [PACKAGING WIN64] ---")
        println("Output: ${appImageDir.absolutePath}")
        println("JVM Arguments:")
        resolvedJvmArgs.forEach { println("  - $it") }
        val process = ProcessBuilder(commandList).inheritIO().start()
        if (process.waitFor() != 0) throw GradleException("jpackage failed.")
    }
}

// --- Archiving ---

tasks.register<Zip>("zipCrossPlatform") {
    group = "distribution"
    description = "Archives the cross-platform distribution."
    dependsOn(packageCrossPlatform)
    from(crossPlatformDir.get().dir(gameSettings.gameName.get()))
    destinationDirectory.set(archiveDir)
    archiveFileName.set("${gameSettings.gameName.get()}-v${gameSettings.fullVersionString.get()}-universal.zip")
    doLast {  println("Cross-platform archive created: ${archiveFileName.get()}")  }
}

tasks.register<Zip>("zipWin64") {
    group = "distribution"
    description = "Archives the Windows 64-bit distribution."
    dependsOn(packageWin64)
    from(win64DistDir.get().dir(gameSettings.gameName.get()))
    destinationDirectory.set(archiveDir)
    archiveFileName.set("${gameSettings.gameName.get()}-v${gameSettings.fullVersionString.get()}-win64.zip")
    doLast {  println("Windows 64-bit archive created: ${archiveFileName.get()}")  }
}