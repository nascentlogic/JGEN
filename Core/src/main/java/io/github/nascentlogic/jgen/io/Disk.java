package io.github.nascentlogic.jgen.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.nascentlogic.jgen.gfx.Bitmap;
import io.github.nascentlogic.jgen.gfx.Color;
import io.github.nascentlogic.jgen.gfx.Shader;
import io.github.nascentlogic.jgen.gui.Font;
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
import java.util.stream.Stream;

/**
 * F.Dahl, 8/27/2026
 */
public class Disk {

    /** Size limit (in bytes) for reading files (internal ByteBuffer allocation).
     * Assume reading larger files throw {@link IOException}. */
    public static final long MAX_FILE_SIZE = 256 * 1024 * 1024;
    private static volatile boolean INITIALIZED;
    private static boolean DEV_MODE;
    private static Path GAME_ROOT;
    private static Path USER_DATA;
    private static Path USER_CACHE;
    private static Gson GSON;


    // =============================================================================
    // FONTS
    // =============================================================================


    public static List<Font> userLoadFonts(String first, String... more) throws IOException {
        return loadFonts(resolveConfine(USER_DATA,first, more));
    }

    public static List<Font> userLoadFonts(Path path) throws IOException {
        return loadFonts(resolveConfine(USER_DATA,path));
    }

    public static List<Font> gameLoadFonts(String first, String... more) throws IOException {
        return loadFonts(resolveConfine(GAME_ROOT,first, more));
    }

    public static List<Font> gameLoadFonts(Path path) throws IOException {
        return loadFonts(resolveConfine(GAME_ROOT,path));
    }

    public static List<Font> loadFonts(String first, String... more) throws IOException {
        return loadFonts(toPath(first, more));
    }

    public static List<Font> loadFonts(Path path) throws IOException {
        FileToken directory = FileToken.of(path); // exist or throw
        List<FileToken> files = directory.listFilesInDir( // dir or throw
                t -> (!t.isDirectory && t.extension.equals(".ttf")));
        if (files.isEmpty()) return List.of();
        Path cacheDir = USER_CACHE.resolve("font");

        List<Font> fonts = new ArrayList<>();
        for (FileToken file : files) {
            Path cachePng = cacheDir.resolve(file.name +".png");
            Path cacheJson = cacheDir.resolve(file.name +".json");
            if (Files.exists(cachePng) && Files.exists(cacheJson)) {
                try {
                    Font font = loadJson(Font.class,cacheJson);
                    Bitmap bitmap = loadImage(cachePng);
                    font.setBitmap(bitmap);
                    fonts.add(font);
                    continue;
                } catch (IOException e) {
                    Logger.warn(e,"Failed to load cached font: {}",file.name);
                }
            }
            Font font; // No font in cache. Generate font
            ByteBuffer ttf = load(file.toPath(),true);
            try {
                font = Font.generate(file.name, ttf);
                fonts.add(font);
            } catch (Exception e) {
                Logger.warn(e,"Failed to generate font: {}",file.name);
                continue;
            }

            try {  // Cache the generated font
                writeJson(font,cacheJson);
                saveImage(font.bitmap(),cachePng);
            } catch (IOException e) {
                Logger.warn(e,"Failed to cache font: {}", file.name);
            }
        } return fonts;
    }

    // all fonts are cached under the same directory, no matter where they are loaded from
    public static Font resourceFont(String first, String... more) throws IOException {
        ResourcePath resourcePath = new ResourcePath(first,more);
        if (!resourcePath.extension().equals(".ttf"))
            throw new IOException("Font path is not a .ttf: \"" + resourcePath.path() + "\"");
        String name = resourcePath.name();
        Path cacheDir = USER_CACHE.resolve("font");
        Path cachePng = cacheDir.resolve(name +".png");
        Path cacheJson = cacheDir.resolve(name +".json");
        if (Files.exists(cachePng) && Files.exists(cacheJson)) {
            try {
                Font font = loadJson(Font.class,cacheJson);
                Bitmap bitmap = loadImage(cachePng);
                font.setBitmap(bitmap);
                return font;
            } catch (IOException e) {
                Logger.warn(e,"Failed to load cached font: {}",name);
            }
        }
        Font font; // No font in cache. Generate font
        ByteBuffer ttf = resource(resourcePath,true);
        try { font = Font.generate(name,ttf);
        } catch (Exception e) {
            throw new IOException(e);
        }
        // Cache the generated font
        try { writeJson(font,cacheJson);
            saveImage(font.bitmap(),cachePng);
        } catch (IOException e) {
            Logger.warn(e,"Failed to cache font: {}", name);
        } return font;
    }

    public static Font loadFont(String first, String... more) throws IOException {
        return loadFont(toPath(first, more));
    }

    public static Font loadFont(Path path) throws IOException {
        FileToken pathToken = FileToken.of(path); // exist or throw
        Path absolute = pathToken.toPath();
        if (!pathToken.extension.equals(".ttf"))
            throw new IOException("Font path is not a .ttf: \"" + pathToken + "\"");
        if (absolute.startsWith(USER_CACHE))
            throw new IOException("Cannot load Font directly from cache");
        String name = pathToken.name;
        Path cacheDir = USER_CACHE.resolve("font");
        Path cachePng = cacheDir.resolve(name +".png");
        Path cacheJson = cacheDir.resolve(name +".json");
        if (Files.exists(cachePng) && Files.exists(cacheJson)) {
            try {
                Font font = loadJson(Font.class,cacheJson);
                Bitmap bitmap = loadImage(cachePng);
                font.setBitmap(bitmap);
                return font;
            } catch (IOException e) {
                Logger.warn(e,"Failed to load cached font: {}",name);
            }
        }
        Font font; // No font in cache. Generate font
        ByteBuffer ttf = load(absolute,true);
        try { font = Font.generate(name,ttf);
        } catch (Exception e) {
            throw new IOException(e);
        }
        // Cache the generated font
        try { writeJson(font,cacheJson);
            saveImage(font.bitmap(),cachePng);
        } catch (IOException e) {
            Logger.warn(e,"Failed to cache font: {}", name);
        }

        return font;

    }

