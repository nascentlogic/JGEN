package io.github.nascentlogic.jgen.gfx;

import io.github.nascentlogic.jgen.utils.Disposable;

import java.nio.*;

import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;

/**
 * F.Dahl, 7/5/2026
 */
public class UniformBuffer implements Disposable {

    private final int handle;
    private final int size;
    private boolean disposed;

    /** Constructor leaves this buffer bound as the active GL_UNIFORM_BUFFER target. */
    public UniformBuffer(int size, boolean dynamic) {
        this.handle = Buffers.generateUBO(dynamic,size);
        this.size = size;
    }

    public int size() { return size; }
    public int handle() { return handle; }
    public boolean isDisposed() { return disposed; }

    /** Bind the UniformBuffer to the correct binding point (slot), as configured in the Shader
     * @see Buffers#bindBufferBase(int, int, int)  */
    public void bindToSlot(int slot) {
        if (disposed) throw new IllegalStateException("Cannot bind a disposed Uniform Buffer");
        Buffers.bindBufferBase(GL_UNIFORM_BUFFER, slot, handle);
    }

    /** @see #upload(int, Buffer) */
    public void upload(Buffer data) { upload(0,data); }

    /**
     * Binds to target and uploads data
     * @param offset in bytes
     * @see Buffers#upload(int, int, Buffer)
     * */
    public void upload(int offset, Buffer data) {
        if (disposed) throw new IllegalStateException("Cannot upload to a disposed Uniform Buffer");
        Buffers.bindUBO(handle);
        Buffers.upload(GL_UNIFORM_BUFFER,offset,data);
    }

    public void free() {
        if (disposed) return;
        Buffers.deleteBuffer(handle);
        disposed = true;
    }

    public static void unbindSlot(int slot) {
        Buffers.bindBufferBase(GL_UNIFORM_BUFFER, slot, 0);
    }
}
