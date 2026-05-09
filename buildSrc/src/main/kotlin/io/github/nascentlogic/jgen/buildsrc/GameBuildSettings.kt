package io.github.nascentlogic.jgen.buildsrc

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import javax.inject.Inject

abstract class GameBuildSettings @Inject constructor(objects: ObjectFactory) {

    abstract val gameName: Property<String>
    abstract val companyName: Property<String>
    abstract val entryClass: Property<String>
    abstract val assetDirectories: ListProperty<String>
    abstract val projectGroup: Property<String>
    abstract val projectDescription: Property<String>
    abstract val versionMajor: Property<Int>
    abstract val versionMinor: Property<Int>
    abstract val versionPatch: Property<Int>
    abstract val versionClassifier: Property<String>
    abstract val internalLogEnabled: Property<Boolean>
    abstract val jvmArguments: ListProperty<String>
    abstract val programArguments: ListProperty<String>

    val fullVersionString: Provider<String> = versionMajor.flatMap { maj ->
        versionMinor.flatMap { min ->
            versionPatch.flatMap { patch ->
                versionClassifier.map { classifier ->
                    val base = "$maj.$min.$patch"
                    if (classifier.isNotEmpty()) "$base-$classifier" else base
                }
            }
        }
    }

    init {
        gameName.convention("Untitled-Game")
        companyName.convention("NascentLogic")
        projectGroup.convention("org.example")
        projectDescription.convention("No game description")
        assetDirectories.convention(listOf())
        versionMajor.convention(0)
        versionMinor.convention(1)
        versionPatch.convention(0)
        versionClassifier.convention("SNAPSHOT")
        internalLogEnabled.convention(false)
        jvmArguments.convention(listOf())
        programArguments.convention(listOf())
    }
}