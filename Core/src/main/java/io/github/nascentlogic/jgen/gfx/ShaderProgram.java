package io.github.nascentlogic.jgen.gfx;

import io.github.nascentlogic.jgen.Jgen;
import io.github.nascentlogic.jgen.io.Shader;
import io.github.nascentlogic.jgen.utils.Disposable;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.tinylog.Logger;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glGetIntegerv;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.GL_MAJOR_VERSION;
import static org.lwjgl.opengl.GL30.GL_MINOR_VERSION;
import static org.lwjgl.opengl.GL43.*;

/**
 * F.Dahl, 6/29/2026
 */
public class ShaderProgram implements Disposable {

    private static final class SamplerManager {
        final Map<String,Integer> map = HashMap.newHashMap(16);
        int nextAvailable = 0;
        void assignAndBind(String uniformName, Texture texture) {
            Objects.requireNonNull(uniformName);
            Objects.requireNonNull(texture);
            Integer assignedUnit = map.get(uniformName);
            if (assignedUnit == null) {
                int location = getUniformLocation(uniformName);
                if (!validUniformLocation(location)) return;
                assignedUnit = nextAvailable++;
                map.put(uniformName, assignedUnit);
                glUniform1i(location, assignedUnit);
            } texture.bindToSlot(assignedUnit);
        }
    }

    private static final Map<String, ShaderProgram> PROGRAM_MAP = HashMap.newHashMap(64);
    private static ShaderProgram CURRENT_PROGRAM = null;
    private static int INVALID_UNIFORM_LOCATION = -1;

    private int handle;
    private final String name;
    private final SamplerManager samplerManager;
    private final Map<String,Integer> uniformCache;
    private final Set<String> invalidUniformSet;

    /**
     * <p>Note: Existing programs with the same name will be replaced (if this installs successfully).</p>
     * @param shader shader to install
     * @throws Exception if missing files, compilation error or linking error. */
    public ShaderProgram(Shader shader) throws Exception {
        Objects.requireNonNull(shader);
        name = shader.name();
        final int[] handles = new int[Shader.Type.array.length];
        for (Shader.File file : shader) {
            handles[file.type().ordinal()] = compileShader(file, name);
        } handle = glCreateProgram();
        for (int id : handles) {
            if (id == 0) continue;
            glAttachShader(handle, id);
        } glLinkProgram(handle);
        int linkStatus = glGetProgrami(handle, GL_LINK_STATUS);
        for (int id : handles) {
            if (id == 0) continue;
            glDetachShader(handle,id);
            glDeleteShader(id);
        } if (linkStatus == GL_FALSE) {
            String log = glGetProgramInfoLog(handle);
            glDeleteProgram(handle);
            Logger.warn("Shader linking error: \"{}\"", name);
            throw new Exception(log);
        } uniformCache = uniformLocationMap(handle);
        invalidUniformSet = HashSet.newHashSet(16);
        samplerManager = new SamplerManager();
        ShaderProgram existing = PROGRAM_MAP.put(name,this);
        if (existing != null) {
            existing.free();
            Logger.info("Program replaced: \"{}\"", name);
        }
    }

    public String name() {
        return name;
    }

    public void use() {
        if (handle == GL_NONE) {
            throw new IllegalStateException("Program: \""+ name +"\" has been disposed");
        } if (this != CURRENT_PROGRAM) {
            glUseProgram(handle);
            CURRENT_PROGRAM = this;
        }
    }

    @Override
    public void free() {
        if (handle == GL_NONE) return;
        if (this == CURRENT_PROGRAM) useNone();
        PROGRAM_MAP.remove(name);
        glDeleteProgram(handle);
        handle = GL_NONE;
    }

    public static ShaderProgram currentProgram() {
        return CURRENT_PROGRAM;
    }

    /** @return {@code true} if program exist */
    public static boolean useProgram(String name) {
        ShaderProgram program = PROGRAM_MAP.get(name);
        if (program == null) return false;
        program.use();
        return true;
    }

    public static void useNone() {
        glUseProgram(GL_NONE);
        CURRENT_PROGRAM = null;
    }

    public static void deleteProgram(String name) {
        Disposable.free(PROGRAM_MAP.get(name));
    }