    public static Font userLoadFont(String first, String... more) throws IOException {
        return loadFont(resolveConfine(USER_DATA,first,more));
    }

    public static Font userLoadFont(Path path) throws IOException {
        return loadFont(resolveConfine(USER_DATA,path));
    }

    public static Font gameLoadFont(String first, String... more) throws IOException {
        return loadFont(resolveConfine(GAME_ROOT,first,more));
    }

    public static Font gameLoadFont(Path path) throws IOException {
        return loadFont(resolveConfine(GAME_ROOT,path));
    }


    // =============================================================================
    // SHADERS
    // =============================================================================

    public static Shader resourceShader(String name, String first, String... more) throws IOException {
        if (Objects.requireNonNull(name,"Shader name is null").isBlank())
            throw new IOException("Shader name cannot be blank");
        ResourcePath directory = new ResourcePath(first, more);
        final Shader.File[] files = new Shader.File[Shader.Type.array.length];
        for (int i = 0; i < files.length; i++) {
            Shader.Type type = Shader.Type.array[i];
            String filePath = directory.path() + "/" + name + type.extension;
            try { String sourceCode = resourceString(filePath);
                files[i] = new Shader.File(type, sourceCode);
            } catch (IOException ignored) { /* */ }
        } Shader shader = new Shader(name, files);
        if (!shader.isComplete()) throw new IOException("Incomplete shader: \"" + name + "\"");
        return shader;
    }

    public static Shader loadShader(String name, String first, String... more) throws IOException {
        return loadShader(name,toPath(first, more));
    }

    public static Shader loadShader(String name, Path path) throws IOException {
        if (Objects.requireNonNull(name,"Shader name is null").isBlank())
            throw new IOException("Shader name cannot be blank");
        Path directory = Objects.requireNonNull(path,"Path is null").toAbsolutePath().normalize();
        if (!Files.isDirectory(directory)) throw new NotDirectoryException(directory.toString());
        final Shader.File[] files = new Shader.File[Shader.Type.array.length];
        for (int i = 0; i < files.length; i++) {
            Shader.Type type = Shader.Type.array[i];
            Path filePath = directory.resolve("/" + name + type.extension);
            if (!Files.exists(filePath)) continue;
            files[i] = new Shader.File(type,loadString(filePath));
        } Shader shader = new Shader(name, files);
        if (!shader.isComplete()) throw new IOException("Incomplete shader: \"" + name + "\"");
        return shader;
    }

    public static Shader userLoadShader(String name, String first, String... more) throws IOException {
        return loadShader(name,resolveConfine(USER_DATA,first,more));
    }

    public static Shader userLoadShader(String name, Path path) throws IOException {
        return loadShader(name,resolveConfine(USER_DATA,path));
    }

    public static Shader gameLoadShader(String name, String first, String... more) throws IOException {
        return loadShader(name,resolveConfine(GAME_ROOT,first,more));
    }

    public static Shader gameLoadShader(String name, Path path) throws IOException {
        return loadShader(name,resolveConfine(GAME_ROOT,path));
    }

    public static Shader cacheLoadShader(String name, String first, String... more) throws IOException {
        return loadShader(name,resolveConfine(USER_CACHE,first,more));
    }

    public static Shader cacheLoadShader(String name, Path path) throws IOException {
        return loadShader(name,resolveConfine(USER_CACHE,path));
    }

    public static List<Shader> loadShaders(String first, String... more) throws IOException {
        return loadShaders(toPath(first, more));
    }

    public static List<Shader> loadShaders(Path path) throws IOException {
        FileToken directory = FileToken.of(path);
        final Map<String, Shader.File[]> map = new HashMap<>();
        final Shader.Type[] types = Shader.Type.array;
        try (Stream<FileToken> stream = directory.streamDirectory()) {
            stream.filter(t -> !t.isDirectory).forEach(file -> {
                for (Shader.Type type : types) {
                    if (file.extension.equals(type.extension)) {
                        try { String sourceCode = loadString(file.toPath());
                            Shader.File[] files = map.computeIfAbsent(file.name, k -> new Shader.File[3]);
                            files[type.ordinal()] = new Shader.File(type, sourceCode);
                        } catch (IOException e) { Logger.warn(e); }
                        break;
                    }
                }
            });
        } if (map.isEmpty()) return List.of();
        List<Shader> list = new ArrayList<>(map.size());
        var entrySet = map.entrySet();
        for (var entry : entrySet) {
            Shader shader = new Shader(entry.getKey(), entry.getValue());
            if (shader.isComplete()) list.add(shader);
            else Logger.warn("Shader: \"{}\", missing file/s",shader.name());
        } return list;
    }

    public static List<Shader> userLoadShaders(String first, String... more) throws IOException {
        return loadShaders(resolveConfine(USER_DATA,first, more));
    }

    public static List<Shader> userLoadShaders(Path path) throws IOException {
        return loadShaders(resolveConfine(USER_DATA,path));
    }

    public static List<Shader> gameLoadShaders(String first, String... more) throws IOException {
        return loadShaders(resolveConfine(GAME_ROOT,first, more));
    }

    public static List<Shader> gameLoadShaders(Path path) throws IOException {
        return loadShaders(resolveConfine(GAME_ROOT,path));
    }

    public static List<Shader> cacheLoadShaders(String first, String... more) throws IOException {
        return loadShaders(resolveConfine(USER_CACHE,first, more));
    }

    public static List<Shader> cacheLoadShaders(Path path) throws IOException {
        return loadShaders(resolveConfine(USER_CACHE,path));
    }

    // =============================================================================
    // IMAGES
    // =============================================================================


    public static Bitmap resourceImage(String first, String... more) throws IOException {
        return new Bitmap(resourceDirect(first, more));
    }

    public static Bitmap loadImage(String first, String... more) throws IOException {
        return new Bitmap(loadDirect(first, more));
    }

