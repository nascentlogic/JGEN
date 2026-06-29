package io.github.nascentlogic.jgen.io;


import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

/**
 * F.Dahl, 6/26/2026
 */
public class Shader implements Iterable<Shader.File> {


    public record File(Type type, String source) {
        public File {
            Objects.requireNonNull(source);
            Objects.requireNonNull(type);
        }
    };

    public enum Type {
        VERT_SHADER(".vert",GL_VERTEX_SHADER),
        GEOM_SHADER(".geom",GL_GEOMETRY_SHADER),
        FRAG_SHADER(".frag",GL_FRAGMENT_SHADER);
        public static final Type[] array = values();
        public final String extension;
        public final int glEnum;
        Type(String extension, int glEmun) {
            this.extension = extension;
            this.glEnum = glEmun;
        }
    }

    private final File[] files;
    private final String name;


    Shader(String name, File... files) {
        this.files = new File[Type.array.length];
        this.name = Objects.requireNonNull(name);
        Objects.requireNonNull(files);
        for (File file : files)
            if (file != null)
                this.files[file.type.ordinal()] = file;
    }

    public String name() {
        return name;
    }

    public File get(Type type) {
        return files[type.ordinal()];
    }

    public boolean isComplete() {
        return files[Type.VERT_SHADER.ordinal()] != null &&
                files[Type.FRAG_SHADER.ordinal()] != null;
    }

    @Override
    public Iterator<File> iterator() {
        return Arrays.stream(files)
                .filter(Objects::nonNull)
                .iterator();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Shader other = (Shader) obj;
        return this.name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
