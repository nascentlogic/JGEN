package io.github.nascentlogic.jgen.gfx;

import io.github.nascentlogic.jgen.Jgen;
import io.github.nascentlogic.jgen.utils.Disposable;
import org.joml.Vector2i;
import org.joml.Vector4i;
import org.lwjgl.system.MemoryStack;
import org.tinylog.Logger;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.GL_NONE;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

/**
 * F.Dahl, 7/2/2026
 */
public class Framebuffer implements Disposable {

    private record Attachment(Texture texture, boolean dispose) {}
    private static final int MAX_COLOR_ATTACHMENTS = 8;
    private static final int MAX_DRAW_BUFFERS = 8;
    private static int DRAW_BUFFER = GL_NONE;
    private static int READ_BUFFER = GL_NONE;

    private final Attachment[] attachments = new Attachment[MAX_COLOR_ATTACHMENTS];
    private final int handle;
    private final int width;
    private final int height;
    private boolean disposed;
    private int depthStencilAttachement = GL_NONE;


    public Framebuffer(Vector2i resolution) { this(resolution.x,resolution.y); }
    public Framebuffer(int width, int height) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Unreasonable Framebuffer dimensions");
        this.width = width;
        this.height = height;
        this.handle = glGenFramebuffers();
    }

    public int handle() { return handle; }
    public int width() { return width; }
    public int height() { return height; }
    public boolean isDisposed() { return disposed; }

    public void attachTexture(Texture texture, int slot, boolean disposeWithFbo) {
        Objects.requireNonNull(texture, "Illegal null argument for color attachment");
        if (disposed) throw new IllegalStateException("Illegal attach call on disposed Framebuffer");
        if (texture.isDisposed() || !texture.isAllocated())
            throw new IllegalStateException("Cannot attach disposed or unallocated texture to Framebuffer");
        if (slot < 0 || slot >= MAX_COLOR_ATTACHMENTS)
            throw new IllegalArgumentException("Attachment slot out of bounds: " + slot);
        if (width != texture.width() || height != texture.height())
            throw new IllegalArgumentException("Invalid Framebuffer attachment dimensions (must match FBO size)");
        if (attachments[slot] != null)
            throw new IllegalStateException("Framebuffer slot [" + slot + "] is already populated");
        bindDraw();
        // glFramebufferTexture universally handles standard 2D, Cube Maps, and Multisample textures automatically
        glFramebufferTexture(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + slot, texture.handle(), 0);
        attachments[slot] = new Attachment(texture, disposeWithFbo);
    }

    public void attachTextureLayer(Texture texture, int slot, int layer) {
        Objects.requireNonNull(texture, "Illegal null argument for color attachment");
        if (disposed) throw new IllegalStateException("Illegal attach call on disposed Framebuffer");
        if (texture.isDisposed() || !texture.isAllocated())
            throw new IllegalStateException("Cannot attach disposed or unallocated texture to Framebuffer");
        if (slot < 0 || slot >= MAX_COLOR_ATTACHMENTS)
            throw new IllegalArgumentException("Attachment slot out of bounds: " + slot);
        if (width != texture.width() || height != texture.height())
            throw new IllegalArgumentException("Invalid Framebuffer attachment dimensions (must match FBO size)");
        // Support both standard arrays and future multisample texture arrays
        if (texture.target() != GL_TEXTURE_2D_ARRAY && texture.target() != GL_TEXTURE_2D_MULTISAMPLE_ARRAY)
            throw new IllegalArgumentException("Attach requires a texture array target");
        if (texture.depth() <= layer || layer < 0)
            throw new IllegalArgumentException("No such Texture array layer: " + layer);
        if (attachments[slot] != null)
            throw new IllegalStateException("Framebuffer slot [" + slot + "] is already populated");
        bindDraw();
        glFramebufferTextureLayer(GL_DRAW_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + slot, texture.handle(), 0, layer);
        attachments[slot] = new Attachment(texture, false);
    }

    public void attachDepthStencil(boolean multisampled) {
        if (disposed) throw new IllegalStateException("Illegal attach call on disposed Framebuffer");
        if (depthStencilAttachement == GL_NONE) {
            depthStencilAttachement = glGenRenderbuffers();
            bindDraw();
            glBindRenderbuffer(GL_RENDERBUFFER,depthStencilAttachement);
            if (multisampled) {
                glRenderbufferStorageMultisample(GL_RENDERBUFFER, Texture.MSAA_SAMPLES, GL_DEPTH24_STENCIL8, width,height);
            } else glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, width,height);
            glBindRenderbuffer(GL_RENDERBUFFER,GL_NONE);
            glFramebufferRenderbuffer(GL_DRAW_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, depthStencilAttachement);
        } else Logger.warn("Depth stencil buffer already attached");
    }

    public Texture attachment(int slot) {
        if (attachmentExist(slot)) {
            return attachments[slot].texture;
        } return null;
    }

    public void bindDraw() {
        if (disposed) throw new IllegalStateException("Illegal bind on disposed Framebuffer");
        if (DRAW_BUFFER != handle) {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER,handle);
            DRAW_BUFFER = handle;
        }
    }

    public void bindRead() {
        if (disposed) throw new IllegalStateException("Illegal bind on disposed Framebuffer");
        if (READ_BUFFER != handle) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER,handle);
            READ_BUFFER = handle;
        }
    }

    public void unbind() {
        if (disposed) throw new IllegalStateException("Illegal unbind on disposed Framebuffer");
        if (handle == DRAW_BUFFER) {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, GL_NONE);
            DRAW_BUFFER = GL_NONE;
        } if (handle == READ_BUFFER) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, GL_NONE);
            READ_BUFFER = GL_NONE;
        }
    }

    public void viewport() {
        if (DRAW_BUFFER != handle) throw new IllegalStateException("Framebuffer is not currently bound for draw");
        glViewport(0,0,width,height);
    }

    public void readBuffer(int slot) {
        if (DRAW_BUFFER != handle) throw new IllegalStateException("Framebuffer is not currently bound for read");
        glReadBuffer(GL_COLOR_ATTACHMENT0 + slot);
    }

    public void drawBuffer(int slot) {
        if (DRAW_BUFFER != handle) throw new IllegalStateException("Framebuffer is not currently bound for draw");
        glDrawBuffer(GL_COLOR_ATTACHMENT0 + slot);
    }

    public void drawbuffers(int slot1, int slot2) {
        if (DRAW_BUFFER != handle) throw new IllegalStateException("Framebuffer is not currently bound for draw");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int i = GL_COLOR_ATTACHMENT0;
            IntBuffer buffer = stack.ints(i+slot1,i+slot2);
            glDrawBuffers(buffer);
        }
    }

    public void drawbuffers(int slot1, int slot2, int slot3) {
        if (DRAW_BUFFER != handle) throw new IllegalStateException("Framebuffer is not currently bound for draw");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int i = GL_COLOR_ATTACHMENT0;
            IntBuffer buffer = stack.ints(i+slot1,i+slot2,i+slot3);
            glDrawBuffers(buffer);
        }
    }

    public void drawbuffers(int slot1, int slot2, int slot3, int slot4) {
        if (DRAW_BUFFER != handle) throw new IllegalStateException("Framebuffer is not currently bound for draw");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int i = GL_COLOR_ATTACHMENT0;
            IntBuffer buffer = stack.ints(i+slot1,i+slot2,i+slot3,i+slot4);
            glDrawBuffers(buffer);
        }
    }

    public void drawBuffers(int... slots) {
        if (DRAW_BUFFER != handle) throw new IllegalStateException("Framebuffer is not currently bound for draw");
        if (slots == null || slots.length == 0) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.mallocInt(slots.length);
            for (int slot : slots) {
                buffer.put(GL_COLOR_ATTACHMENT0 + slot);
            } glDrawBuffers(buffer.flip());
        }
    }

    public void clearColor(int drawBufferIndex, float r, float g, float b, float a) {
        if (DRAW_BUFFER != handle) throw new IllegalStateException("Framebuffer is not currently bound for draw");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.floats(r, g, b, a);
            glClearBufferfv(GL_COLOR, drawBufferIndex, buffer);
        }
    }

    public void clearColorInt(int drawBufferIndex, int r, int g, int b, int a) {
        if (DRAW_BUFFER != handle) throw new IllegalStateException("Framebuffer is not currently bound for draw");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.ints(r, g, b, a);
            glClearBufferiv(GL_COLOR, drawBufferIndex, buffer);
        }
    }

    public void clearColorUint(int drawBufferIndex, int r, int g, int b, int a) {
        if (DRAW_BUFFER != handle) throw new IllegalStateException("Framebuffer is not currently bound for draw");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer buffer = stack.ints(r, g, b, a);
            glClearBufferuiv(GL_COLOR, drawBufferIndex, buffer);
        }
    }

    /**
     * Clears the depth buffer attachment.
     * <p>
     * <strong>NOTE:</strong> This operation is strictly subject to the global OpenGL depth write mask.
     * If the depth mask has been disabled elsewhere via {@code glDepthMask(false)}, this clear
     * command will be ignored by the hardware. Ensure {@code glDepthMask(true)} is active prior
     * to calling if a complete buffer reset is required.
     * </p>
     *
     * @param value The depth value to clear the buffer with (typically 1.0f for standard reverse-Z
     * or clear passes).
     */
    public void clearDepth(float value) {
        if (DRAW_BUFFER != handle) throw new IllegalStateException("Framebuffer is not currently bound for draw");
        if (depthStencilAttached()) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer buffer = stack.floats(value);
                glClearBufferfv(GL_DEPTH,0,buffer);
            }
        }
    }

    /**
     * Clears the stencil buffer attachment.
     * <p>
     * <strong>NOTE:</strong> This operation is strictly subject to the global OpenGL stencil write mask.
     * If the stencil mask has been restricted elsewhere via {@code glStencilMask(mask)}, only the
     * unmasked bits will be updated. Ensure {@code glStencilMask(0xFF)} is active prior to calling
     * if a complete buffer reset is required.
     * </p>
     *
     * @param value The integer value to clear the stencil buffer with. The value is automatically
     * masked with {@code 0xFF} to fit the standard 8-bit stencil storage limits.
     */
    public void clearStencil(int value) {
        if (DRAW_BUFFER != handle) throw new IllegalStateException("Framebuffer is not currently bound for draw");
        if (depthStencilAttached()) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer buffer = stack.ints(value & 0xFF);
                glClearBufferiv(GL_STENCIL,0,buffer);
            }
        }
    }

    /**
     * Clears both the depth and stencil buffer attachments simultaneously.
     * <p>
     * <strong>NOTE:</strong> This operation is subject to <em>both</em> the global depth and stencil
     * write masks. If either {@code glDepthMask(false)} or a restrictive {@code glStencilMask} is
     * currently active in the OpenGL context, the corresponding clear operation for that channel
     * will be blocked or masked out. Ensure masks are completely open ({@code true} and {@code 0xFF})
     * prior to calling for a total buffer wipe.
     * </p>
     *
     * @param depthValue   The depth value to clear the buffer with (typically 1.0f).
     * @param stencilValue The integer value to clear the stencil buffer with. The value is automatically
     * masked with {@code 0xFF} to fit standard 8-bit stencil storage limits.
     */
    public void clearDepthStencil(float depthValue, int stencilValue) {
        if (DRAW_BUFFER != handle) throw new IllegalStateException("Framebuffer is not currently bound for draw");
        if (depthStencilAttached()) {
            glClearBufferfi(GL_DEPTH_STENCIL, 0, depthValue, stencilValue & 0xFF);
        }
    }


    public void free() {
        if (disposed) return;
        unbind();
        if (depthStencilAttachement != GL_NONE) {
            glDeleteRenderbuffers(depthStencilAttachement);
            depthStencilAttachement = GL_NONE;
        } for (int i = 0; i < attachments.length; i++) {
            Attachment attachment = attachments[i];
            if (attachment != null && attachment.dispose)
                Disposable.free(attachment.texture);
            attachments[i] = null;
        } glDeleteFramebuffers(handle);
        disposed = true;
    }

    public boolean attachmentExist(int slot) {
        if (slot < 0 || slot >= attachments.length) return false;
        return attachments[slot] != null;
    }

    public boolean depthStencilAttached() {
        return depthStencilAttachement != GL_NONE;
    }


    public static void bindDefaultDraw() {
        if (DRAW_BUFFER != GL_NONE) {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER,GL_NONE);
            DRAW_BUFFER = GL_NONE;
        }
    }

    public static void bindDefaultRead() {
        if (READ_BUFFER != GL_NONE) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER,GL_NONE);
            READ_BUFFER = GL_NONE;
        }
    }

    /**
     * Clears the default GLFW window framebuffer's color channel.
     * <p>
     * <strong>NOTE:</strong> This operation explicitly binds the default draw target (0) and is
     * subject to the global OpenGL color write mask. If color writing has been disabled elsewhere
     * via {@code glColorMask(false, false, false, false)} (e.g., during a shadow map pass), this
     * clear command will be ignored by the hardware. Ensure color writing is enabled prior to
     * calling for a complete screen reset.
     * </p>
     *
     * @param r The red component (0.0f to 1.0f).
     * @param g The green component (0.0f to 1.0f).
     * @param b The blue component (0.0f to 1.0f).
     * @param a The alpha component (0.0f to 1.0f).
     */
    public static void clearDefault(float r, float g, float b, float a) {
        bindDefaultDraw();
        glClearColor(r, g, b, a);
        glClear(GL_COLOR_BUFFER_BIT);
    }

    private static final Vector4i viewport = new Vector4i();
    public static void viewportDefault() {
        Jgen.get().window().viewport(viewport);
        glViewport(viewport.x, viewport.y, viewport.z, viewport.w);
    }

    public static int maxDrawBuffers() {
        return MAX_DRAW_BUFFERS;
    }

    public static int maxColorAttachments() {
        return MAX_COLOR_ATTACHMENTS;
    }

    public static void checkDrawStatus() {
        int status = glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            String message = switch (status) {
                case GL_FRAMEBUFFER_UNDEFINED                       -> ": Framebuffer undefined";
                case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT           -> ": Incomplete attachment";
                case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT   -> ": Missing attachment";
                case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER          -> ": Incomplete draw buffer";
                case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER          -> ": Incomplete read buffer";
                case GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE          -> ": Attachment object type";
                case GL_FRAMEBUFFER_UNSUPPORTED                     -> ": Framebuffer unsupported";
                case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE          -> ": Incomplete multi-sample";
                case GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS        -> ": Incomplete layer targets";
                default                                             -> ": Unknown error";
            }; Logger.warn("Framebuffer draw status: {}", message);
        }
    }

    public static void checkReadStatus() {
        int status = glCheckFramebufferStatus(GL_READ_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            String message = switch (status) {
                case GL_FRAMEBUFFER_UNDEFINED                       -> ": Framebuffer undefined";
                case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT           -> ": Incomplete attachment";
                case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT   -> ": Missing attachment";
                case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER          -> ": Incomplete draw buffer";
                case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER          -> ": Incomplete read buffer";
                case GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE          -> ": Attachment object type";
                case GL_FRAMEBUFFER_UNSUPPORTED                     -> ": Framebuffer unsupported";
                case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE          -> ": Incomplete multi-sample";
                case GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS        -> ": Incomplete layer targets";
                default                                             -> ": Unknown error";
            }; Logger.warn("Framebuffer read status: {}", message);
        }
    }
}
