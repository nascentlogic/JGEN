package io.github.nascentlogic.jgen.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.tinylog.Logger;
import org.tinylog.configuration.Configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public final class Disk {

    /** Size limit (in bytes) for reading files (internal ByteBuffer allocation).
     * Assume reading larger files throw {@link IOException}. */
    public static final long MAX_FILE_SIZE = 256 * 1024 * 1024;
    private static volatile boolean INITIALIZED;
    private static boolean DEV_MODE;
    private static Path GAME_ROOT;
    private static Path USER_DATA;
    private static Gson GSON;



    // **************************************************************************************
    //  INITIALIZATION
    // **************************************************************************************



    /**
     * Initializes the IO system by discovering the game's entry class,
     * determining the execution environment (Dev vs Production), and
     * resolving the root directories for game assets and user data.
     * This method is thread-safe and should be called once at the
     * very beginning of the Core initialization.
     * @throws IOException if critical paths or properties cannot be resolved.
     */
    public static void initialize() throws IOException {
        if (INITIALIZED) return;;
        synchronized (Disk.class) {
            if (INITIALIZED) return;
            Class<?> entryClass = identifyEntryClass();
            Path codeSourceLoc = codeSourceLocation(entryClass);
            DEV_MODE = !Files.isRegularFile(codeSourceLoc);
            GAME_ROOT = identifyGameRoot(codeSourceLoc);
            USER_DATA = identifyUserDirectory();
            GSON = configureGson();
            configureLogger();
            INITIALIZED = true;
        }
    }

    /**
     * Resolves the physical filesystem location of the provided class's bytecode.
     * This method returns an absolute path pointing to either a directory (when running
     * from compiled classes in an IDE or Gradle) or a specific JAR file (when running
     * a packaged distribution).
     * @param clazz the class whose origin is to be resolved
     * @return an absolute Path representing the location of the class's code source
     * @throws IOException if the CodeSource is null or the location URL cannot be converted to a URI
     */
    private static Path codeSourceLocation(Class<?> clazz) throws IOException {
        var codeSource = clazz.getProtectionDomain().getCodeSource();
        if (codeSource == null) throw new IOException("CodeSource of class '" + clazz.getName() + "' is null");
        try { return Path.of(codeSource.getLocation().toURI());
        } catch (Exception e) {
            throw new IOException("Failed to convert code source location to Path for class: " + clazz.getName(), e);
        }
    }

    /**
     * Identifies the game entry class using properties or the JVM command fallback.
     * @return the Class object for the game entry point.
     * @throws IOException if the class cannot be found or fails to link.
     */
    private static Class<?> identifyEntryClass() throws IOException {
        String entryClassName = GameModuleProperties.get(GameModuleProperties.GAME_ENTRY_CLASS);
        if (entryClassName == null || entryClassName.isBlank()) {
            System.out.println("no entry class available in game module properties"); // should not occur
            String command = System.getProperty("sun.java.command");
            if (command == null || command.isBlank()) {
                throw new IOException("sun.java.command property is missing - cannot determine entry class");
            } entryClassName = command.split("\\s+")[0].trim();
        } try { // Use the Context ClassLoader to bridge the Core-to-Game module gap.
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            return Class.forName(entryClassName, false, loader);
        } catch (ClassNotFoundException e) {
            throw new IOException("Could not find entry class: " + entryClassName, e);
        } catch (LinkageError e) { // Catches 'other throwable' like NoClassDefFoundError if dependencies are missing.
            throw new IOException("Linkage error while loading entry class: " + entryClassName, e);
        }
    }

    /**
     * Locates the root of the game installation. In Dev mode, it walks up the
     * file tree looking for a 'build' folder. In Production, it returns the
     * folder containing the Jar.
     * @param codeSourceLocation the Path to the class bytecode.
     * @return the resolved game root directory.
     * @throws IOException if a 'build' folder cannot be found during Dev mode discovery.
     */
    private static Path identifyGameRoot(Path codeSourceLocation) throws IOException {
        boolean insideJar = Files.isRegularFile(codeSourceLocation);
        if (insideJar) return codeSourceLocation.getParent().toAbsolutePath().normalize();
        Path current = codeSourceLocation.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("build"))) {
                return current;
            } current = current.getParent();
        } throw new IOException("Could not resolve game root. No 'build' folder found walking up from: "
                + codeSourceLocation);
    }

    /**
     * Resolves the OS-specific root directory for application data (e.g., AppData on Windows).
     * @return the platform-specific data root path.
     * @throws IOException if the user home or environment variables cannot be resolved.
     */
    private static Path platformAppDataRoot() throws IOException {
        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isBlank()) {
            throw new IOException("Critical System Property 'user.home' is missing or inaccessible.");
        } Path home = toPath(userHome);
        return switch (Platform.get()) {
            case WINDOWS -> { String env = System.getenv("APPDATA");
                yield (env != null && !env.isBlank()) ? toPath(env) : resolve(home, "AppData", "Roaming");
            } case MAC -> resolve(home, "Library", "Application Support");
            case LINUX -> { String env = System.getenv("XDG_DATA_HOME");
                yield (env != null && !env.isBlank()) ? toPath(env) : resolve(home, ".local", "share");
            }
        };
    }

    /**
     * Resolves the specific user directory for the game based on the game name property.
     * @return the Path to the game-specific persistent data folder.
     * @throws IOException if properties or platform paths cannot be resolved.
     */
    private static Path identifyUserDirectory() throws IOException {
        Path appDataRoot = platformAppDataRoot();
        String gameName = GameModuleProperties.get(GameModuleProperties.GAME_NAME);
        String companyName = GameModuleProperties.get(GameModuleProperties.GAME_COMPANY_NAME);
        gameName = gameName == null || gameName.isBlank() ? "Untitled-Game" : gameName.trim();
        companyName = companyName == null || companyName.isBlank() ? "JGEN" : companyName.trim();
        return resolve(appDataRoot,companyName,gameName);
    }

    /**
     * Configures and returns a shared {@link Gson} instance with pretty printing
     * and disabled HTML escaping.
     * @return the configured Gson object.
     */
    private static Gson configureGson() {
        // todo: look into this later
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    /**
     * Configures TinyLog programmatically based on the current environment.
     * <p>This method sets up different logging strategies depending on whether
     * the application is running in development (IDE) or production mode:</p>
     * <ul>
     *   <li><strong>Development Mode:</strong> Console writer + Internal on-screen logger</li>
     *   <li><strong>Production Mode:</strong> Rolling file writer + Internal on-screen logger</li>
     * </ul>
     * <p>The internal (on-screen) logger is controlled by the property
     * {@code GAME_INTERNAL_LOG_ENABLED} from {@link GameModuleProperties}.</p>
     */
    private static void configureLogger() {
        Map<String,String> config = new LinkedHashMap<>();
        config.put("level", devMode() ? "debug" : "info"); // Global level
        boolean internalLogEnabled = false;
        Exception iLogConfEx = null;
        Exception fLogConfEx = null;
        try { String logEnabledString = GameModuleProperties.get(GameModuleProperties.GAME_INTERNAL_LOG_ENABLED);
            if (Boolean.parseBoolean(logEnabledString)) {
                config.put("writerInternal", JgenlLogWriter.class.getName());
                internalLogEnabled = true; }
        } catch (IOException e) { iLogConfEx = e; }
        if (!devMode()) {
            try {Path logFolder = logOutputDirectory();
                Files.createDirectories(logFolder);
                String sep = FileSystems.getDefault().getSeparator();
                config.put("writerFile", "rolling file");
                config.put("writerFile.file", logFolder + sep + "log-{count}.txt");
                config.put("writerFile.policies", "startup, size: 1mb");
                config.put("writerFile.backups", "3");
                config.put("writerFile.buffered", "true");
                config.put("writerFile.append", "false");
                config.put("writerFile.writingthread", "true");
                config.put("writerFile.format","{date: HH:mm:ss.SS} {pipe} {level|min-size=5} " +
                        "{pipe} {class-name|size=20} {pipe} {line|min-size=4} {pipe} {message}");
            } catch (IOException e) { fLogConfEx = e; }
        } if (devMode() || fLogConfEx != null) {
            config.put("writerConsole", "console");
            config.put("writerConsole.stream", "out");
            config.put("writerConsole.writingthread", "false");
            config.put("writerConsole.format","{date: HH:mm:ss.SS} {pipe} {level|min-size=5} " +
                    "{pipe} {class-name|size=20} {pipe} {line|min-size=4} {pipe} {message}");
        } Configuration.replace(config);
        if (fLogConfEx != null) Logger.warn(fLogConfEx,"Failed to create log directory - falling back to console");
        if (iLogConfEx != null) Logger.warn(iLogConfEx,"Failed to read internal log property");
        Logger.info("TinyLog initialized ({} mode, internal log: {})",
                devMode() ? "DEVELOPMENT" : "PRODUCTION", internalLogEnabled);
    }




    // **************************************************************************************
    //  READ OPERATIONS
    // **************************************************************************************




    /**
     * Loads a file from the build "resources" directory into a direct {@link ByteBuffer}.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a flipped direct {@code ByteBuffer} containing the resource data.
     * @throws IOException if the resource cannot be found or read.
     */
    public static ByteBuffer resourceDirect(String first, String... more) throws IOException {
        return resource(toResourcePath(first,more),true);
    }

    /**
     * Loads a file from the build "resources" directory into a heap {@link ByteBuffer}.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a flipped heap {@code ByteBuffer} containing the resource data.
     * @throws IOException if the resource cannot be found or read.
     */
    public static ByteBuffer resourceHeap(String first, String... more) throws IOException {
        return resource(toResourcePath(first,more),false);
    }

    /**
     * Loads a file from the {@code GAME_ROOT} directory into a direct {@link ByteBuffer}.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a flipped direct {@code ByteBuffer} containing the file data.
     * @throws IOException if the file is missing, exceeds size limits, or is locked.
     */
    public static ByteBuffer gameLoadDirect(String first, String... more) throws IOException {
        return load(resolve(gameRootDirectory(),first,more),true);
    }

    /**
     * Loads a file from the {@code GAME_ROOT} directory into a heap {@link ByteBuffer}.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a flipped heap {@code ByteBuffer} containing the file data.
     * @throws IOException if the file is missing, exceeds size limits, or is locked.
     */
    public static ByteBuffer gameLoadHeap(String first, String... more) throws IOException {
        return load(resolve(gameRootDirectory(),first,more),false);
    }

    /**
     * Loads a file from the {@code USER_DATA} directory into a direct {@link ByteBuffer}.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a flipped direct {@code ByteBuffer} containing the file data.
     * @throws IOException if the file is missing, exceeds size limits, or is locked.
     */
    public static ByteBuffer userLoadDirect(String first, String... more) throws IOException {
        return load(resolve(userDataDirectory(),first,more),true);
    }

    /**
     * Loads a file from the {@code USER_DATA} directory into a heap {@link ByteBuffer}.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a flipped heap {@code ByteBuffer} containing the file data.
     * @throws IOException if the file is missing, exceeds size limits, or is locked.
     */
    public static ByteBuffer userLoadHeap(String first, String... more) throws IOException {
        return load(resolve(userDataDirectory(),first,more),false);
    }

    /**
     * Loads a file from the specified path into a direct {@link ByteBuffer}.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a flipped direct {@code ByteBuffer} containing the file data.
     * @throws IOException if the file is missing, exceeds size limits, or is locked.
     */
    public static ByteBuffer loadDirect(String first, String... more) throws IOException {
        return load(toPath(first,more),true);
    }

    /**
     * Loads a file from the specified path into a heap {@link ByteBuffer}.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a flipped heap {@code ByteBuffer} containing the file data.
     * @throws IOException if the file is missing, exceeds size limits, or is locked.
     */
    public static ByteBuffer loadHeap(String first, String... more) throws IOException {
        return load(toPath(first,more),false);
    }

    /**
     * Convenience method for reading a resource with a specified allocation type.
     * @param filePath    the validated resource path string.
     * @param directAlloc if {@code true}, uses direct memory; otherwise heap memory.
     * @return a flipped {@code ByteBuffer} containing the resource data.
     * @throws IOException if the resource cannot be found or read.
     */
    public static ByteBuffer resource(String filePath, boolean directAlloc) throws IOException {
        return resourcesRead(filePath,directAlloc);
    }

    /**
     * Convenience method for reading a filesystem file with a specified allocation type.
     * @param filePath    the target {@link Path}.
     * @param directAlloc if {@code true}, uses direct memory; otherwise heap memory.
     * @return a flipped {@code ByteBuffer} containing the file data.
     * @throws IOException if the file cannot be read after retries or exceeds size limits.
     */
    public static ByteBuffer load(Path filePath, boolean directAlloc) throws IOException {
        return fileSystemRead(filePath,directAlloc);
    }


    /**
     * Reads a file from the "resources" directory and returns its content as a byte array.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a {@code byte[]} containing the resource data.
     * @throws IOException if the resource is missing or unreadable.
     */
    public static byte[] resourceBytes(String first, String... more) throws IOException {
        return toArray(resourceHeap(first,more));
    }

    /**
     * Loads a file from {@code GAME_ROOT} and returns its content as a byte array.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a {@code byte[]} containing the file data.
     * @throws IOException if the file is missing or unreadable.
     */
    public static byte[] gameLoadBytes(String first, String... more) throws IOException {
        return loadBytes(resolve(gameRootDirectory(),first,more));
    }

    /**
     * Loads a file from {@code USER_DATA} and returns its content as a byte array.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a {@code byte[]} containing the file data.
     * @throws IOException if the file is missing or unreadable.
     */
    public static byte[] userLoadBytes(String first, String... more) throws IOException {
        return loadBytes(resolve(userDataDirectory(),first,more));
    }

    /**
     * Loads a file from the specified path segments and returns its content as a byte array.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a {@code byte[]} containing the file data.
     * @throws IOException if the file is missing or unreadable.
     */
    public static byte[] loadBytes(String first, String... more) throws IOException {
        return loadBytes(toPath(first,more));
    }

    /**
     * Loads a file from the specified {@link Path} and returns its content as a byte array.
     * @param filePath the path to the target file.
     * @return a {@code byte[]} containing the file data.
     * @throws IOException if the file is missing or unreadable.
     */
    public static byte[] loadBytes(Path filePath) throws IOException {
        return toArray(load(filePath,false));
    }


    /**
     * Reads a resource file as a UTF-8 encoded string.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return the file content as a {@code String}.
     * @throws IOException if the resource is missing or unreadable.
     */
    public static String resourceToString(String first, String... more) throws IOException {
        return new String(resourceBytes(first,more), StandardCharsets.UTF_8);
    }

    /**
     * Reads a file from {@code GAME_ROOT} as a UTF-8 encoded string.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return the file content as a {@code String}.
     * @throws IOException if the file is missing or unreadable.
     */
    public static String gameFileToString(String first, String... more) throws IOException {
        return fileToString(resolve(gameRootDirectory(),first,more));
    }

    /**
     * Reads a file from {@code USER_DATA} as a UTF-8 encoded string.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return the file content as a {@code String}.
     * @throws IOException if the file is missing or unreadable.
     */
    public static String userFileToString(String first, String... more) throws IOException {
        return fileToString(resolve(userDataDirectory(),first,more));
    }

    /**
     * Reads a file from the specified path segments as a UTF-8 encoded string.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return the file content as a {@code String}.
     * @throws IOException if the file is missing or unreadable.
     */
    public static String fileToString(String first, String... more) throws IOException {
        return fileToString(toPath(first, more));
    }

    /**
     * Reads a file from the specified {@link Path} as a UTF-8 encoded string.
     * @param filePath the path to the target file.
     * @return the file content as a {@code String}.
     * @throws IOException if the file is missing or unreadable.
     */
    public static String fileToString(Path filePath) throws IOException {
        return new String(loadBytes(filePath), StandardCharsets.UTF_8);
    }



    /**
     * Reads a resource file and splits it into a list of lines.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a list of lines contained in the resource.
     * @throws IOException if the resource is missing or unreadable.
     */
    public static List<String> resourceToLines(String first, String... more) throws IOException {
        return stringAsLines(resourceToString(first,more));
    }

    /**
     * Reads a file from {@code GAME_ROOT} and splits it into a list of lines.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a list of lines contained in the file.
     * @throws IOException if the file is missing or unreadable.
     */
    public static List<String> gameFileToLines(String first, String... more) throws IOException {
        return stringAsLines(gameFileToString(first,more));
    }

    /**
     * Reads a file from {@code USER_DATA} and splits it into a list of lines.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a list of lines contained in the file.
     * @throws IOException if the file is missing or unreadable.
     */
    public static List<String> userFileToLines(String first, String... more) throws IOException {
        return stringAsLines(userFileToString(first,more));
    }

    /**
     * Reads a file from the specified segments and splits it into a list of lines.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @return a list of lines contained in the file.
     * @throws IOException if the file is missing or unreadable.
     */
    public static List<String> fileToLines(String first, String... more) throws IOException {
        return stringAsLines(fileToString(first,more));
    }

    /**
     * Reads a file from the specified {@link Path} and splits it into a list of lines.
     * @param filePath the path to the target file.
     * @return a list of lines contained in the file.
     * @throws IOException if the file is missing or unreadable.
     */
    public static List<String> fileToLines(Path filePath) throws IOException {
        return stringAsLines(fileToString(filePath));
    }



    /**
     * Deserializes a resource JSON file into an object of the specified class.
     * @param clazz the class of the object to return.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @param <T>   the type of the deserialized object.
     * @return the deserialized object.
     * @throws IOException if the resource is missing, unreadable, or contains invalid JSON.
     */
    public static  <T> T resourceJson(Class<T> clazz, String first, String... more) throws IOException {
        return jsonToObject(gson(),clazz,resourceToString(first,more));
    }

    /**
     * Deserializes a JSON file from {@code GAME_ROOT} into an object of the specified class.
     * @param clazz the class of the object to return.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @param <T>   the type of the deserialized object.
     * @return the deserialized object.
     * @throws IOException if the file is missing, unreadable, or contains invalid JSON.
     */
    public static  <T> T gameLoadJson(Class<T> clazz, String first, String... more) throws IOException {
        return loadJson(clazz,resolve(gameRootDirectory(),first,more));
    }

    /**
     * Deserializes a JSON file from {@code USER_DATA} into an object of the specified class.
     * @param clazz the class of the object to return.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @param <T>   the type of the deserialized object.
     * @return the deserialized object.
     * @throws IOException if the file is missing, unreadable, or contains invalid JSON.
     */
    public static  <T> T userLoadJson(Class<T> clazz, String first, String... more) throws IOException {
        return loadJson(clazz,resolve(userDataDirectory(),first,more));
    }

    /**
     * Deserializes a JSON file from the specified segments into an object of the specified class.
     * @param clazz the class of the object to return.
     * @param first the first path segment.
     * @param more  optional additional path segments.
     * @param <T>   the type of the deserialized object.
     * @return the deserialized object.
     * @throws IOException if the file is missing, unreadable, or contains invalid JSON.
     */
    public static  <T> T loadJson(Class<T> clazz, String first, String... more) throws IOException {
        return loadJson(clazz,toPath(first,more));
    }

    /**
     * Deserializes a JSON file from the specified {@link Path} into an object of the specified class.
     * @param clazz    the class of the object to return.
     * @param filePath the path to the target JSON file.
     * @param <T>      the type of the deserialized object.
     * @return the deserialized object.
     * @throws IOException if the file is missing, unreadable, or contains invalid JSON.
     */
    public static  <T> T loadJson(Class<T> clazz, Path filePath) throws IOException {
        return jsonToObject(gson(),clazz,fileToString(filePath));
    }




    // **************************************************************************************
    //  WRITE / MODIFY / DELETE - OPERATIONS (USER DATA DIRECTORY ONLY)
    // **************************************************************************************




    /**
     * Atomically overwrites a file in {@code USER_DATA} with {@link ByteBuffer} content.
     * @param content the buffer containing data to write.
     * @param first   the first path segment relative to {@code USER_DATA}.
     * @param more    optional additional path segments.
     * @throws IOException if write access is denied or the I/O operation fails.
     */
    public static void userWrite(ByteBuffer content, String first, String... more) throws IOException {
        Path path = validateUserWriteAccess(resolve(userDataDirectory(),first,more));
        fileSystemWrite(content,path,false);
    }

    /**
     * Atomically overwrites a file in {@code USER_DATA} with {@code byte[]} content.
     * @param content the byte array to write.
     * @param first   the first path segment relative to {@code USER_DATA}.
     * @param more    optional additional path segments.
     * @throws IOException if write access is denied or the I/O operation fails.
     */
    public static void userWrite(byte[] content, String first, String... more) throws IOException {
        Objects.requireNonNull(content,"byte[] content is null");
        Path path = validateUserWriteAccess(resolve(userDataDirectory(),first,more));
        fileSystemWrite(ByteBuffer.wrap(content),path,false);
    }

    /**
     * Atomically overwrites a file in {@code USER_DATA} with a UTF-8 string.
     * @param content the string to write.
     * @param first   the first path segment relative to {@code USER_DATA}.
     * @param more    optional additional path segments.
     * @throws IOException if write access is denied or the I/O operation fails.
     */
    public static void userWrite(String content, String first, String... more) throws IOException {
        Objects.requireNonNull(content,"String content is null");
        Path path = validateUserWriteAccess(resolve(userDataDirectory(),first,more));
        fileSystemWrite(StandardCharsets.UTF_8.encode(content),path,false);
    }

    /**
     * Serializes an object to JSON and atomically overwrites a file in {@code USER_DATA}.
     * @param obj   the object to serialize.
     * @param first the first path segment relative to {@code USER_DATA}.
     * @param more  optional additional path segments.
     * @throws IOException if write access is denied or serialization fails.
     */
    public static void userWriteJson(Object obj, String first, String... more) throws IOException {
        Path path = validateUserWriteAccess(resolve(userDataDirectory(),first,more));
        String jsonString = objectToJson(gson(),obj);
        ByteBuffer content = StandardCharsets.UTF_8.encode(jsonString);
        fileSystemWrite(content,path,false);
    }

    /**
     * Appends {@link ByteBuffer} data directly to the end of a file in {@code USER_DATA}.
     * @param content the buffer containing data to append.
     * @param first   the first path segment relative to {@code USER_DATA}.
     * @param more    optional additional path segments.
     * @throws IOException if write access is denied or the append operation fails.
     */
    public static void userAppend(ByteBuffer content, String first, String... more) throws IOException {
        Path path = validateUserWriteAccess(resolve(userDataDirectory(),first,more));
        fileSystemWrite(content,path,true);
    }

    /**
     * Appends {@code byte[]} data directly to the end of a file in {@code USER_DATA}.
     * @param content the byte array to append.
     * @param first   the first path segment relative to {@code USER_DATA}.
     * @param more    optional additional path segments.
     * @throws IOException if write access is denied or the append operation fails.
     */
    public static void userAppend(byte[] content, String first, String... more) throws IOException {
        Objects.requireNonNull(content,"byte[] content is null");
        Path path = validateUserWriteAccess(resolve(userDataDirectory(),first,more));
        fileSystemWrite(ByteBuffer.wrap(content),path,true);
    }

    /**
     * Appends a UTF-8 string directly to the end of a file in {@code USER_DATA}.
     * @param content the string to append.
     * @param first   the first path segment relative to {@code USER_DATA}.
     * @param more    optional additional path segments.
     * @throws IOException if write access is denied or the append operation fails.
     */
    public static void userAppend(String content, String first, String... more) throws IOException {
        Objects.requireNonNull(content,"String content is null");
        Path path = validateUserWriteAccess(resolve(userDataDirectory(),first,more));
        fileSystemWrite(StandardCharsets.UTF_8.encode(content),path,true);
    }

    /**
     * Deletes a file or directory within {@code USER_DATA} recursively.
     * @param first the first path segment relative to {@code USER_DATA}.
     * @param more  optional additional path segments.
     * @throws IOException if the path points outside {@code USER_DATA} or deletion fails.
     */
    public static void userDelete(String first, String... more) throws IOException {
        Path path = validateUserWriteAccess(resolve(userDataDirectory(),first,more));
        fileSystemDelete(path);
    }




    // **************************************************************************************
    //  PUBLIC GETTERS
    // **************************************************************************************




    /**
     * @return {@code true} if the engine is running in a development environment
     * (IDE/Gradle), {@code false} if running from a packaged JAR.
     */
    public static boolean devMode() {
        return DEV_MODE;
    }

    /**
     * @return the absolute {@link Path} to the game's root directory (GAME_ROOT).
     */
    public static Path gameRootDirectory() {
        return GAME_ROOT;
    }

    /**
     * Construct valid a GAME_ROOT path.
     * @param first the first path segment.
     * @param more optional additional path segments.
     * @return absolute path resolved to GAME_ROOT directory
     * @throws IOException if the resulting path is invalid our point outside GAME_ROOT
     */
    public static Path gameRootDirectory(String first, String... more) throws IOException {
        Path root = gameRootDirectory();
        Path path = resolve(root,first,more);
        if (path.startsWith(root)) return path;
        throw new IOException("Path: \"" + path + "\" points outside GAME_ROOT");
    }

    /**
     * @return the absolute {@link Path} to the persistent user data directory (USER_DATA).
     */
    public static Path userDataDirectory() {
        return USER_DATA;
    }

    /**
     * Construct a valid USER_DATA path.
     * @param first the first path segment.
     * @param more optional additional path segments.
     * @return absolute path resolved to USER_DATA directory
     * @throws IOException if the resulting path is invalid our point outside USER_DATA
     */
    public static Path userDataDirectory(String first, String... more) throws IOException {
        Path root = userDataDirectory();
        Path path = resolve(root,first,more);
        if (path.startsWith(root)) return path;
        throw new IOException("Path: \"" + path + "\" points outside USER_DATA");
    }

    /**
     * Logs are outputed here if running from .jar / .exe.
     * When running from the IDEA / Development, logging is outputed to the console instead.
     * @return Path to the log file directory, should be {@code [USER_DATA]/logs}.
     */
    public static Path logOutputDirectory() {
        return USER_DATA.resolve("logs");
    }

    /**
     * @param path any absolute or relative path.
     * @return {@code true} if the path exists on disk and is NOT a symbolic link.
     */
    public static boolean pathExists(Path path) {
        return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
    }

    /**
     * @param path any absolute or relative path.
     * @return {@code true} if the path exists as a regular file and is NOT a symbolic link.
     */
    public static boolean pathIsFile(Path path) {
        return Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS);
    }

    /**
     * @param path any absolute or relative path.
     * @return {@code true} if the path exists as a directory and is NOT a symbolic link.
     */
    public static boolean pathIsDir(Path path) {
        return Files.isDirectory(path,LinkOption.NOFOLLOW_LINKS);
    }

    /**
     * @return shared / thread-safe {@link Gson} object.
     * Used for JSON read/write - operations.
     */
    public static Gson gson() {
        return GSON;
    }




    // **************************************************************************************
    //  BASE METHODS FOR: READ / WRITE / DELETE
    //  All public methods must use these (for consistency).
    // **************************************************************************************




    /**
     * Reads file from the build "resources" directory.
     * The method is not optimized for performance, but decent for loading text files
     * or small images/icons. As a general rule: Game assets should not be stored in "resources".
     * @param filePath resource path
     * @param directAlloc if true, uses direct memory; otherwise uses heap memory.
     * @return flipped ByteBuffer of raw data ready for read.
     * @throws IOException file not found or unable to read for any reason
     * @throws NullPointerException if filePath is null.
     */
    private static ByteBuffer resourcesRead(String filePath, boolean directAlloc) throws IOException {
        Objects.requireNonNull(filePath,"String filePath is null");
        filePath = filePath.startsWith("/") ? filePath : "/" + filePath;
        try (InputStream is = Disk.class.getResourceAsStream(filePath)) {
            if (is == null) throw new FileNotFoundException("Resource could not be found: " + filePath);
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = allocateBuffer(bytes.length, directAlloc);
            return buffer.put(bytes).flip();
        }
    }

    /**
     * Reads any external file into a ByteBuffer. Includes a retry mechanism
     * to mitigate temporary file locks (e.g., from Antivirus software).
     * @param filePath Path of the file.
     * @param directAlloc if true, uses direct memory; otherwise uses heap memory.
     * @return a flipped ByteBuffer containing the raw file data.
     * @throws IOException if the file is not found, not a regular file, exceeds
     * size limit or remains unreadable after multiple retries.
     * @throws NullPointerException if filePath is null.
     */
    private static ByteBuffer fileSystemRead(Path filePath, boolean directAlloc) throws IOException {
        Objects.requireNonNull(filePath,"Path filePath is null");
        if (!Files.isRegularFile(filePath,LinkOption.NOFOLLOW_LINKS))
            throw new FileNotFoundException("File not found or not a regular file: " + filePath);
        long initialSize = Files.size(filePath); // If the file is too large, it throws outside (never enters the loop).
        if (initialSize > Integer.MAX_VALUE) throw new IOException("File size (" + initialSize + " bytes) exceeds Integer.MAX_VALUE.");
        if (initialSize > MAX_FILE_SIZE) throw new IOException("File size (" + initialSize + " bytes) exceeds the maximum allowed limit: " + MAX_FILE_SIZE);
        final int maxRetries = 3;
        final int retryDelayMs = 8;
        IOException lastException = null;
        for (int i = 0; i < maxRetries; i++) {
            try (FileChannel channel = FileChannel.open(filePath, StandardOpenOption.READ)) {
                long currentSize = channel.size(); // no cap inside the retry loop (It's fine)
                ByteBuffer buffer = allocateBuffer((int) currentSize, directAlloc);
                while (buffer.hasRemaining()) {
                    if (channel.read(buffer) == -1) break;
                } return buffer.flip();
            } catch (IOException e) {
                lastException = e;
                if (i < maxRetries - 1) {
                    try { Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted during file load retry", ie);
                    }
                }
            }
        }
        throw lastException;
    }

    /**
     * Writes a {@code ByteBuffer} to the specified file path.
     * <p>
     * This method intelligently branches between three writing strategies:
     * <ul>
     * <li><b>Append:</b> Directly adds data to the end of an existing or new file.</li>
     * <li><b>New File:</b> Performs a direct write if the file does not already exist.</li>
     * <li><b>Atomic Overwrite:</b> If the file exists and {@code append} is false, it uses
     * a temporary "sidecar" file and an atomic move to prevent data corruption.</li>
     * </ul>
     * @param content  The {@code ByteBuffer} containing data to be written.
     * Position must be at the start of the data.
     * @param filePath The destination {@link Path}.
     * @param append   If true, appends to the file; if false, performs an atomic overwrite.
     * @throws IOException          If the directory cannot be created, the file cannot be
     * opened, or the I/O operation fails.
     * @throws NullPointerException if {@code content} or {@code filePath} is null.
     */
    private static void fileSystemWrite(ByteBuffer content, Path filePath, boolean append) throws IOException {
        Objects.requireNonNull(content, "ByteBuffer content is null");
        Objects.requireNonNull(filePath, "Path filePath is null");
        Path parent = filePath.getParent();
        if (parent != null) Files.createDirectories(parent);
        if (append) writeDirect(content, filePath, StandardOpenOption.APPEND);
        else { try { writeDirect(content, filePath, StandardOpenOption.CREATE_NEW);
            } catch (FileAlreadyExistsException e) {
                writeAtomic(content, filePath);
            }
        }
    }

    /**
     * Performs a low-level write operation using a {@link FileChannel}.
     * <p>
     * Ensures all remaining bytes in the buffer are written to the channel and
     * forces a synchronization with the storage device to minimize data loss.
     * </p>
     * @param content The data buffer to write.
     * @param path    The target file path.
     * @param mode    The {@link OpenOption} determining how the file is opened.
     * @throws IOException if an I/O error occurs during opening, writing, or forcing.
     */
    private static void writeDirect(ByteBuffer content, Path path, OpenOption mode) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE, mode)) {
            while (content.hasRemaining()) channel.write(content);
            channel.force(true); // Forced to wait
        }
    }

    /**
     * Safely overwrites an existing file by writing to a temporary file first.
     * <p>
     * This method generates a unique temporary filename using {@code System.nanoTime()}
     * to avoid collisions. Once the write is complete and forced to disk, it performs
     * an {@code ATOMIC_MOVE}. If the filesystem does not support atomic moves, it
     * falls back to a standard replacement move.
     * </p>
     * @param content  The data buffer to write.
     * @param filePath The final destination path.
     * @throws IOException if the temporary file cannot be written or the move fails.
     */
    private static void writeAtomic(ByteBuffer content, Path filePath) throws IOException {
        String tempName = filePath.getFileName().toString() + "." + System.nanoTime() + ".tmp";
        Path tempFile = filePath.resolveSibling(tempName);
        try { writeDirect(content, tempFile, StandardOpenOption.TRUNCATE_EXISTING);
            try { Files.move(tempFile, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) { Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tempFile); }
    }

    /**
     * Deletes a file or a directory (including all sub-contents) recursively.
     * Handles symbolic links safely by deleting the link itself rather than the target.
     * @param path the file or directory to delete.
     * @throws IOException if any file cannot be deleted (e.g., access denied or I/O error).
     * @throws NullPointerException if {@code path} is null.
     */
    private static void fileSystemDelete(Path path) throws IOException {
        Objects.requireNonNull(path, "Path path is null");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try { Files.delete(file);
                } catch (AccessDeniedException e) {
                    if (Platform.get() == Platform.WINDOWS) {
                        try { Files.setAttribute(file, "dos:readonly", false);
                            Files.delete(file);
                        } catch (Exception ex) { throw e; }
                    } else throw e;
                } return FileVisitResult.CONTINUE;
            } public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                try { Files.delete(dir);
                } catch (AccessDeniedException e) {
                    if (Platform.get() == Platform.WINDOWS) {
                        try { Files.setAttribute(dir, "dos:readonly", false);
                            Files.delete(dir);
                        } catch (Exception ex) { throw e; }
                    } else throw e;
                } return FileVisitResult.CONTINUE;
            }
        });
    }




    // **************************************************************************************
    //  HELPERS
    // **************************************************************************************




    /**
     * Resolve a relative path against a root directory and normalize the result.
     * <p>
     * This method joins the two paths and collapses any redundant {@code .} or
     * {@code ..} segments. It guards against environmental errors like invalid
     * character combinations or filesystem mismatches.
     * </p>
     * @param root the base directory to resolve against.
     * @param relative the relative path to be appended.
     * @return a normalized, resolved Path.
     * @throws IOException if the resulting path is invalid for the host OS, or
     * if the paths belong to different FileSystem providers.
     * @throws NullPointerException if either of the Path arguments are null.
     */
    private static Path resolve(Path root, Path relative) throws IOException {
        Objects.requireNonNull(root, "Root Path object is null");
        Objects.requireNonNull(relative, "Relative Path object is null");
        try { return root.resolve(relative).normalize();
        } catch (InvalidPathException e) {
            throw new IOException("The resulting path is invalid: " + relative, e);
        } catch (ProviderMismatchException e) {
            throw new IOException("FileSystem mismatch during resolution", e);
        }
    }

    /**
     * Resolve multiple path segments against a "root" directory.
     * <p>
     * This is a convenience helper that first converts the provided strings into
     * a Path before performing the resolution.
     * </p>
     * @param root the base directory (e.g. USER_DATA or GAME_ROOT).
     * @param first the first segment of the relative path.
     * @param more optional additional path segments.
     * @return a normalized, resolved Path.
     * @throws IOException if any segments contain illegal characters,
     * or if the resolution leads to an invalid system path.
     * @throws NullPointerException if any of the provided segments are null.
     */
    private static Path resolve(Path root, String first, String... more) throws IOException {
        return resolve(root, toPath(first, more));
    }

    /**
     * Converts path segments to a {@link Path}.
     * <p>
     * This method is consistent with {@link Path#of(String, String...)},
     * but converts internal {@link InvalidPathException}s into {@link IOException}s
     * to catch ANY user input (E.g. from a GUI input field)
     * </p>
     * @param first the first path segment.
     * @param more  additional path segments.
     * @return a constructed Path.
     * @throws IOException          if the path contains invalid characters.
     * @throws NullPointerException if any of the provided segments are null.
     */
    private static Path toPath(String first, String... more) throws IOException {
        nullCheckSegments(first,more);
        try { return Path.of(first, more);
        } catch (InvalidPathException e) {
            throw new IOException("Invalid path segments provided", e);
        }
    }

    /**
     * Joins multiple string segments into a single, platform-independent resource path.
     * <p>
     * This method concatenates segments with forward slashes.
     * before delegating the validation and normalization logic to {@link #parseResourcePath(String)}.
     * </p>
     * @param first the first path segment
     * @param more optional additional path segments.
     * @return a validated, root-relative resource path string starting with "/".
     * @throws IOException the resulting path is invalid.
     * @throws NullPointerException if any of the provided segments are null.
     */
    private static String toResourcePath(String first, String... more) throws IOException {
        nullCheckSegments(first,more);
        String path;
        if (more.length == 0) {
            path = first;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(first);
            for (String segment : more) {
                if (!segment.isEmpty()) {
                    if (!sb.isEmpty())
                        sb.append('/');
                    sb.append(segment);
                }
            } path = sb.toString();
        } return parseResourcePath(path);
    }

    /**
     * Validate and normalize a resource path string for use with {@link Class#getResourceAsStream(String)}.
     * Use in tandem with {@link #toResourcePath(String, String...)} (Internal use only)
     * <p>
     * This method enforces three core rules:
     * <ul>
     * <li>Separators are standardized to forward slashes ({@code /}).</li>
     * <li>Redundant segments ({@code .}, {@code ..}) are collapsed via {@link Path#normalize()}.</li>
     * <li>Any path attempting to escape the classpath root (e.g., starting with {@code ..}) is rejected.</li>
     * </ul>
     * </p>
     * @param path the joined path string to validate.
     * @return a normalized path string with a mandatory leading slash.
     * @throws IOException if the path is malformed, blank, or violates root-access security rules.
     */
    private static String parseResourcePath(String path) throws IOException {
        String standardized = path.trim().replace('\\', '/');
        while (standardized.startsWith("/")) {
            standardized = standardized.substring(1);
        } // Catch cases where the input was just "/" or "   "
        if (standardized.isBlank()) {
            throw new IOException("Resource path is blank or resolves to root (\"/\")");
        } try { //  Use Path API to normalize "." and ".." tokens
            Path normalized = Path.of(standardized).normalize();
            // Convert back to String and ensure forward slashes (Path.toString() is OS-dependent)
            String result = normalized.toString().replace('\\', '/');
            // Detailed Validation
            // isEmpty(): user provided something like "folder/.."
            // equals("."): user provided "."
            // startsWith(".."): user provided "../../secret.txt"
            if (result.isEmpty() || result.equals(".") || result.startsWith("..")) {
                throw new IOException("Invalid resource path (escapes root or invalid target): " + path);
            } return "/" + result;
        } catch (InvalidPathException e) {
            throw new IOException("Malformed resource path: " + path, e);
        }
    }

    /**
     * Early null check path segments. To make sure the API stays consistent.
     * Called before constructing {@link Path} objects OR resource {@link String} paths internally.
     * @param first the first path segment
     * @param more optional additional path segments.
     * @throws NullPointerException if "first" is null or any of the "more" segments (when provided) are null.
     */
    private static void nullCheckSegments(String first, String... more) {
        Objects.requireNonNull(first,"Null path segment: \"first\".");
        Objects.requireNonNull(more,"Null path varArgs: \"...more\".");
        for (String segment : more) {
            Objects.requireNonNull(segment,"One or more null path segments: \"...more\".");
        }
    }

    /**
     * @param gson {@link Gson} instance used for conversion.
     * @param clazz {@code Class<T>} of the expected object
     * @param json A JSON-formatted string representing an instance of {@code Class<T>}.
     * @return The expected instance of {@code Class<T>} OR null if the Json-string argument is empty
     * @throws IOException if the Json-string is not a valid representation for an object of type classOf
     * @throws NullPointerException if any of the arguments are null
     */
    private static <T> T jsonToObject(Gson gson, Class<T> clazz, String json) throws IOException {
        Objects.requireNonNull(clazz,"Class argument is null");
        Objects.requireNonNull(json,"Json-string argument is null");
        Objects.requireNonNull(gson,"Gson object argument is null");
        try { return gson.fromJson(json,clazz);
        } catch (JsonSyntaxException e) { throw new IOException(e); }
    }

    /**
     * @param gson {@link Gson} instance used for conversion.
     * @param obj object for conversion
     * @return Json-formatted String representing {@code obj}.
     */
    private static String objectToJson(Gson gson, Object obj) {
        Objects.requireNonNull(obj,"Object argument is null");
        Objects.requireNonNull(gson,"Gson object argument is null");
        return gson.toJson(obj);
    }

    /**
     * Converts a ByteBuffer to a byte array.
     * <p>
     * This method attempts to return the buffer's underlying array if possible (zero-copy).
     * Otherwise, it copies the remaining bytes into a new array.
     * </p>
     * @param buffer the buffer to convert.
     * @return the byte array containing the remaining bytes.
     * @throws NullPointerException if the buffer is null.
     */
    private static byte[] toArray(ByteBuffer buffer) {
        Objects.requireNonNull(buffer,"Null ByteBuffer to byte array.");
        if (buffer.hasArray()) {
            byte[] array = buffer.array();
            if (buffer.arrayOffset() == 0 && buffer.remaining() == array.length) return array;
        } byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    /**
     * Splits a string into a mutable list of lines.
     * <p>
     * This method splits the string using the regular expression {@code \\R},
     * which matches any Unicode line break sequence (such as \n, \r, and \r\n).
     * </p>
     * @param string the string to split; may be null or empty.
     * @return a mutable {@link ArrayList} containing the lines of the string.
     */
    private static List<String> stringAsLines(String string) {
        return (string == null || string.isEmpty())
                ? new ArrayList<>()
                : new ArrayList<>(Arrays.asList(string.split("\\R", -1)));
    }

    /**
     * Makes sure the path is within the game's user data directory
     * @param path absolute path
     * @return the same path
     * @throws AccessDeniedException if path leads outside of user data directory.
     */
    private static Path validateUserWriteAccess(Path path) throws AccessDeniedException {
        if (!path.startsWith(USER_DATA)) {
            throw new AccessDeniedException("user path leads outside the user data directory: " + path);
        } return path;
    }

    /**
     * Allocates a {@link ByteBuffer} of the specified size.
     * @param size        the capacity of the buffer.
     * @param directAlloc if {@code true}, allocates direct memory; otherwise heap memory.
     * @return the allocated {@code ByteBuffer}.
     */
    private static ByteBuffer allocateBuffer(int size, boolean directAlloc) {
        return directAlloc ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
    }

}