    /** Called by the engine before Jgen shuts down.
     * No need to delete them manually. */
    public static void deleteAllPrograms() {
        PROGRAM_MAP.values().forEach(program -> {
            glDeleteProgram(program.handle);
            program.handle = GL_NONE;
        }); PROGRAM_MAP.clear();
        useNone();
    }

    public static void setTexture(String name, Texture texture) {
        if (CURRENT_PROGRAM == null) throw new IllegalStateException("No bound Program");
        CURRENT_PROGRAM.samplerManager.assignAndBind(name,texture);
    }

    public static void setUniformI(String name, int i) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            glUniform1i(uniform_location, i);
        }
    }

    public static void setUniformI(String name, int i0, int i1) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(2);
                buffer.put(i0).put(i1).flip();
                glUniform2iv(uniform_location, buffer);
            }
        }
    }

    public static void setUniformI(String name, int i0, int i1, int i2) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(3);
                buffer.put(i0).put(i1).put(i2).flip();
                glUniform3iv(uniform_location, buffer);
            }
        }
    }

    public static void setUniformI(String name, int i0, int i1, int i2, int i3) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(4);
                buffer.put(i0).put(i1).put(i2).put(i3).flip();
                glUniform4iv(uniform_location, buffer);
            }
        }
    }

    public static void setUniformI(String name, int[] array) {
        setUniformI(name, array, 0, array.length);
    }

    public static void setUniformI(String name, int[] array, int offset, int count) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(count);
                for (int i = 0; i < count; i++) {
                    buffer.put(array[i + offset]);
                } glUniform1iv(uniform_location, buffer.flip());
            }
        }
    }

    public static void setUniformI(String name, IntBuffer buffer) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            glUniform1iv(uniform_location, buffer);
        }
    }


    public static void setUniformF(String name, float f) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            glUniform1f(uniform_location, f);
        }
    }

    public static void setUniformF(String name, float f0, float f1) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(2);
                buffer.put(f0).put(f1).flip();
                glUniform2fv(uniform_location, buffer);
            }
        }
    }

    public static void setUniformF(String name, float f0, float f1, float f2) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(3);
                buffer.put(f0).put(f1).put(f2).flip();
                glUniform3fv(uniform_location, buffer);
            }
        }
    }

    public static void setUniformF(String name, float f0, float f1, float f2, float f3) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(4);
                buffer.put(f0).put(f1).put(f2).put(f3).flip();
                glUniform4fv(uniform_location, buffer);
            }
        }
    }

    public static void setUniformF(String name, float[] array) {
        setUniformF(name, array, 0, array.length);
    }

    public static void setUniformF(String name, float[] array, int offset, int count) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(count);
                for (int i = 0; i < count; i++) {
                    buffer.put(array[i + offset]);
                } glUniform1fv(uniform_location, buffer.flip());
            }
        }
    }

    public static void setUniformF(String name, FloatBuffer buffer) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            glUniform1fv(uniform_location, buffer);
        }
    }

    public static void setUniformF(String name, Vector2f vec2) {
        setUniformF(name, vec2.x, vec2.y);
    }

    public static void setUniformF(String name, Vector3f vec3) {
        setUniformF(name, vec3.x, vec3.y, vec3.z);
    }

    public static void setUniformF(String name, Vector4f vec4) {
        setUniformF(name, vec4.x, vec4.y, vec4.z, vec4.w);
    }

    public static void setUniformI(String name, Vector2i vec2) {
        setUniformI(name, vec2.x, vec2.y);
    }

    public static void setUniformI(String name, Vector3i vec3) {
        setUniformI(name, vec3.x, vec3.y, vec3.z);
    }

    public static void setUniformI(String name, Vector4i vec4) {
        setUniformI(name, vec4.x, vec4.y, vec4.z, vec4.w);
    }

    public static void setUniformF(String name, Matrix2f mat2) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(4);
                glUniformMatrix2fv(uniform_location, false, mat2.get(buffer));
            }
        }
    }

    public static void setUniformF(String name, Matrix3f mat3) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(9);
                glUniformMatrix3fv(uniform_location, false, mat3.get(buffer));
            }
        }
    }

    public static void setUniformF(String name, Matrix4f mat4) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(16);
                glUniformMatrix4fv(uniform_location, false, mat4.get(buffer));
            }
        }
    }

    public static void setUniformF(String name, Vector2f[] vec2) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(2 * vec2.length);
                for (Vector2f value : vec2) {
                    buffer.put(value.x).put(value.y);
                } glUniform2fv(uniform_location, buffer.flip());
            }
        }
    }

    public static void setUniformF(String name, Vector3f[] vec3) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(3 * vec3.length);
                for (Vector3f v : vec3) {
                    buffer.put(v.x).put(v.y).put(v.z);
                } glUniform3fv(uniform_location, buffer.flip());
            }
        }
    }

    public static void setUniformF(String name, Vector4f[] vec4) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(4 * vec4.length);
                for (Vector4f v : vec4) { buffer.put(v.x).put(v.y).put(v.z).put(v.w);
                } glUniform4fv(uniform_location, buffer.flip());
            }
        }
    }

    public static void setUniformI(String name, Vector2i[] vec2) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(2 * vec2.length);
                for (Vector2i value : vec2) {
                    buffer.put(value.x).put(value.y);
                } glUniform2iv(uniform_location, buffer.flip());
            }
        }
    }

    public static void setUniformI(String name, Vector3i[] vec3) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(3 * vec3.length);
                for (Vector3i v : vec3) {
                    buffer.put(v.x).put(v.y).put(v.z);
                } glUniform3iv(uniform_location, buffer.flip());
            }
        }
    }

    public static void setUniformI(String name, Vector4i[] vec4) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(4 * vec4.length);
                for (Vector4i v : vec4) {
                    buffer.put(v.x).put(v.y).put(v.z).put(v.w);
                } glUniform4iv(uniform_location, buffer.flip());
            }
        }
    }

    public static void setUniformF(String name, Matrix2f[] mat2) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(4 * mat2.length);
                for (int i = 0; i < mat2.length; i++) {
                    mat2[i].get(4 * i, buffer);
                } glUniformMatrix2fv(uniform_location, false, buffer);
            }
        }
    }

    public static void setUniformF(String name, Matrix3f[] mat3) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(9 * mat3.length);
                for (int i = 0; i < mat3.length; i++) {
                    mat3[i].get(9 * i, buffer);
                } glUniformMatrix3fv(uniform_location, false, buffer);
            }
        }
    }

    public static void setUniformF(String name, Matrix4f[] mat4) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(16 * mat4.length);
                for (int i = 0; i < mat4.length; i++) {
                    mat4[i].get(16 * i, buffer);
                } glUniformMatrix4fv(uniform_location, false, buffer);
            }
        }
    }

    public static void setUniformU(String name, int u) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            glUniform1ui(uniform_location, u);
        }
    }

    public static void setUniformU(String name, int u0, int u1) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(2);
                buffer.put(u0).put(u1).flip();
                glUniform2uiv(uniform_location, buffer);
            }
        }
    }

    public static void setUniformU(String name, int u0, int u1, int u2) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(3);
                buffer.put(u0).put(u1).put(u2).flip();
                glUniform3uiv(uniform_location, buffer);
            }
        }
    }

    public static void setUniformU(String name, int u0, int u1, int u2, int u3) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(4);
                buffer.put(u0).put(u1).put(u2).put(u3).flip();
                glUniform4uiv(uniform_location, buffer);
            }
        }
    }

    public static void setUniformU(String name, int[] array) {
        setUniformU(name, array, 0, array.length);
    }

    public static void setUniformU(String name, int[] array, int offset, int count) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(count);
                for (int i = 0; i < count; i++) {
                    buffer.put(array[i + offset]);
                } glUniform1uiv(uniform_location, buffer.flip());
            }
        }
    }

    public static void setUniformU(String name, IntBuffer buffer) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            glUniform1uiv(uniform_location, buffer);
        }
    }

    public static void setUniformU(String name, Vector2i vec2) {
        setUniformU(name, vec2.x, vec2.y);
    }

    public static void setUniformU(String name, Vector3i vec3) {
        setUniformU(name, vec3.x, vec3.y, vec3.z);
    }

    public static void setUniformU(String name, Vector4i vec4) {
        setUniformU(name, vec4.x, vec4.y, vec4.z, vec4.w);
    }

    public static void setUniformU(String name, Vector2i[] vec2) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(2 * vec2.length);
                for (Vector2i value : vec2) {
                    buffer.put(value.x).put(value.y);
                } glUniform2uiv(uniform_location, buffer.flip());
            }
        }
    }

    public static void setUniformU(String name, Vector3i[] vec3) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(3 * vec3.length);
                for (Vector3i v : vec3) {
                    buffer.put(v.x).put(v.y).put(v.z);
                } glUniform3uiv(uniform_location, buffer.flip());
            }
        }
    }

    public static void setUniformU(String name, Vector4i[] vec4) {
        int uniform_location = getUniformLocation(name);
        if (validUniformLocation(uniform_location)) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.mallocInt(4 * vec4.length);
                for (Vector4i v : vec4) {
                    buffer.put(v.x).put(v.y).put(v.z).put(v.w);
                } glUniform4uiv(uniform_location, buffer.flip());
            }
        }
    }

    private static boolean validUniformLocation(int location) {
        return location != INVALID_UNIFORM_LOCATION;
    }

    private static int getUniformLocation(String name) {
        Objects.requireNonNull(name);
        if (CURRENT_PROGRAM == null) throw new IllegalStateException("No bound Program");
        int uniformLocation = CURRENT_PROGRAM.uniformCache.getOrDefault(name,INVALID_UNIFORM_LOCATION);
        if (uniformLocation == INVALID_UNIFORM_LOCATION) {
            if (CURRENT_PROGRAM.invalidUniformSet.add(name)) // prevent log output clutter
                Logger.warn("No such uniform: \"{}\" for Program: \"{}\"",name,CURRENT_PROGRAM.name);
        } return uniformLocation;
    }

    private static Map<String,Integer> uniformLocationMap(int program) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer uniformCount = stack.mallocInt(1);
            glGetProgramInterfaceiv(program, GL_UNIFORM, GL_ACTIVE_RESOURCES, uniformCount);
            int numUniforms = uniformCount.get(0);
            Map<String,Integer> map = HashMap.newHashMap(numUniforms);
            for (int uniform = 0; uniform < numUniforms; uniform++) {
                String name = glGetProgramResourceName(program, GL_UNIFORM, uniform);
                int uniformLocation = glGetUniformLocation(program, name);
                if (uniformLocation >= 0) {
                    String[] split = name.split("\\[\\d+?]");
                    if (split.length > 0) {
                        String uniform_name = split[0];
                        map.putIfAbsent(uniform_name, uniformLocation);
                    }
                }
            }
            return map;
        }
    }

    private static int compileShader(Shader.File file, String name) throws Exception {
        int shader = glCreateShader(file.type().glEnum);
        glShaderSource(shader,versionHeader() + file.source());
        glCompileShader(shader);
        int compile_status = glGetShaderi(shader,GL_COMPILE_STATUS);
        if (compile_status == GL_FALSE) {
            String errorString = glGetShaderInfoLog(shader);
            Logger.warn("Shader compilation error: \"{}\"", name + file.type().extension);
            glDeleteShader(shader);
            throw new Exception(errorString);
        } return shader;
    }

    /** GLSL version header (E.g. £version 450 core). */
    private static String versionHeader;
    private static String versionHeader() {
        if (versionHeader == null) {
            try (MemoryStack stack = MemoryStack.stackPush()){
                IntBuffer version_major = stack.mallocInt(1);
                IntBuffer version_minor = stack.mallocInt(1);
                glGetIntegerv(GL_MAJOR_VERSION, version_major);
                glGetIntegerv(GL_MINOR_VERSION, version_minor);
                long window_handle = Jgen.get().window().handle();
                boolean core_profile = glfwGetWindowAttrib(window_handle,GLFW_OPENGL_PROFILE) == GLFW_OPENGL_CORE_PROFILE;
                versionHeader = "#version " + version_major.get(0) + version_minor.get(0) + "0"; // Note space added below
                if (core_profile) versionHeader += " core";
                versionHeader += "\n";
            }
        } return versionHeader;
    }





}
