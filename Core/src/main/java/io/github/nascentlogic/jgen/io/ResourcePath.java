package io.github.nascentlogic.jgen.io;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * An immutable, validated path representation for Java classpath resources.
 * <p>
 * Guarantees a normalized path string that always starts with a leading slash ({@code /}),
 * uses forward-slash separators, and prevents escaping the classpath root.
 * Integrates cleanly with disk-based operations by providing safe conversion methods.
 * </p>
 *
 * F.Dahl, 8/26/2026
 */
public class ResourcePath {

    private final String path;
    private final String name;
    private final String extension;

    /**
     * Constructs and validates a new {@code ResourcePath} from one or more path segments.
     * @param first the initial path segment (must not be {@code null})
     * @param more  optional additional path segments
     * @throws IOException          if the joined path is blank, malformed, or escapes the root
     * @throws NullPointerException if any segment is {@code null}
     */
    public ResourcePath (String first, String... more) throws IOException {
        Objects.requireNonNull(first,"Null path segment: \"first\".");
        Objects.requireNonNull(more,"Null path varArgs: \"...more\".");
        for (String segment : more) Objects.requireNonNull(segment,"One or more null path segments: \"...more\".");
        path = buildPath(first, more);
        String[] split = extractNameAndExtension(path);
        name = split[0];
        extension = split[1];
    }

    /**
     * Joins multiple path segments using forward slashes and delegates to {@link #normalizePath(String)}.
     * @param first the initial path segment
     * @param more  additional path segments
     * @return the joined and normalized path string
     * @throws IOException if validation fails during normalization
     */
    private String buildPath(String first, String... more) throws IOException {
        // segments are already null-checked
        String path;
        if (more.length == 0) path = first;
        else { StringBuilder sb = new StringBuilder();
            sb.append(first);
            for (String segment : more) {
                if (!segment.isEmpty()) {
                    if (!sb.isEmpty()) sb.append('/');
                    sb.append(segment);}
            } path = sb.toString();
        } return normalizePath(path);
    }

    /**
     * Validates and normalizes a raw path string for use with resource loading APIs.
     * <p>
     * Enforces three security and formatting rules:
     * <ul>
     *   <li>Standardizes separators to forward slashes ({@code /}).</li>
     *   <li>Collapses redundant segments ({@code .}, {@code ..}) via {@link Path#normalize()}.</li>
     *   <li>Rejects paths that escape the root directory or attempt absolute disk access.</li>
     * </ul>
     * </p>
     * @param path the raw path string to normalize
     * @return a normalized path string starting with a mandatory leading slash
     * @throws IOException if the path is invalid, blank, or escapes the root
     */
    private String normalizePath(String path) throws IOException{
        String standardized = path.trim().replace('\\', '/');
        while (standardized.startsWith("/")) {
            standardized = standardized.substring(1);
        } // Catch cases where the input was just "/" or "   "
        if (standardized.isBlank()) {
            throw new IOException("Resource path is blank or resolves to root (\"/\")");
        } try { //  Use Path API to normalize "." and ".." tokens
            Path normalized = Path.of(standardized).normalize();
            if (normalized.isAbsolute()) throw new IOException("Resource path cannot be absolute: " + path);
            // Convert back to String and ensure forward slashes (Path.toString() is OS-dependent)
            String result = normalized.toString().replace('\\', '/');
            // 1. isEmpty(): user provided something like "folder/.."
            // 2. equals("."): user provided "."
            // 3. startsWith(".."): user provided "../../secret.txt"
            if (result.isEmpty() || result.equals(".") || result.startsWith("..")) {
                throw new IOException("Invalid resource path (escapes root or invalid target): " + path);
            } return "/" + result;
        } catch (InvalidPathException e) {
            throw new IOException("Malformed resource path: " + path, e);
        }
    }

    /**
     * Extracts the base name and extension from a normalized path string. <p>
     * Treats hidden dotfiles (e.g., {@code .gitignore}) and trailing dots as having no extension.
     * For multi-dot filenames (e.g., {@code build.gradle.kts}), the last dot acts as the delimiter. </p>
     * @param path the normalized path string
     * @return a two-element array containing {@code [name, extension]}
     */
    private String[] extractNameAndExtension(String path) {
        String[] values = new String[2];
        int slashIndex = path.lastIndexOf('/');
        String fullName = path.substring(slashIndex + 1);
        int dotIndex = fullName.lastIndexOf('.');
        // dotIndex <= 0: Covers no dot (-1), leading dot files like .gitignore (0)
        // dotIndex == fullName.length() - 1: Covers trailing dots like "file."
        if (dotIndex <= 0 || dotIndex == fullName.length() - 1) {
            values[0] = fullName;
            values[1] = "";
        } else {
            values[0] = fullName.substring(0, dotIndex);
            values[1] = fullName.substring(dotIndex);
        }
        return values;
    }

    /**
     * Appends this resource path to a target disk directory, safely stripping the leading slash.
     * Prevents Windows drive-root resets during local cache resolutions.
     * @param rootDirectory the base directory on the local file system
     * @return the resolved {@link Path}
     */
    public Path appendTo(Path rootDirectory) {
        return rootDirectory.resolve(path.substring(1));
    }

    /**
     * Checks if this path contains a non-empty file extension.
     * A path without extension does not necessarily mean the path is a directory.
     * @return {@code true} if an extension is present, {@code false} otherwise
     */
    public boolean hasExtension() {
        return !extension.isEmpty();
    }

    /**
     * Returns the full, normalized resource path string.
     * @return the path starting with {@code /} (e.g., {@code "/images/duck.png"})
     */
    public String path() {
        return path;
    }

    /**
     * Returns the isolated name of the file or directory without its extension.
     * @return the base name (e.g., {@code "duck"} for {@code "/images/duck.png"})
     */
    public String name() {
        return name;
    }

    /**
     * Returns the file extension, including the leading dot.
     * @return the extension (e.g., {@code ".png"}), or an empty string if none exists
     */
    public String extension() {
        return extension;
    }

    /**
     * Returns the complete file or folder name including its extension.
     * @return the full filename (e.g., {@code "duck.png"})
     */
    public String fullName() {
        return name + extension;
    }

    @Override
    public String toString() {
        return path;
    }
}