    public static Bitmap loadImage(Path path) throws IOException {
        return new Bitmap(loadDirect(path));
    }

    public static Bitmap userLoadImage(String first, String... more) throws IOException {
        return new Bitmap(userLoadDirect(first, more));
    }

    public static Bitmap userLoadImage(Path path) throws IOException {
        return new Bitmap(userLoadDirect(path));
    }

    public static Bitmap gameLoadImage(String first, String... more) throws IOException {
        return new Bitmap(gameLoadDirect(first, more));
    }

    public static Bitmap gameLoadImage(Path path) throws IOException {
        return new Bitmap(gameLoadDirect(path));
    }

    public static Bitmap cacheLoadImage(String first, String... more) throws IOException {
        return new Bitmap(cacheLoadDirect(first, more));
    }

    public static Bitmap cacheLoadImage(Path path) throws IOException {
        return new Bitmap(cacheLoadDirect(path));
    }

    public static void saveImage(Bitmap bitmap, String first, String... more) throws IOException {
        write(Objects.requireNonNull(bitmap, "Bitmap is null").compress(),first,more);
    }

    public static void saveImage(Bitmap bitmap, Path path) throws IOException {
        write(Objects.requireNonNull(bitmap, "Bitmap is null").compress(),path);
    }

    public static void userSaveImage(Bitmap bitmap, String first, String... more) throws IOException {
        userWrite(Objects.requireNonNull(bitmap, "Bitmap is null").compress(),first,more);
    }

    public static void userSaveImage(Bitmap bitmap, Path path) throws IOException {
        userWrite(Objects.requireNonNull(bitmap, "Bitmap is null").compress(),path);
    }

    public static void gameSaveImage(Bitmap bitmap, String first, String... more) throws IOException {
        gameWrite(Objects.requireNonNull(bitmap, "Bitmap is null").compress(),first,more);
    }

    public static void gameSaveImage(Bitmap bitmap, Path path) throws IOException {
        gameWrite(Objects.requireNonNull(bitmap, "Bitmap is null").compress(),path);
    }

    public static void cacheSaveImage(Bitmap bitmap, String first, String... more) throws IOException {
        cacheWrite(Objects.requireNonNull(bitmap, "Bitmap is null").compress(),first,more);
    }

    public static void cacheSaveImage(Bitmap bitmap, Path path) throws IOException {
        cacheWrite(Objects.requireNonNull(bitmap, "Bitmap is null").compress(),path);
    }


