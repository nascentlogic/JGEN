package io.github.nascentlogic.jgen.io;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class GameModuleProperties {

    private GameModuleProperties() { }
    private static Properties properties;
    private static volatile boolean loaded;

    private static final String PROPERTIES_PATH = "/META-INF/game-module.properties";
    public static final String GAME_NAME = "game.name";
    public static final String GAME_COMPANY_NAME = "game.company.name";
    public static final String GAME_ENTRY_CLASS = "game.entry.class";
    public static final String GAME_VERSION_MAJOR = "game.version.major";
    public static final String GAME_VERSION_MINOR = "game.version.minor";
    public static final String GAME_VERSION_PATCH = "game.version.patch";
    public static final String GAME_VERSION_CLASSIFIER = "game.version.classifier";
    public static final String GAME_VERSION_FULL = "game.version.full";
    public static final String GAME_INTERNAL_LOG_ENABLED = "game.internal.log.enabled";

    /**
     * Keys are public static Strings, located in {@link GameModuleProperties}.
     * @param key properties key.
     * @return property as String
     * @throws IOException if the properties file could not be found
     */
    public static String get(String key) throws IOException {
        if (loaded) return properties.getProperty(key);
        synchronized (GameModuleProperties.class) {
            if (loaded) return properties.getProperty(key);
            try (InputStream stream = GameModuleProperties.class.getResourceAsStream(PROPERTIES_PATH)){
                if (stream == null) {
                    throw new FileNotFoundException("Game module metadata not found: " + PROPERTIES_PATH);
                } properties = new Properties();
                properties.load(stream);
                loaded = true;
                return properties.getProperty(key);
            }
        }
    }

}
