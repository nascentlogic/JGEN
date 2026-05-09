plugins {
    // id("common-conventions")
    id("game-conventions")
}

gameSettings {
    gameName.set("MyGame")
    versionMajor.set(0)
    versionMinor.set(1)
    versionPatch.set(0)
    entryClass.set("org.example.Main")
    internalLogEnabled.set(false)
    // assetDirectories.set(listOf("assets"))
    // programArguments.add("--show-fps")
}