    // =============================================================================
    // JSON
    // =============================================================================


    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T resourceJson(Class<T> clazz, String first, String... more) throws IOException {
        return resourceJson(clazz,GSON,first,more);
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T resourceJson(Class<T> clazz, Gson gson, String first, String... more) throws IOException {
        Objects.requireNonNull(clazz, "Class is null");
        Objects.requireNonNull(gson, "Gson is null");
        String jsonString = resourceString(first, more);
        if (jsonString.isBlank()) throw new IOException("Cannot deserialize JSON: Resource is blank");
        T object;
        try { object = gson.fromJson(jsonString, clazz);
        } catch (RuntimeException e) {
            throw new IOException("Failed to parse JSON content from resource", e);
        } if (object == null) throw new IOException("Gson returned null while deserializing resource");
        return object;
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T userLoadJson(Class<T> clazz, String first, String... more) throws IOException {
        return loadJson(clazz,GSON,resolveConfine(USER_DATA,first,more));
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T userLoadJson(Class<T> clazz, Gson gson, String first, String... more) throws IOException {
        return loadJson(clazz,gson,resolveConfine(USER_DATA,first,more));
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T userLoadJson(Class<T> clazz, Path path) throws IOException {
        return loadJson(clazz,GSON,resolveConfine(USER_DATA,path));
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T userLoadJson(Class<T> clazz, Gson gson, Path path) throws IOException {
        return loadJson(clazz,gson,resolveConfine(USER_DATA,path));
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T gameLoadJson(Class<T> clazz, String first, String... more) throws IOException {
        return loadJson(clazz,GSON,resolveConfine(GAME_ROOT,first,more));
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T gameLoadJson(Class<T> clazz, Gson gson, String first, String... more) throws IOException {
        return loadJson(clazz,gson,resolveConfine(GAME_ROOT,first,more));
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T gameLoadJson(Class<T> clazz, Path path) throws IOException {
        return loadJson(clazz,GSON,resolveConfine(GAME_ROOT,path));
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T gameLoadJson(Class<T> clazz, Gson gson, Path path) throws IOException {
        return loadJson(clazz,gson,resolveConfine(GAME_ROOT,path));
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T cacheLoadJson(Class<T> clazz, String first, String... more) throws IOException {
        return loadJson(clazz,GSON,resolveConfine(USER_CACHE,first,more));
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T cacheLoadJson(Class<T> clazz, Gson gson, String first, String... more) throws IOException {
        return loadJson(clazz,gson,resolveConfine(USER_CACHE,first,more));
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T cacheLoadJson(Class<T> clazz, Path path) throws IOException {
        return loadJson(clazz,GSON,resolveConfine(USER_CACHE,path));
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T cacheLoadJson(Class<T> clazz, Gson gson, Path path) throws IOException {
        return loadJson(clazz,gson,resolveConfine(USER_CACHE,path));
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T loadJson(Class<T> clazz, Path path) throws IOException {
        return loadJson(clazz,GSON,path);
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T loadJson(Class<T> clazz, String first, String... more) throws IOException {
        return loadJson(clazz,GSON,first,more);
    }

    /** @see #loadJson(Class, Gson, Path) */
    public static <T> T loadJson(Class<T> clazz, Gson gson, String first, String... more) throws IOException {
        return loadJson(clazz,gson,toPath(first, more));
    }

    /**
     * Deserializes JSON content from a file into an object of the specified class.
     * @param <T>   the target type.
     * @param clazz the class of {@code T}; must not be {@code null}.
     * @param gson  the {@link Gson} instance to use; must not be {@code null}.
     * @param path  the target file path; must not be {@code null}.
     * @return the deserialized object instance; never {@code null}.
     * @throws NullPointerException if {@code clazz}, {@code gson}, or {@code path} is {@code null}.
     * @throws IOException          if an I/O error occurs, if the file is empty/invalid JSON,
     *                              or if deserialization yields {@code null}.
     */
    public static <T> T loadJson(Class<T> clazz, Gson gson, Path path) throws IOException {
        Objects.requireNonNull(clazz, "Class is null");
        Objects.requireNonNull(gson, "Gson is null");
        ByteBuffer buffer = loadHeap(path);
        String jsonString = StandardCharsets.UTF_8.decode(buffer).toString();
        if (jsonString.isBlank()) throw new IOException("Cannot deserialize JSON: File is empty or contains only whitespace: \"" + path + "\"");
        T object;
        try { object = gson.fromJson(jsonString, clazz);
        } catch (RuntimeException e) {
            throw new IOException("Failed to parse JSON content from: \"" + path + "\"", e);
        } if (object == null) throw new IOException("Gson returned null while deserializing path: \"" + path + "\"");
        return object;
    }



    /** @see #writeJson(Object, Gson, Path) */
    public static void writeJson(Object obj, String first, String... more) throws IOException {
        writeJson(obj,GSON,first,more);
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void writeJson(Object obj, Path path) throws IOException {
        writeJson(obj,GSON,path);
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void writeJson(Object obj, Gson gson, String first, String... more) throws IOException {
        writeJson(obj,gson,toPath(first, more));
    }

    /**
     * Serializes an object to JSON using the provided {@link Gson} instance and writes it to a file.
     * <p> The destination file is overwritten atomically if it already exists. Missing parent
     * directories are created automatically before writing.</p>
     * @param obj  the object to serialize to JSON; must not be {@code null}.
     * @param gson the {@link Gson} instance to use for serialization; must not be {@code null}.
     * @param path the target file path; must not be {@code null}.
     * @throws NullPointerException if {@code obj}, {@code gson}, or {@code path} is {@code null}.
     * @throws IOException          if JSON serialization fails or an I/O error occurs while writing.
     */
    public static void writeJson(Object obj, Gson gson, Path path) throws IOException {
        Objects.requireNonNull(obj, "Object is null");
        Objects.requireNonNull(gson, "Gson is null");
        String jsonString;
        try { jsonString = gson.toJson(obj);
        } catch (RuntimeException e) {
            throw new IOException("Failed to serialize object to JSON", e);
        } ByteBuffer content = StandardCharsets.UTF_8.encode(jsonString);
        write(content, path, false);
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void userWriteJson(Object obj, String first, String... more) throws IOException {
        writeJson(obj,resolveConfine(USER_DATA,first,more));
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void userWriteJson(Object obj, Path path) throws IOException {
        writeJson(obj,resolveConfine(USER_DATA,path));
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void userWriteJson(Object obj, Gson gson, String first, String... more) throws IOException {
        writeJson(obj,gson,resolveConfine(USER_DATA,first,more));
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void userWriteJson(Object obj, Gson gson, Path path) throws IOException {
        writeJson(obj,gson,resolveConfine(USER_DATA,path));
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void gameWriteJson(Object obj, String first, String... more) throws IOException {
        writeJson(obj,resolveConfine(GAME_ROOT,first,more));
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void gameWriteJson(Object obj, Path path) throws IOException {
        writeJson(obj,resolveConfine(GAME_ROOT,path));
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void gameWriteJson(Object obj, Gson gson, String first, String... more) throws IOException {
        writeJson(obj,gson,resolveConfine(GAME_ROOT,first,more));
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void gameWriteJson(Object obj, Gson gson, Path path) throws IOException {
        writeJson(obj,gson,resolveConfine(GAME_ROOT,path));
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void cacheWriteJson(Object obj, String first, String... more) throws IOException {
        writeJson(obj,resolveConfine(USER_CACHE,first,more));
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void cacheWriteJson(Object obj, Path path) throws IOException {
        writeJson(obj,resolveConfine(USER_CACHE,path));
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void cacheWriteJson(Object obj, Gson gson, String first, String... more) throws IOException {
        writeJson(obj,gson,resolveConfine(USER_CACHE,first,more));
    }

    /** @see #writeJson(Object, Gson, Path) */
    public static void cacheWriteJson(Object obj, Gson gson, Path path) throws IOException {
        writeJson(obj,gson,resolveConfine(USER_CACHE,path));
    }


    // =============================================================================
    // STRING
    // =============================================================================

    public static List<String> resourceAsLines(String first, String... more) throws IOException {
        return stringAsLines(resourceString(first, more));
    }

    public static List<String> asLines(String first, String... more) throws IOException {
        return stringAsLines(loadString(first, more));
    }

    public static List<String> asLines(Path path) throws IOException {
        return stringAsLines(loadString(path));
    }

    public static List<String> userAsLines(String first, String... more) throws IOException {
        return stringAsLines(userLoadString(first, more));
    }

    public static List<String> userAsLines(Path path) throws IOException {
        return stringAsLines(userLoadString(path));
    }

    public static List<String> gameAsLines(String first, String... more) throws IOException {
        return stringAsLines(gameLoadString(first, more));
    }

    public static List<String> gameAsLines(Path path) throws IOException {
        return stringAsLines(gameLoadString(path));
    }

    public static List<String> cacheAsLines(String first, String... more) throws IOException {
        return stringAsLines(cacheLoadString(first, more));
    }

    public static List<String> cacheAsLines(Path path) throws IOException {
        return stringAsLines(cacheLoadString(path));
    }

    public static String resourceString(String first, String... more) throws IOException {
        return new String(resourceBytes(first, more), StandardCharsets.UTF_8);
    }

    public static String loadString(String first, String... more) throws IOException {
        return new String(loadBytes(first, more), StandardCharsets.UTF_8);
    }

    public static String loadString(Path path) throws IOException {
        return new String(loadBytes(path), StandardCharsets.UTF_8);
    }

    public static String userLoadString(String first, String... more) throws IOException {
        return new String(userLoadBytes(first, more), StandardCharsets.UTF_8);
    }

    public static String userLoadString(Path path) throws IOException {
        return new String(userLoadBytes(path), StandardCharsets.UTF_8);
    }

    public static String gameLoadString(String first, String... more) throws IOException {
        return new String(gameLoadBytes(first, more), StandardCharsets.UTF_8);
    }

    public static String gameLoadString(Path path) throws IOException {
        return new String(gameLoadBytes(path), StandardCharsets.UTF_8);
    }

    public static String cacheLoadString(String first, String... more) throws IOException {
        return new String(cacheLoadBytes(first, more), StandardCharsets.UTF_8);
    }

    public static String cacheLoadString(Path path) throws IOException {
        return new String(cacheLoadBytes(path), StandardCharsets.UTF_8);
    }



    public static void writeString(String content, String first, String... more) throws IOException {
        writeBytes(stringBytes(content),first,more);
    }

    public static void writeString(String content, Path path) throws IOException {
        writeBytes(stringBytes(content),path);
    }

    public static void appendString(String content, String first, String... more) throws IOException {
        appendBytes(stringBytes(content),first,more);
    }

    public static void appendString(String content, Path path) throws IOException {
        appendBytes(stringBytes(content),path);
    }

    public static void userWriteString(String content, String first, String... more) throws IOException {
        userWriteBytes(stringBytes(content),first,more);
    }

    public static void userWriteString(String content, Path path) throws IOException {
        userWriteBytes(stringBytes(content),path);
    }

    public static void userAppendString(String content, String first, String... more) throws IOException {
        userAppendBytes(stringBytes(content),first,more);
    }

    public static void userAppendString(String content, Path path) throws IOException {
        userAppendBytes(stringBytes(content),path);
    }

    public static void gameWriteString(String content, String first, String... more) throws IOException {
        gameWriteBytes(stringBytes(content),first,more);
    }

    public static void gameWriteString(String content, Path path) throws IOException {
        gameWriteBytes(stringBytes(content),path);
    }

    public static void gameAppendString(String content, String first, String... more) throws IOException {
        gameAppendBytes(stringBytes(content),first,more);
    }

    public static void gameAppendString(String content, Path path) throws IOException {
        gameAppendBytes(stringBytes(content),path);
    }

    public static void cacheWriteString(String content, String first, String... more) throws IOException {
        cacheWriteBytes(stringBytes(content),first,more);
    }

    public static void cacheWriteString(String content, Path path) throws IOException {
        cacheWriteBytes(stringBytes(content),path);
    }

    public static void cacheAppendString(String content, String first, String... more) throws IOException {
        cacheAppendBytes(stringBytes(content),first,more);
    }

    public static void cacheAppendString(String content, Path path) throws IOException {
        cacheAppendBytes(stringBytes(content),path);
    }

    // =============================================================================
    // BYTE ARRAY
    // =============================================================================

    public static byte[] resourceBytes(String first, String... more) throws IOException {
        return toArray(resourceHeap(first, more));
    }

    public static byte[] loadBytes(String first, String... more) throws IOException {
        return toArray(loadHeap(first, more));
    }

    public static byte[] loadBytes(Path path) throws IOException {
        return toArray(loadHeap(path));
    }

    public static byte[] userLoadBytes(String first, String... more) throws IOException {
        return toArray(userLoadHeap(first, more));
    }

    public static byte[] userLoadBytes(Path path) throws IOException {
        return toArray(userLoadHeap(path));
    }

    public static byte[] gameLoadBytes(String first, String... more) throws IOException {
        return toArray(gameLoadHeap(first, more));
    }

    public static byte[] gameLoadBytes(Path path) throws IOException {
        return toArray(gameLoadHeap(path));
    }

    public static byte[] cacheLoadBytes(String first, String... more) throws IOException {
        return toArray(cacheLoadHeap(first, more));
    }

    public static byte[] cacheLoadBytes(Path path) throws IOException {
        return toArray(cacheLoadHeap(path));
    }

    public static void writeBytes(byte[] content, String first, String... more) throws IOException {
        write(wrapBytes(content),first,more);
    }

    public static void writeBytes(byte[] content, Path path) throws IOException {
        write(wrapBytes(content),path);
    }

    public static void appendBytes(byte[] content, String first, String... more) throws IOException {
        append(wrapBytes(content),first,more);
    }

    public static void appendBytes(byte[] content, Path path) throws IOException {
        append(wrapBytes(content),path);
    }

    public static void userWriteBytes(byte[] content, String first, String... more) throws IOException {
        userWrite(wrapBytes(content),first,more);
    }

    public static void userWriteBytes(byte[] content, Path path) throws IOException {
        userWrite(wrapBytes(content),path);
    }

    public static void userAppendBytes(byte[] content, String first, String... more) throws IOException {
        userAppend(wrapBytes(content),first,more);
    }

    public static void userAppendBytes(byte[] content, Path path) throws IOException {
        userAppend(wrapBytes(content),path);
    }

    public static void gameWriteBytes(byte[] content, String first, String... more) throws IOException {
        gameWrite(wrapBytes(content),first,more);
    }

    public static void gameWriteBytes(byte[] content, Path path) throws IOException {
        gameWrite(wrapBytes(content),path);
    }

    public static void gameAppendBytes(byte[] content, String first, String... more) throws IOException {
        gameAppend(wrapBytes(content),first,more);
    }

    public static void gameAppendBytes(byte[] content, Path path) throws IOException {
        gameAppend(wrapBytes(content),path);
    }

    public static void cacheWriteBytes(byte[] content, String first, String... more) throws IOException {
        cacheWrite(wrapBytes(content),first,more);
    }

    public static void cacheWriteBytes(byte[] content, Path path) throws IOException {
        cacheWrite(wrapBytes(content),path);
    }

    public static void cacheAppendBytes(byte[] content, String first, String... more) throws IOException {
        cacheAppend(wrapBytes(content),first,more);
    }

    public static void cacheAppendBytes(byte[] content, Path path) throws IOException {
        cacheAppend(wrapBytes(content),path);
    }

    // =============================================================================
    // BASIC
    // =============================================================================

    public static ByteBuffer resourceDirect(String first, String... more) throws IOException {
        return resource(new ResourcePath(first, more),true);
    }

    public static ByteBuffer resourceHeap(String first, String... more) throws IOException {
        return resource(new ResourcePath(first, more),false);
    }

    public static ByteBuffer loadDirect(String first, String... more) throws IOException {
        return load(toPath(first, more),true);
    }

    public static ByteBuffer loadDirect(Path path) throws IOException {
        return load(path,true);
    }

    public static ByteBuffer loadHeap(String first, String... more) throws IOException {
        return load(toPath(first, more),false);
    }

    public static ByteBuffer loadHeap(Path path) throws IOException {
        return load(path,false);
    }

    public static ByteBuffer userLoadDirect(String first, String... more) throws IOException {
        return load(resolveConfine(USER_DATA,first,more),true);
    }

    public static ByteBuffer userLoadDirect(Path path) throws IOException {
        return load(resolveConfine(USER_DATA,path),true);
    }

    public static ByteBuffer userLoadHeap(String first, String... more) throws IOException {
        return load(resolveConfine(USER_DATA,first,more),false);
    }

    public static ByteBuffer userLoadHeap(Path path) throws IOException {
        return load(resolveConfine(USER_DATA,path),false);
    }

    public static ByteBuffer gameLoadDirect(String first, String... more) throws IOException {
        return load(resolveConfine(GAME_ROOT,first,more),true);
    }

    public static ByteBuffer gameLoadDirect(Path path) throws IOException {
        return load(resolveConfine(GAME_ROOT,path),true);
    }

    public static ByteBuffer gameLoadHeap(String first, String... more) throws IOException {
        return load(resolveConfine(GAME_ROOT,first,more),false);
    }

    public static ByteBuffer gameLoadHeap(Path path) throws IOException {
        return load(resolveConfine(GAME_ROOT,path),false);
    }

    public static ByteBuffer cacheLoadDirect(String first, String... more) throws IOException {
        return load(resolveConfine(USER_CACHE,first,more),true);
    }

    public static ByteBuffer cacheLoadDirect(Path path) throws IOException {
        return load(resolveConfine(USER_CACHE,path),true);
    }

    public static ByteBuffer cacheLoadHeap(String first, String... more) throws IOException {
        return load(resolveConfine(USER_CACHE,first,more),false);
    }

    public static ByteBuffer cacheLoadHeap(Path path) throws IOException {
        return load(resolveConfine(USER_CACHE,path),false);
    }



    public static void write(ByteBuffer content, String first, String... more) throws IOException {
        write(content, toPath(first, more),false);
    }

    public static void write(ByteBuffer content, Path path) throws IOException {
        write(content, path,false);
    }

    public static void append(ByteBuffer content, String first, String... more) throws IOException {
        write(content, toPath(first, more),true);
    }

    public static void append(ByteBuffer content, Path path) throws IOException {
        write(content, path,true);
    }

    public static void userWrite(ByteBuffer content, String first, String... more) throws IOException {
        write(content, resolveConfine(USER_DATA,first,more),false);
    }

    public static void userWrite(ByteBuffer content, Path path) throws IOException {
        write(content, resolveConfine(USER_DATA,path),false);
    }

    public static void userAppend(ByteBuffer content, String first, String... more) throws IOException {
        write(content, resolveConfine(USER_DATA,first,more),true);
    }

    public static void userAppend(ByteBuffer content, Path path) throws IOException {
        write(content, resolveConfine(USER_DATA,path),true);
    }

    public static void gameWrite(ByteBuffer content, String first, String... more) throws IOException {
        write(content, resolveConfine(GAME_ROOT,first,more),false);
    }

    public static void gameWrite(ByteBuffer content, Path path) throws IOException {
        write(content, resolveConfine(GAME_ROOT,path),false);
    }

    public static void gameAppend(ByteBuffer content, String first, String... more) throws IOException {
        write(content, resolveConfine(GAME_ROOT,first,more),true);
    }

    public static void gameAppend(ByteBuffer content, Path path) throws IOException {
        write(content, resolveConfine(GAME_ROOT,path),true);
    }

    public static void cacheWrite(ByteBuffer content, String first, String... more) throws IOException {
        write(content, resolveConfine(USER_CACHE,first,more),false);
    }

    public static void cacheWrite(ByteBuffer content, Path path) throws IOException {
        write(content, resolveConfine(USER_CACHE,path),false);
    }

    public static void cacheAppend(ByteBuffer content, String first, String... more) throws IOException {
        write(content, resolveConfine(USER_CACHE,first,more),true);
    }

    public static void cacheAppend(ByteBuffer content, Path path) throws IOException {
        write(content, resolveConfine(USER_CACHE,path),true);
    }



    public static void userDelete(String first, String... more) throws IOException {
        delete(resolveConfine(USER_DATA,first,more));
    }

    public static void userDelete(Path path) throws IOException {
        delete(resolveConfine(USER_DATA,path));
    }

    public static void cacheDelete(String first, String... more) throws IOException {
        delete(resolveConfine(USER_CACHE,first,more));
    }

    public static void cacheDelete(Path path) throws IOException {
        delete(resolveConfine(USER_CACHE,path));
    }

    public static void deleteCache() throws IOException {
        delete(USER_CACHE);
    }

    // =============================================================================
    // CORE
    // =============================================================================


    public static ByteBuffer resource(ResourcePath path, boolean direct) throws IOException {
        try (InputStream stream = Disk.class.getResourceAsStream(path.toString())) {
            if (stream == null) throw new FileNotFoundException("Resource could not be found: \"" + path + "\".");
            byte[] bytes = stream.readAllBytes();
            ByteBuffer buffer = allocate(bytes.length, direct);
            return buffer.put(bytes).flip();
        }
    }

    public static ByteBuffer load(Path path, boolean direct) throws IOException {
        final Path absolute = Objects.requireNonNull(path,"Path is null").toAbsolutePath();
        if (!Files.exists(absolute)) throw new FileNotFoundException("File not found: \"" + absolute + "\".");
        if (!Files.isRegularFile(absolute)) throw new IOException("Path exists but is not a regular file: \"" + absolute + "\".");
        if (Files.size(absolute) > MAX_FILE_SIZE)
            throw new IOException("File size for: \"" + absolute + "\" exceeds the maximum allowed limit: " + MAX_FILE_SIZE);
        final int numRetries = 3;
        final int retryDelayMs = 8;
        IOException lastException = null;
        for (int i = 0; i < numRetries; i++) {
            try (FileChannel channel = FileChannel.open(absolute, StandardOpenOption.READ)) {
                ByteBuffer buffer = allocate((int) channel.size(), direct);
                while (buffer.hasRemaining()) {
                    if (channel.read(buffer) == -1) break;
                } return buffer.flip();
            } catch (IOException e) {
                lastException = e;
                if (i < numRetries - 1) {
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
     * Writes the remaining bytes of a {@link ByteBuffer} to the specified file path.
     * <p> Missing parent directories are created automatically before writing. </p>
     * <p> If {@code append} is {@code true}, content is appended to the existing file or a new file
     * is created if it does not exist. If {@code append} is {@code false}, existing files are
     * overwritten using a safe atomic swap mechanism to prevent partial writes on failure. </p>
     *
     * @param content the buffer containing data to write; must not be {@code null}.
     * @param path    the destination file path; must not be {@code null}.
     * @param append  {@code true} to append content; {@code false} to overwrite.
     * @throws NullPointerException if {@code content} or {@code path} is {@code null}.
     * @throws IOException          if an I/O error occurs during directory creation or file writing.
     */
    private static void write(ByteBuffer content, Path path, boolean append) throws IOException {
        Objects.requireNonNull(content, "ByteBuffer content is null");
        Path absolute = Objects.requireNonNull(path, "Path is null").toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) Files.createDirectories(parent);
        if (append) writeDirect(content, absolute, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        else try { writeDirect(content, absolute, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException e) {
            writeAtomic(content, absolute);
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
     * @param options The {@link OpenOption}'s determining how the file is opened.
     * @throws IOException if an I/O error occurs during opening, writing, or forcing.
     */
    private static void writeDirect(ByteBuffer content, Path path, OpenOption... options) throws IOException {
        ByteBuffer workBuffer = content.duplicate();
        try (FileChannel channel = FileChannel.open(path, options)) {
            while (workBuffer.hasRemaining()) channel.write(workBuffer);
            channel.force(true);
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
     * @param path The final destination path.
     * @throws IOException if the temporary file cannot be written or the move fails.
     */
    private static void writeAtomic(ByteBuffer content, Path path) throws IOException {
        String tempName = path.getFileName().toString() + "." + System.nanoTime() + ".tmp";
        Path tempFile = path.resolveSibling(tempName);
        try { writeDirect(content, tempFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
            try { Files.move(tempFile, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) { Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(tempFile); }
    }

    /**
     * Deletes a file or directory recursively.
     * <p> If the target is a symbolic link, only the link itself is deleted; the link's target
     * contents remain untouched regardless of whether it points to a file or directory.</p>
     * <p> If deletion fails due to an {@link AccessDeniedException} on Windows (e.g., read-only files),
     * an attempt is made to strip the {@code dos:readonly} attribute and retry deletion. </p>
     * @param path the file, directory, or symbolic link to delete; ignored if it does not exist.
     * @throws IOException          if an I/O error occurs during deletion.
     * @throws NullPointerException if {@code path} is {@code null}.
     */
    private static void delete(Path path) throws IOException {
        Objects.requireNonNull(path, "Path is null");
        // Early exit if path does not exist (checking link itself, not target)
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        // Fast-path: regular files and symbolic links do not require tree traversal
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            deleteWithWindowsFallback(path);
            return;
        }
        // Directory traversal for recursive directory deletion
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                deleteWithWindowsFallback(file);
                return FileVisitResult.CONTINUE;
            }
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) throw exc;
                deleteWithWindowsFallback(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Attempts to delete a file or directory, falling back to removing the Windows read-only attribute
     * if an AccessDeniedException occurs.
     */
    private static void deleteWithWindowsFallback(Path target) throws IOException {
        try { Files.delete(target);
        } catch (AccessDeniedException e) {
            if (Platform.get() != Platform.WINDOWS) throw e;
            try { Files.setAttribute(target, "dos:readonly", false, LinkOption.NOFOLLOW_LINKS);
                Files.delete(target);
            } catch (Exception ex) {
                throw e;
            }
        }
    }

    // =============================================================================
    // HELPERS
    // =============================================================================

    /**
     * Allocates a {@link ByteBuffer} of the specified size.
     * @param size        the capacity of the buffer.
     * @param direct if {@code true}, allocates direct memory; otherwise heap memory.
     * @return the allocated {@code ByteBuffer}.
     */
    private static ByteBuffer allocate(int size, boolean direct) {
        return direct ? ByteBuffer.allocateDirect(size) : ByteBuffer.allocate(size);
    }

    /**
     * Reads remaining bytes from a ByteBuffer into a byte array.
     * <p>
     * Fast-paths to return the backing array if the buffer is heap-allocated
     * and unshifted. Otherwise, copies memory into a new array.
     * </p>
     * @param buffer the source byte buffer
     * @return the byte array containing the buffer contents
     * @throws NullPointerException if buffer is null
     */
    private static byte[] toArray(ByteBuffer buffer) {
        Objects.requireNonNull(buffer, "Null ByteBuffer to byte array.");
        if (buffer.hasArray()) {
            byte[] array = buffer.array();
            if (buffer.arrayOffset() == 0 && buffer.position() == 0 && buffer.remaining() == array.length) {
                return array;
            }
        }
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    /**
     * Splits a string into an immutable list of lines using Unicode line terminators.
     * <p>
     * Preserves all empty lines, including trailing empty lines created by trailing terminators.
     * Returns an empty list if the input string is {@code null} or empty.
     * </p>
     * @param string the string to split; may be null or empty
     * @return an immutable list of lines representing the exact string structure
     */
    private static List<String> stringAsLines(String string) {
        if (string == null || string.isEmpty()) return List.of();
        return List.of(string.split("\\R", -1));
    }

    private static ByteBuffer wrapBytes(byte[] bytes) {
        Objects.requireNonNull(bytes, "byte[] bytes is null");
        return ByteBuffer.wrap(bytes);
    }

    private static byte[] stringBytes(String string) {
        Objects.requireNonNull(string, "String is null");
        return string.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Converts path segments to a {@link Path}.
     * <p> This method is consistent with {@link Path#of(String, String...)},
     * but converts internal {@link InvalidPathException}s into {@link IOException}s
     * to catch ANY user input (E.g. from a GUI input field) </p>
     * @param first the first path segment.
     * @param more  additional path segments.
     * @return a constructed Path.
     * @throws IOException          if the path contains invalid characters.
     * @throws NullPointerException if any of the provided segments are null.
     */
    public static Path toPath(String first, String... more) throws IOException {
        try { return Path.of(first, more);
        } catch (InvalidPathException e) {
            throw new IOException("Invalid path segments provided", e);
        }
    }

    /** @see Disk#resolveConfine(Path, Path) */
    public static Path resolveConfine(Path root, String first, String... more) throws IOException {
        return resolveConfine(root,toPath(first, more));
    }

    /**
     * Resolves a relative path against a root directory, ensuring the target path
     * does not syntactically escape the root directory boundary via directory traversals (e.g. "..").
     * <p> Symbolic links within or pointing outside the root directory are permitted.</p>
     * @param root     the base directory path.
     * @param relative the relative path to resolve against the root.
     * @return the resolved, normalized path.
     * @throws IOException           if {@code relative} is absolute, if a filesystem provider mismatch occurs,
     *                               or if the resolved path escapes {@code root}.
     * @throws NullPointerException  if {@code root} or {@code relative} is {@code null}.
     */
    public static Path resolveConfine(Path root, Path relative) throws IOException {
        Objects.requireNonNull(root, "Root path is null");
        Objects.requireNonNull(relative, "Relative path is null");
        if (relative.isAbsolute()) throw new IOException("Path must be relative: \"" + relative + "\"");
        Path absoluteRoot = root.toAbsolutePath().normalize();
        Path resolved;
        try { resolved = absoluteRoot.resolve(relative).normalize();
        } catch (ProviderMismatchException e) {
            throw new IOException("FileSystem mismatch between root and relative path", e);
        } if (!resolved.startsWith(absoluteRoot)) {
            throw new AccessDeniedException("Access is confined to root bounds: \"" + absoluteRoot + "\"");
        } return resolved;
    }



    // =============================================================================
    // INITIALIZATION
    // =============================================================================

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
            USER_CACHE = USER_DATA.resolve("cache");
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
                yield (env != null && !env.isBlank()) ? toPath(env) : resolveConfine(home, "AppData", "Roaming");
            } case MAC -> resolveConfine(home, "Library", "Application Support");
            case LINUX -> { String env = System.getenv("XDG_DATA_HOME");
                yield (env != null && !env.isBlank()) ? toPath(env) : resolveConfine(home, ".local", "share");
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
        return resolveConfine(appDataRoot,companyName,gameName);
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
                .registerTypeAdapter(Color.class,new Color.Adapter())
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
        config.put("level", DEV_MODE ? "debug" : "info"); // Global level
        boolean internalLogEnabled = false;
        Exception iLogConfEx = null;
        Exception fLogConfEx = null;
        try { String logEnabledString = GameModuleProperties.get(GameModuleProperties.GAME_INTERNAL_LOG_ENABLED);
            if (Boolean.parseBoolean(logEnabledString)) {
                config.put("writerInternal", JgenlLogWriter.class.getName());
                internalLogEnabled = true; }
        } catch (IOException e) { iLogConfEx = e; }
        if (!DEV_MODE) {
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
        } if (DEV_MODE || fLogConfEx != null) {
            config.put("writerConsole", "console");
            config.put("writerConsole.stream", "out");
            config.put("writerConsole.writingthread", "false");
            config.put("writerConsole.format","{date: HH:mm:ss.SS} {pipe} {level|min-size=5} " +
                    "{pipe} {class-name|size=20} {pipe} {line|min-size=4} {pipe} {message}");
        } Configuration.replace(config);
        if (fLogConfEx != null) Logger.warn(fLogConfEx,"Failed to create log directory - falling back to console");
        if (iLogConfEx != null) Logger.warn(iLogConfEx,"Failed to read internal log property");
        Logger.info("TinyLog initialized ({} mode, internal log: {})",
                DEV_MODE ? "DEVELOPMENT" : "PRODUCTION", internalLogEnabled);
    }


    // =============================================================================
    // PUBLIC GETTERS
    // =============================================================================

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
     * @return the absolute {@link Path} to the persistent user data directory (USER_DATA).
     */
    public static Path userDataDirectory() {
        return USER_DATA;
    }


    public static Path userCacheDirectory() { return USER_CACHE; }

    /**
     * Logs are outputed here if running from .jar / .exe.
     * When running from the IDEA / Development, logging is outputed to the console instead.
     * @return Path to the log file directory: {@code [USER_DATA]/logs}.
     */
    public static Path logOutputDirectory() {
        return USER_DATA.resolve("logs");
    }

    /**
     * @return shared / thread-safe {@link Gson} object.
     * Used for JSON read/write - operations.
     */
    public static Gson gson() {
        return GSON;
    }







}
