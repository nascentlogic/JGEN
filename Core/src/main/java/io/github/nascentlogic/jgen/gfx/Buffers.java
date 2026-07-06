package io.github.nascentlogic.jgen.gfx;

import io.github.nascentlogic.jgen.Jgen;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.tinylog.Logger;

import java.nio.*;
import java.util.Objects;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;

/**
 * F.Dahl, 6/19/2026
 */
public class Buffers {


    // =============================================================================
    // Buffer Binding
    // =============================================================================

    public static void bindVAO(int vao) { glBindVertexArray(vao); }
    public static void bindVBO(int vbo) { bindBuffer(GL_ARRAY_BUFFER,vbo); }
    public static void bindEBO(int ebo) { bindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo); }
    public static void bindUBO(int ubo) { bindBuffer(GL_UNIFORM_BUFFER,ubo); }
    public static void bindBuffer(int target, int buffer) { glBindBuffer(target,buffer); }
    public static void bindBufferBase(int target, int index, int buffer) { glBindBufferBase(target, index, buffer); }

    // =============================================================================
    // Buffer Generation
    // =============================================================================

    public static int generateVAO() { return glGenVertexArrays(); }
    public static int generateBindVAO() {
        int vao = generateVAO();
        bindVAO(vao);
        return vao;
    }

    public static int generateVBO(int usage, int size) { return generateBuffer(GL_ARRAY_BUFFER,usage,size); }
    public static int generateEBO(int size) { return generateBuffer(GL_ELEMENT_ARRAY_BUFFER,GL_STATIC_DRAW,size); }
    public static int generateEBO(byte[] indices) {
        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int size = indices.length;
            ByteBuffer buffer;
            if (size < stack.getSize()) {
                buffer = stack.malloc(size);
            } else buffer = MemoryUtil.memAlloc(size);
            buffer.put(indices).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER,buffer,GL_STATIC_DRAW);
            if (size >= stack.getSize()) MemoryUtil.memFree(buffer);
            Jgen.glCheckError();
        } return ebo;
    }

    public static int generateEBO(short[] indices) {
        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int size = indices.length * Short.BYTES;
            ShortBuffer buffer;
            if (size < stack.getSize()) {
                buffer = stack.mallocShort(indices.length);
            } else buffer = MemoryUtil.memAllocShort(indices.length);
            buffer.put(indices).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER,buffer,GL_STATIC_DRAW);
            if (size >= stack.getSize()) MemoryUtil.memFree(buffer);
            Jgen.glCheckError();
        } return ebo;
    }

    public static int generateEBO(int[] indices) {
        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER,ebo);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int size = indices.length * Integer.BYTES;
            IntBuffer buffer;
            if (size < stack.getSize()) {
                buffer = stack.mallocInt(indices.length);
            } else buffer = MemoryUtil.memAllocInt(indices.length);
            buffer.put(indices).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER,buffer,GL_STATIC_DRAW);
            if (size >= stack.getSize()) MemoryUtil.memFree(buffer);
            Jgen.glCheckError();
        } return ebo;
    }

    /**
     * Generate Indices (short) array (EBO) for quad rendering.
     * <p>Element order: 0,1,2,2,3,0 -> 4,5,6,6,7,4 -> ...</p>
     * <p>(use draw elements: GL_UNSIGNED_SHORT)</p>
     * @param count number of quads (max count is 8192)
     * @return opengl buffer handle for the ebo
     */
    public static int generateQuadEBO(int count) {
        return generateEBO(quadIndicesArray(count));
    }

    public static int generateUBO(boolean dynamic, int size) {
        return generateUBO(dynamic ? GL_DYNAMIC_DRAW : GL_STATIC_DRAW, size);
    }

    public static int generateUBO(int usage, int size) {
        if (size > 16384) Logger.info("Creating large UBO ({} bytes). OpenGL min supported size is 16 KB.", size);
        if (size > 65536) Logger.warn("Creating very large UBO ({} bytes). Maximum recommended size is 64 KB.", size);
        return generateBuffer(GL_UNIFORM_BUFFER,usage,size);
    }

    public static int generateBuffer(int target, int usage, int size) {
        if (size < 0) throw new IllegalArgumentException("Buffer size must be greater than 0");
        int handle = glGenBuffers();
        glBindBuffer(target,handle);
        glBufferData(target,size,usage);
        Jgen.glCheckError();
        return handle;
    }

    private static short[] quadIndicesArray(int count) {
        int QUAD_LIMIT = (Short.MAX_VALUE + 1) / 4;
        if (count > QUAD_LIMIT) {
            Logger.warn("Indices array too large, capping count to: {} quads",QUAD_LIMIT);
            count = QUAD_LIMIT;
        } short[] array = new short[count * 6];
        for (int i = 0, j = 0; i < array.length; i += 6, j += 4) {
            array[i    ] = (short) (j    );
            array[i + 1] = (short) (j + 1);
            array[i + 2] = (short) (j + 2);
            array[i + 3] = (short) (j + 2);
            array[i + 4] = (short) (j + 3);
            array[i + 5] = (short) (j    );
        } return array;
    }

    // =============================================================================
    // Buffer Upload
    // =============================================================================

    public static void uploadVBO(Buffer data) { upload(GL_ARRAY_BUFFER, data); }
    public static void uploadVBO(int offset, Buffer data) { upload(GL_ARRAY_BUFFER, offset, data); }
    public static void uploadEBO(int offset, Buffer data) { upload(GL_ELEMENT_ARRAY_BUFFER, offset, data); }
    public static void uploadEBO(Buffer data) { upload(GL_ELEMENT_ARRAY_BUFFER, data); }
    public static void uploadUBO(Buffer data) { upload(GL_UNIFORM_BUFFER, data); }
    public static void uploadUBO(int offset, Buffer data) { upload(GL_UNIFORM_BUFFER, offset, data); }
    public static void upload(int target, Buffer data) { upload(target,0,data); }

    /**
     * Upload data to the bound target buffer.
     * @param target E.g. GL_UNIFORM_BUFFER, GL_ELEMENT_ARRAY_BUFFER, GL_ARRAY_BUFFER
     * @param offset offset <strong>IN BYTES</strong> into the target buffer
     * @param data The NIO Buffer containing the data. Must be a direct buffer
     */
    public static void upload(int target, int offset, Buffer data) {
        Objects.requireNonNull(data);
        if (!data.isDirect()) throw new IllegalArgumentException("Data buffer must be a direct buffer");
        if (data.remaining() == 0) return;
        switch (data) {
            case ByteBuffer b  -> glBufferSubData(target, offset, b);
            case FloatBuffer b -> glBufferSubData(target, offset, b);
            case IntBuffer b   -> glBufferSubData(target, offset, b);
            case ShortBuffer b -> glBufferSubData(target, offset, b);
            case DoubleBuffer b-> glBufferSubData(target, offset, b);
            case LongBuffer b  -> glBufferSubData(target, offset, b);
            default -> throw new IllegalArgumentException("Unsupported buffer type: " + data.getClass().getName());
        }
    }

    // =============================================================================
    // Buffer Delete
    // =============================================================================

    /** NOTE: Reccomend deleting the VAO before VBOs and EBO */
    public static void deleteVAO(int vao) { glDeleteVertexArrays(vao); }
    public static void deleteBuffer(int buffer) { glDeleteBuffers(buffer); }
    public static void deleteBuffers(int... buffers) { glDeleteBuffers(buffers); }

    // =============================================================================
    // CPU - Copy / Transfer
    // =============================================================================

    /**
     * Performs a memory block transfer (blit) from a source
     * Buffer to a destination Buffer.
     * <p>This method works seamlessly across any combination of direct (native)
     * and heap-allocated buffers. Position and limit tracking markers of both buffers remain
     * completely unchanged after the transfer.
     * <p>The transferred range spans inclusively from {@code src.position()} to
     * {@code src.position() + src.remaining() - 1}, writing into the destination
     * buffer starting exactly at {@code dst.position()}.
     * @param src The source buffer providing the data.
     * @param dst The destination buffer receiving the data.
     */
    public static void blit(ByteBuffer src, ByteBuffer dst) {
        if (!isValidBlitTransfer(src, dst)) return;
        int count = src.remaining();
        if (src.isDirect() && dst.isDirect()) {
            MemoryUtil.memCopy(MemoryUtil.memAddress(src) + src.position(), MemoryUtil.memAddress(dst) + dst.position(), count);
        } else if (!src.isDirect() && !dst.isDirect() && src.hasArray() && dst.hasArray()) {
            System.arraycopy(src.array(), src.arrayOffset() + src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else if (!src.isDirect() && dst.isDirect() && src.hasArray()) {
            dst.put(dst.position(), src.array(), src.arrayOffset() + src.position(), count);
        } else if (src.isDirect() && !dst.isDirect() && dst.hasArray()) {
            src.get(src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else { int srcPos = src.position();
            int dstPos = dst.position();
            for (int i = 0; i < count; i++) dst.put(dstPos + i, src.get(srcPos + i));
        }
    }
    /** @see #blit(ByteBuffer, ByteBuffer) */
    public static void blit(ShortBuffer src, ShortBuffer dst) {
        if (!isValidBlitTransfer(src, dst)) return;
        int count = src.remaining();
        if (src.isDirect() && dst.isDirect()) {
            MemoryUtil.memCopy(
                    MemoryUtil.memAddress(src) + ((long) src.position() * Short.BYTES),
                    MemoryUtil.memAddress(dst) + ((long) dst.position() * Short.BYTES),
                    (long) count * Short.BYTES);
        } else if (!src.isDirect() && !dst.isDirect() && src.hasArray() && dst.hasArray()) {
            System.arraycopy(src.array(), src.arrayOffset() + src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else if (!src.isDirect() && dst.isDirect() && src.hasArray()) {
            dst.put(dst.position(), src.array(), src.arrayOffset() + src.position(), count);
        } else if (src.isDirect() && !dst.isDirect() && dst.hasArray()) {
            src.get(src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else {
            int srcPos = src.position();
            int dstPos = dst.position();
            for (int i = 0; i < count; i++) dst.put(dstPos + i, src.get(srcPos + i));
        }
    }
    /** @see #blit(ByteBuffer, ByteBuffer) */
    public static void blit(IntBuffer src, IntBuffer dst) {
        if (!isValidBlitTransfer(src, dst)) return;
        int count = src.remaining();
        if (src.isDirect() && dst.isDirect()) {
            MemoryUtil.memCopy(
                    MemoryUtil.memAddress(src) + ((long) src.position() * Integer.BYTES),
                    MemoryUtil.memAddress(dst) + ((long) dst.position() * Integer.BYTES),
                    (long) count * Integer.BYTES);
        } else if (!src.isDirect() && !dst.isDirect() && src.hasArray() && dst.hasArray()) {
            System.arraycopy(src.array(), src.arrayOffset() + src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else if (!src.isDirect() && dst.isDirect() && src.hasArray()) {
            dst.put(dst.position(), src.array(), src.arrayOffset() + src.position(), count);
        } else if (src.isDirect() && !dst.isDirect() && dst.hasArray()) {
            src.get(src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else {
            int srcPos = src.position();
            int dstPos = dst.position();
            for (int i = 0; i < count; i++) dst.put(dstPos + i, src.get(srcPos + i));
        }
    }
    /** @see #blit(ByteBuffer, ByteBuffer) */
    public static void blit(FloatBuffer src, FloatBuffer dst) {
        if (!isValidBlitTransfer(src, dst)) return;
        int count = src.remaining();
        if (src.isDirect() && dst.isDirect()) {
            MemoryUtil.memCopy(
                    MemoryUtil.memAddress(src) + ((long) src.position() * Float.BYTES),
                    MemoryUtil.memAddress(dst) + ((long) dst.position() * Float.BYTES),
                    (long) count * Float.BYTES);
        } else if (!src.isDirect() && !dst.isDirect() && src.hasArray() && dst.hasArray()) {
            System.arraycopy(src.array(), src.arrayOffset() + src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else if (!src.isDirect() && dst.isDirect() && src.hasArray()) {
            dst.put(dst.position(), src.array(), src.arrayOffset() + src.position(), count);
        } else if (src.isDirect() && !dst.isDirect() && dst.hasArray()) {
            src.get(src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else {
            int srcPos = src.position();
            int dstPos = dst.position();
            for (int i = 0; i < count; i++) dst.put(dstPos + i, src.get(srcPos + i));
        }
    }

    private static boolean isValidBlitTransfer(Buffer src, Buffer dst) {
        if (dst.isReadOnly()) {
            Logger.warn("Buffer blit: Unable to write to read-only buffer");
            return false;
        } int count = src.remaining();
        if (count == 0) {
            Logger.warn("Buffer blit: Nothing to write. Src buffer remaining = 0");
            return false;
        } if (dst.remaining() < count) {
            Logger.warn("Buffer blit: Nothing written. Dst buffer required: {}, available: {}", count, dst.remaining());
            return false;
        } return true;
    }




}
