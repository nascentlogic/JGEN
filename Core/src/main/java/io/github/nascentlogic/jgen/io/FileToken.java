package io.github.nascentlogic.jgen.io;


import org.tinylog.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;


/**
 * A lightweight, immutable descriptor representing a file or directory on disk. <p>
 * Provides absolute path resolution, cached file attributes, and utility methods
 * for directory traversal and state-change detection. </p>
 * F.Dahl, 8/26/2026
 */
public class FileToken implements Comparable<FileToken> {

    /**
     * The normalized, absolute path string representing this file or directory.
     */
    public final String path;
    /**
     * The isolated name of the file or directory without its extension.
     * <p> For standard files and directories, this contains the file name excluding the extension.
     * For hidden dotfiles (e.g., {@code .gitignore}), this contains the full file name.
     * For filesystem root directories (e.g., {@code "/"} or {@code "C:\"}), this contains
     * the root path string itself. </p>
     * <p>Guaranteed to never be blank or empty.</p>
     */
    public final String name;
    /**
     * The file extension including the leading dot (e.g., {@code ".png"}). <p>
     * Evaluated as an empty string ({@code ""}) if the path represents a directory,
     * a dotfile without a secondary suffix, or has no extension. </p>
     */
    public final String extension;
    /**
     * The last modification timestamp of the file in milliseconds since the epoch.
     */
    public final long lastModified;
    /**
     * {@code true} if this token represents a directory; {@code false} for regular files.
     */
    public final boolean isDirectory;

    /**
     * Internal use constructor.
     * No argument is null. Path and name is never blank.
     * The only entry point is the factory method: {@link #of(Path)} )}
     */
    FileToken(String path, String name, String extension, long lastModified, boolean isDirectory) {
        this.path = path;
        this.name = name;
        this.extension = extension;
        this.lastModified = lastModified;
        this.isDirectory = isDirectory;
    }

    /**
     * Constructs a {@code FileToken} from a filesystem path.
     * @param path the target filesystem path
     * @return a new {@code FileToken} instance
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws NoSuchFileException  if the target file or directory does not exist
     * @throws IOException          if an I/O error occurs reading file attributes
     */
    public static FileToken of(Path path) throws IOException {
        path = Objects.requireNonNull(path, "Path cannot be null").toAbsolutePath().normalize();
        if (!Files.exists(path)) throw new NoSuchFileException(path.toString());
        String pathStr = path.toString();
        Path fileNamePath = path.getFileName();
        String fileName = fileNamePath == null ? pathStr : fileNamePath.toString();
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        long lastModified = attributes.lastModifiedTime().toMillis();
        if (attributes.isDirectory()) {
            return new FileToken(pathStr, fileName, "", lastModified, true);
        } else {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                String tokenName = fileName.substring(0, dotIndex);
                String extension = fileName.substring(dotIndex);
                return new FileToken(pathStr, tokenName, extension, lastModified, false);
            } else {
                return new FileToken(pathStr, fileName, "", lastModified, false);
            }
        }
    }

    /**
     * Collects all child files and directories into an immutable list.
     * @return an immutable list of child tokens, or an empty list if the directory is empty
     * @throws NotDirectoryException if this token is not a directory
     * @throws IOException           if an I/O error occurs opening or reading the directory stream
     */
    public List<FileToken> listFilesInDir() throws IOException {
        return listFilesInDir(null);
    }

    /**
     * Collects matching child files and directories into an immutable list based on a filter predicate.
     * @param filter a predicate to test child elements, or {@code null} to accept all children
     * @return an immutable list of matching child tokens, or an empty list if no children match or the directory is empty
     * @throws NotDirectoryException if this token is not a directory
     * @throws IOException           if an I/O error occurs opening or reading the directory stream
     */
    public List<FileToken> listFilesInDir(Predicate<FileToken> filter) throws IOException {
        if (!isDirectory) throw new NotDirectoryException(path);
        try (Stream<FileToken> stream = streamDirectory()) {
            if (filter == null) return stream.toList();
            return stream.filter(filter).toList();
        }
    }

    /**
     * Opens an I/O-backed stream of child {@code FileToken} elements for this directory.
     * <p><b>Note:</b> Callers must close the returned stream (e.g., using a try-with-resources block)
     * to release underlying system handles.</p>
     * @return a stream of child tokens
     * @throws NotDirectoryException if this token is not a directory or is no longer a directory on disk
     * @throws IOException           if an I/O error occurs opening the directory stream
     */
    @SuppressWarnings("resource")
    public Stream<FileToken> streamDirectory() throws IOException {
        if (!isDirectory) throw new NotDirectoryException(path);
        Path pathObj = toPath();
        if (!Files.isDirectory(pathObj)) {
            throw new NotDirectoryException(path);
        } return Files.list(pathObj)
                .map(p -> {
                    try { return FileToken.of(p);
                    } catch (IOException e) {
                        Logger.warn(e);
                        return null;
                    }}).filter(Objects::nonNull);
    }

    /**
     * Converts this token's absolute path string back into a {@link Path} object.
     * @return an absolute, normalized {@link Path} instance
     */
    public Path toPath() {
        return Path.of(path);
    }

    /**
     * Used internally to check whether any file
     * in a directory has been modified.
     * @return hash of name + lastModified
     */
    public int fingerPrint() {
        int result = 17;
        result = 31 * result + name.hashCode();
        result = 31 * result + Long.hashCode(lastModified);
        return result;
    }

    /**
     * Compares this token to another, ordering directories before files,
     * then alphabetically by name (case-insensitive).
     * @param o the token to compare against
     * @return a negative integer, zero, or a positive integer as this token is less than,
     *         equal to, or greater than the specified token
     */
    @Override
    public int compareTo(FileToken o) {
        if (this.isDirectory != o.isDirectory) {
            return this.isDirectory ? -1 : 1;
        } return this.name.compareToIgnoreCase(o.name);
    }

    /**
     * Compares this token to another object based on absolute path equality.
     * @param obj the reference object with which to compare
     * @return {@code true} if both objects represent the same absolute path
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FileToken other = (FileToken) obj;
        return path.equals(other.path);
    }

    /**
     * Returns a hash code based on the absolute path string.
     * @return the path hash code
     */
    @Override
    public int hashCode() {
        return path.hashCode();
    }

    /**
     * Returns the absolute path string of this file token.
     * @return the string representation of the path
     */
    @Override
    public String toString() {
        return path;
    }

}
