package io.github.nascentlogic.jgen.gfx;

import io.github.nascentlogic.jgen.Jgen;
import io.github.nascentlogic.jgen.io.Disk;
import io.github.nascentlogic.jgen.io.Shader;
import io.github.nascentlogic.jgen.utils.Disposable;
import io.github.nascentlogic.jgen.utils.JgenMath;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.tinylog.Logger;

import java.lang.Math;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static io.github.nascentlogic.jgen.gfx.TextureFormat.RG16UI;
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

    private static InternalPrograms internalPrograms;
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

    public static InternalPrograms internalPrograms() {
        if (internalPrograms == null) internalPrograms = new InternalPrograms();
        return internalPrograms;
    }

    public static ShaderProgram currentProgram() {
        return CURRENT_PROGRAM;
    }

    public static ShaderProgram getProgramByName(String name) {
        return PROGRAM_MAP.get(name);
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
        Disposable.free(internalPrograms);
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

    public static void setUniformB(String name, boolean b) {
        setUniformI(name, b ? 1 : 0);
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



    public static final class InternalPrograms implements Disposable {

        private ShaderProgram blitProgram;
        private ShaderProgram srgbProgram;
        private ShaderProgram udfSeedProgram;
        private ShaderProgram udfJumpProgram;
        private ShaderProgram udfResolveProgram;
        private final int blitVao;
        private final int srgbVao;
        private final int udfVao;

        private final int vbo;
        private final int ebo;

        InternalPrograms() {
            try {
                blitProgram = new ShaderProgram(new Shader("jgen-blit", blitVSource(), blitFSource()));
                blitProgram.use();
                ShaderProgram.setUniformI("uTexture",0);
                srgbProgram = new ShaderProgram(new Shader("jgen-linearToSrgb", blitVSource(), srgbFSource()));
                srgbProgram.use();
                ShaderProgram.setUniformI("uTexture", 0);
                udfSeedProgram = new ShaderProgram(Disk.resourceShader("udf-seed","jgen/glsl/udf"));
                udfJumpProgram = new ShaderProgram(Disk.resourceShader("udf-jump","jgen/glsl/udf"));
                udfResolveProgram = new ShaderProgram(Disk.resourceShader("udf-resolve","jgen/glsl/udf"));
                ShaderProgram.useNone();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // Generate Shared GPU Buffers
            // Allocate once: 1 quad capacity (16 floats of vertex data)
            vbo = Buffers.generateVBO(GL_DYNAMIC_DRAW, 16 * Float.BYTES);
            ebo = Buffers.generateQuadEBO(1);
            blitVao = generateDefaultVAO();
            srgbVao = generateDefaultVAO();
            udfVao = generateDefaultVAO();
            Buffers.bindVBO(0);
            Buffers.bindVAO(0);
        }


        /** Generates a complet VAO with attributes.
         * The vao can be used with {@link #draw(int)} to render quads */
        public int generateDefaultVAO() {
            int vao = Buffers.generateBindVAO();
            Buffers.bindEBO(ebo);
            Buffers.bindVBO(vbo);
            int stride = 4 * Float.BYTES;
            glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2 * Float.BYTES);
            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);
            return vao;
        }

        /**
         * Generates an Unsigned Distance Field (UDF) texture stored as GL_R16F.
         * @param source   4 channel input 2D texture
         * @param distFunc 0 = Euclidean, 1 = Manhattan, 2 = Chebyshev
         * @return GL_R16F texture containing raw pixel distances
         */
        public Texture generateDistanceField(Texture source, int distFunc) {
            final int width = source.width();
            final int height = source.height();
            int drawBuffer = 0;
            int readBuffer = 1;
            Framebuffer[] seedBuffers = Framebuffer.pingPongBuffers(RG16UI,width,height,true);

            // --- PASS 1: Initialize Seeds ---
            udfSeedProgram.use();
            //Framebuffer.checkDrawStatus();
            ShaderProgram.setTexture("uSource",source); // from source
            seedBuffers[drawBuffer].bindDraw();
            draw(udfVao); // draw fullscreen quad

            // --- PASS 2: JFA Ping-Pong Propagation Loop ---
            int totalPasses = JgenMath.log2iCeil(Math.max(width, height));
            int stepSize = 1 << (totalPasses - 1); // Start at maxDim / 2
            udfJumpProgram.use();
            for (int i = 0; i < totalPasses; i++) {
                int tmp = drawBuffer;
                drawBuffer = readBuffer;
                readBuffer = tmp;
                seedBuffers[drawBuffer].bindDraw();
                ShaderProgram.setTexture("uSource",seedBuffers[readBuffer].attachment(0));
                ShaderProgram.setUniformI("uStepSize",stepSize);
                glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);
                stepSize /= 2;
            }
            // --- PASS 3: Resolve Final Distances ---
            Texture distanceField = Texture.generate2D(width,height);
            distanceField.allocate(TextureFormat.R16F,false);
            distanceField.filterNearest();
            distanceField.clampToEdge();
            Framebuffer resolveFBO = new Framebuffer(width,height);
            resolveFBO.bindDraw();
            resolveFBO.attachTexture(distanceField,0,false);
            resolveFBO.drawbuffer(0);
            udfResolveProgram.use();
            ShaderProgram.setTexture("uSource",seedBuffers[drawBuffer].attachment(0));
            ShaderProgram.setUniformI("uDistFunc",distFunc);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);

            Disposable.free(seedBuffers); // attached textures are disposed
            resolveFBO.free(); // attached texture is not disposed
            return distanceField;
        }


        public void blit(Texture source) { blit(source,0,0,1,1,0,0,1,1); }
        public void blit(Texture source, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2) {
            blitProgram.use();
            source.bindToSlot(0);
            draw(blitVao, x1, y1, x2, y2, u1, v1, u2, v2);
        }

        public void linearToSrgb(Texture source) {
            srgbProgram.use();
            source.bindToSlot(0);
            draw(srgbVao);
        }

        // todo drawRepeat
        public void draw(int targetVao) { draw(targetVao, 0,0,1,1,0,0,1,1); }
        public void draw(int targetvao, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.mallocFloat(16);
                buffer.put(x1).put(y2).put(u1).put(v2); // v0: Top-Left     -> Uses Top UV (v2 = 1.0)
                buffer.put(x1).put(y1).put(u1).put(v1); // v1: Bottom-Left  -> Uses Bottom UV (v1 = 0.0)
                buffer.put(x2).put(y1).put(u2).put(v1); // v2: Bottom-Right -> Uses Bottom UV (v1 = 0.0)
                buffer.put(x2).put(y2).put(u2).put(v2); // v3: Top-Right    -> Uses Top UV (v2 = 1.0)
                Buffers.bindVBO(vbo);
                buffer.flip();
                Buffers.uploadVBO(buffer);
                Buffers.bindVAO(targetvao);
                glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);
            }
        }

        private String blitVSource() {
            return """
                    layout(location = 0) in vec2 aPos;
                    layout(location = 1) in vec2 aTexCoord;
                    out vec2 vTexCoord;
                    void main() {
                        vTexCoord = aTexCoord;
                        gl_Position = vec4(aPos * 2.0 - 1.0, 0.0, 1.0);
                    }""";

        }

        private String blitFSource() {
            return """
                    layout(location = 0) out vec4 fColor;
                    uniform sampler2D uTexture;
                    in vec2 vTexCoord;
                    void main() {
                        fColor = texture(uTexture, vTexCoord);
                    }""";
        }

        private String srgbFSource() {
            return """
                    layout(location = 0) out vec4 fColor;
                    uniform sampler2D uTexture;
                    in vec2 vTexCoord;
                    vec3 linearToSRGB(vec3 c) {
                        vec3 cutoff = vec3(lessThan(c, vec3(0.0031308)));
                        vec3 higher = 1.055 * pow(c, vec3(1.0 / 2.4)) - 0.055;
                        vec3 lower = 12.92 * c;
                        return mix(higher, lower, cutoff);
                    }
                    void main() {
                        vec4 texColor = texture(uTexture, vTexCoord);
                        vec3 srgbColor = linearToSRGB(texColor.rgb);
                        fColor = vec4(srgbColor, texColor.a);
                    }""";
        }

        @Override
        public void free() {
            Buffers.deleteVAO(blitVao);
            Buffers.deleteVAO(srgbVao);
            Buffers.deleteVAO(udfVao);
            Buffers.deleteBuffers(vbo,ebo);
        }
    }


}
