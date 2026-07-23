package io.github.nascentlogic.jgen.gfx;

import io.github.nascentlogic.jgen.Jgen;
import io.github.nascentlogic.jgen.utils.Disposable;
import io.github.nascentlogic.jgen.utils.JgenMath;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;
import org.tinylog.Logger;

import java.nio.*;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_1D_ARRAY;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;
import static org.lwjgl.opengl.GL42.*;


/**
 * F.Dahl, 6/30/2026
 */
public class Texture implements Disposable {

    public static final int MSAA_SAMPLES = 4;
    private static long MEMORY_USED;
    private static long MEMORY_PEAK;
    private static final int[] SUPPORTED_TEXTURE_TARGETS = {
            // GL_TEXTURE_2D_MULTISAMPLE <-- later
            GL_TEXTURE_1D,
            GL_TEXTURE_2D,
            GL_TEXTURE_3D,
            GL_TEXTURE_1D_ARRAY,
            GL_TEXTURE_2D_ARRAY
    };

    private TextureFormat format;
    private final int handle, target;
    private final int width, height, depth;
    private int mipLevels;
    private long allocatedBytes;
    private boolean disposed;
    private boolean allocated;

    public static Texture generate1D(int width) { return new Texture(width,1,1,GL_TEXTURE_1D); }
    public static Texture generate2D(int width, int height) { return new Texture(width,height,1,GL_TEXTURE_2D); }
    public static Texture generate3D(int width, int height, int depth) { return new Texture(width,height,depth,GL_TEXTURE_3D); }
    public static Texture generateArray1D(int width, int layers) { return new Texture(width,layers,1,GL_TEXTURE_1D_ARRAY); }
    public static Texture generateArray2D(int width, int height, int layers) { return new Texture(width,height,layers,GL_TEXTURE_2D_ARRAY); }
    private Texture(int width, int height, int depth, int target) {
        if (!isSupportedTarget(target)) throw new IllegalArgumentException("Unsupported texture target");
        if (width < 1 || height < 1 || depth < 1) throw new IllegalArgumentException("Texture dimensions must be > 0");
        this.handle = glGenTextures();
        this.width  = width;
        this.height = height;
        this.depth  = depth;
        this.target = target;
    }

    public int handle() { return handle; }
    public int target() { return target; }
    public int width() { return width; }
    public int height() { return height; }
    public int depth() { return depth; }
    public int mipLevels() { return mipLevels; }
    public long byteSize() { return allocatedBytes; }
    public boolean isDisposed() { return disposed; }
    public boolean isAllocated() { return allocated; }
    public TextureFormat format() { return format; }

    public void allocate(TextureFormat format, boolean mipmap) {
        if (disposed) throw new IllegalStateException("Texture has been disposed");
        if (allocated) throw new IllegalStateException("Texture already allocated");
        this.format = Objects.requireNonNull(format);
        this.mipLevels = mipmap ? calcMipLevels(width,height,depth) : 1;
        bindToSlot(0);
        if (Jgen.get().supportsImmutableStorage()) {
            switch (target) {
                case GL_TEXTURE_1D -> glTexStorage1D(target,mipLevels,format.sizedFormat,width);
                case GL_TEXTURE_2D, GL_TEXTURE_1D_ARRAY -> glTexStorage2D(target,mipLevels,format.sizedFormat,width,height);
                case GL_TEXTURE_3D, GL_TEXTURE_2D_ARRAY -> glTexStorage3D(target,mipLevels,format.sizedFormat,width,height,depth);
                default -> throw new IllegalStateException("Unsupprted texture target: " + target);
            }
        } else { // macOS (OpenGL version 4.1)
            for (int level = 0; level < mipLevels; level++) {
                int mipW  = Math.max(1, width  >> level);
                int mipH  = Math.max(1, height >> level);
                int mipD  = Math.max(1, depth  >> level);
                switch (target) {
                    case GL_TEXTURE_1D -> glTexImage1D(target, level, format.sizedFormat, mipW, 0, format.format, format.dataType, 0);
                    case GL_TEXTURE_2D -> glTexImage2D(target, level, format.sizedFormat, mipW, mipH, 0, format.format, format.dataType, 0);
                    case GL_TEXTURE_3D -> glTexImage3D(target, level, format.sizedFormat, mipW, mipH, mipD, 0, format.format, format.dataType, 0);
                    case GL_TEXTURE_1D_ARRAY -> glTexImage2D(target, level, format.sizedFormat, mipW, height, 0, format.format, format.dataType, 0);
                    case GL_TEXTURE_2D_ARRAY -> glTexImage3D(target, level, format.sizedFormat, mipW, mipH, depth, 0, format.format, format.dataType, 0);
                    default -> throw new IllegalStateException("Unsupported texture target: " + target);
                }
            }
        }
        this.allocated = true;
        this.allocatedBytes = switch (target) {
            case GL_TEXTURE_1D -> sizeOf1D(format,width,mipmap);
            case GL_TEXTURE_2D -> sizeOf2D(format,width,height,mipmap);
            case GL_TEXTURE_3D -> sizeOf3D(format,width,height,depth,mipmap);
            case GL_TEXTURE_1D_ARRAY -> height * sizeOf1D(format,width,mipmap);
            case GL_TEXTURE_2D_ARRAY -> depth * sizeOf2D(format,width,height,mipmap);
            default -> 0;
        };
        MEMORY_USED += allocatedBytes;
        MEMORY_PEAK = Math.max(MEMORY_PEAK,MEMORY_USED);
        Jgen.glCheckError();
    }

    public void generateMipmap() {
        if (disposed || !allocated) throw new IllegalStateException("Texture is disposed or unallocated");
        if (mipLevels > 1) glGenerateMipmap(target);
        else Logger.warn("Attempt to generate mipmaps on texture not allocated for mipmaps");
    }

    public int bindToSlot(int slot) {
        if (slot < 0) throw new IllegalArgumentException("Texture slot cannot be negative");
        if (disposed) throw new IllegalStateException("Texture has been disposed");
        glActiveTexture(slot + GL_TEXTURE0);
        glBindTexture(target,handle);
        return slot;
    }

    public void free() {
        if (!disposed) {
            glDeleteTextures(handle);
            MEMORY_USED -= allocatedBytes;
            allocated = false;
            disposed = true;
        }
    }

    public Vector3i mipLevelSize(int level, Vector3i dst) {
        if (level > (mipLevels - 1)) Logger.warn("Texture has no mipmap for level[{}]",level);
        int mipW = Math.max(1, width  >> level);
        int mipH = Math.max(1, height >> level);
        int mipD = Math.max(1, depth  >> level);
        return dst.set(mipW, mipH, mipD);
    }

    public void parameteri(int pname, int param) { glTexParameteri(target, pname, param); }
    public void parameterf(int pname, float param) { glTexParameterf(target, pname, param); }
    public void filterNearest() { filter(GL_NEAREST,GL_NEAREST); }
    public void filterLinear() { filter(GL_LINEAR,GL_LINEAR); }
    public void filter(int min, int mag) {
        if (target == GL_TEXTURE_2D_MULTISAMPLE)
            Logger.info("No texture filter for multisample textures");
        parameteri(GL_TEXTURE_MIN_FILTER, min);
        parameteri(GL_TEXTURE_MAG_FILTER, mag);
    }

    public void wrapRepeat() { wrap(GL_REPEAT); }
    public void clampToEdge() { wrap(GL_CLAMP_TO_EDGE); }
    public void clampToBorder() { wrap(GL_CLAMP_TO_BORDER); }
    public void wrap(int wrap) {
        switch (target) {
            case GL_TEXTURE_1D -> parameteri(GL_TEXTURE_WRAP_S, wrap);
            case GL_TEXTURE_2D, GL_TEXTURE_1D_ARRAY -> {
                parameteri(GL_TEXTURE_WRAP_S, wrap);
                parameteri(GL_TEXTURE_WRAP_T, wrap);
            } case GL_TEXTURE_3D, GL_TEXTURE_2D_ARRAY -> {
                parameteri(GL_TEXTURE_WRAP_S, wrap);
                parameteri(GL_TEXTURE_WRAP_T, wrap);
                parameteri(GL_TEXTURE_WRAP_R, wrap);
            } case GL_TEXTURE_2D_MULTISAMPLE ->
                    Logger.info("No texture wrap for multisample textures");
        }
    }

    /** Uploads 1D pixel row data or a sub-region of a 1D texture.<p>
     * <b>Target Interpretations:</b>
     * <ul><li><b>GL_TEXTURE_1D:</b> xOff represents the horizontal pixel offset. width represents the length of the pixel row.</li></ul>
     * @param level  the mipmap level to update (0 for base)
     * @param xOff   the x-coordinate pixel offset
     * @param width  the pixel width of the data region
     * @param data   the direct NIO buffer containing the pixel data
     * @see <a href="https://docs.gl/gl4/glTexSubImage1D">glTexSubImage1D Reference</a> */
    public void upload1D(int level, int xOff, int width, Buffer data) {
        if (disposed || !allocated) throw new IllegalStateException("Texture is disposed or unallocated");
        glPixelStorei(GL_UNPACK_ALIGNMENT, format.alignment());
        texSubImage1D(level, xOff, width, data);
    }

    /** Uploads 2D pixel planar data, or a sub-region of a 1D Texture Array.<p>
     * <b>Target Interpretations:</b>
     * <ul>
     * <li><b>GL_TEXTURE_2D:</b> xOff and yOff represent standard 2D spatial pixel offsets. width and height represent the dimensions of the bounding box.</li>
     * <li><b>GL_TEXTURE_1D_ARRAY:</b> yOff represents the starting layer index. height represents the number of sequential layers to update.</li>
     * </ul>
     * @param level  the mipmap level to update (0 for base)
     * @param xOff   the x-coordinate pixel offset
     * @param yOff   the y-coordinate pixel offset or 1D array layer index
     * @param width  the pixel width of the data region
     * @param height the pixel height of the data region or total layer count
     * @param data   the direct NIO buffer containing the pixel data
     * @see <a href="https://docs.gl/gl4/glTexSubImage2D">glTexSubImage2D Reference</a> */
    public void upload2D(int level, int xOff, int yOff, int width, int height, Buffer data) {
        if (disposed || !allocated) throw new IllegalStateException("Texture is disposed or unallocated");
        glPixelStorei(GL_UNPACK_ALIGNMENT, format.alignment());
        texSubImage2D(level, xOff, yOff, width, height, data);
    }

    /** Uploads 3D pixel volumetric data, or a sub-region of a 2D Texture Array / Cube Map.<p>
     * <b>Target Interpretations:</b>
     * <ul>
     * <li><b>GL_TEXTURE_3D:</b> xOff, yOff, zOff represent spatial coordinates. depth represents voxel depth.</li>
     * <li><b>GL_TEXTURE_2D_ARRAY:</b> zOff represents the starting layer index. depth represents the number of layers to update.</li>
     * <li><b>GL_TEXTURE_CUBE_MAP:</b> zOff represents the cube face index (0-5). depth should typically be 1.</li>
     * </ul>
     * @param level  the mipmap level to update (0 for base)
     * @param xOff   the x-coordinate pixel offset
     * @param yOff   the y-coordinate pixel offset
     * @param zOff   the z-coordinate voxel offset or layer/face index
     * @param width  the pixel width of the data region
     * @param height the pixel height of the data region
     * @param depth  the voxel depth or total layer count of the data region
     * @param data   the direct NIO buffer containing the pixel data
     * @see <a href="https://docs.gl/gl4/glTexSubImage3D">glTexSubImage3D Reference</a> */
    public void upload3D(int level, int xOff, int yOff, int zOff, int width, int height, int depth, Buffer data) {
        if (disposed || !allocated) throw new IllegalStateException("Texture is disposed or unallocated");
        glPixelStorei(GL_UNPACK_ALIGNMENT, format.alignment());
        texSubImage3D(level, xOff, yOff, zOff, width, height, depth, data);
    }

    /** Automatically uploads bulk pixel data to cover the entire base level (Level 0) of the texture.<p>
     * This method automatically queries the texture's structural target type and roots the dimensions
     * and offsets to cover the entire texture canvas.<p>
     * <b>Warning:</b> This method only targets the base level 0. For multi-layered array textures, the
     * provided buffer must contain the pixel data for <i>all</i> layers contiguous in memory. For updating
     * specific sub-regions or individual layers, use the granular dimensional upload methods instead.
     * @param data the direct NIO buffer containing the full base-level pixel data */
    public void upload(Buffer data) {
        switch (target) {
            case GL_TEXTURE_1D -> upload1D(0, 0, width, data);
            case GL_TEXTURE_2D, GL_TEXTURE_1D_ARRAY -> upload2D(0, 0, 0, width, height, data);
            case GL_TEXTURE_3D, GL_TEXTURE_2D_ARRAY -> upload3D(0, 0, 0, 0, width, height, depth, data);
            default -> throw new IllegalStateException("Unsupported automatic upload for target: " + target);
        }
    }

    /**
     * Downloads pixel data for a specific mipmap level into the provided direct NIO buffer.
     * @param level the mipmap level to download (0 for base level)
     * @param dst   the direct destination NIO buffer (must be large enough to hold the pixel data)
     */
    public void download(int level, Buffer dst) {
        if (disposed || !allocated) throw new IllegalStateException("Texture is disposed or unallocated");
        if (level < 0 || level >= mipLevels) throw new IllegalArgumentException("Invalid mipmap level: " + level);
        glPixelStorei(GL_PACK_ALIGNMENT, format.alignment());
        getTexImage(level, dst);
    }

    /**
     * Downloads the full base level (Level 0) pixel data into the provided direct NIO buffer.
     * @param dst the direct destination NIO buffer
     */
    public void download(Buffer dst) {
        download(0, dst);
    }

    /**
     * Downloads pixel data for a specific mipmap level into a direct ByteBuffer.
     * @param level the mipmap level to download (0 for base level)
     * @return a direct ByteBuffer containing the pixel data
     */
    public ByteBuffer downloadBytes(int level) {
        Vector3i dims = mipLevelSize(level, new Vector3i());
        int sizeBytes = dims.x * dims.y * dims.z * format.bytesPerPixel;
        ByteBuffer pixels = MemoryUtil.memAlloc(sizeBytes);
        try { download(level, pixels);
            return pixels;
        } catch (Throwable t) {
            MemoryUtil.memFree(pixels);
            throw t;
        }
    }

    /**
     * Downloads the full base level (Level 0) pixel data into a direct ByteBuffer
     * @return a direct ByteBuffer containing base level pixel data
     */
    public ByteBuffer downloadBytes() {
        return downloadBytes(0);
    }

    /**
     * Downloads the base level (Level 0) of a 2D texture into a new Bitmap object.
     * Note: 2D Texture (R8, RG8, RGB8, RGBA8, SRGB or SRGBA8)
     * @return a new Bitmap containing the texture's pixel data
     */
    public Bitmap toBitmap() {
        if (target != GL_TEXTURE_2D) throw new UnsupportedOperationException("toBitmap() only support 2D textures");
        return new Bitmap(downloadBytes(), width, height, format.channels);
    }

    /**
     * Downloads a specific mipmap level of a 2D texture into a new Bitmap object.
     * Note: 2D Texture (R8, RG8, RGB8, RGBA8, SRGB or SRGBA8)
     * @return a new Bitmap containing the texture's pixel data
     */
    public Bitmap toBitmap(int level) {
        if (target != GL_TEXTURE_2D) throw new UnsupportedOperationException("toBitmap() only support 2D textures");
        Vector3i dims = mipLevelSize(level, new Vector3i());
        return new Bitmap(downloadBytes(level),dims.x,dims.y,format.channels);
    }

    private void texSubImage1D(int level, int xOff, int w, Buffer data) {
        int f = format.format;
        int t = format.dataType;
        switch (data) {
            case ByteBuffer b  -> glTexSubImage1D(target, level, xOff, w, f, t, b);
            case FloatBuffer b -> glTexSubImage1D(target, level, xOff, w, f, t, b);
            case IntBuffer b   -> glTexSubImage1D(target, level, xOff, w, f, t, b);
            case ShortBuffer b -> glTexSubImage1D(target, level, xOff, w, f, t, b);
            default -> throw new IllegalArgumentException("Unsupported buffer: " + data.getClass().getName());
        } Jgen.glCheckError();
    }

    private void texSubImage2D(int level, int xOff, int yOff, int w, int h, Buffer data) {
        int f = format.format;
        int t = format.dataType;
        switch (data) {
            case ByteBuffer b  -> glTexSubImage2D(target, level, xOff, yOff, w, h, f, t, b);
            case FloatBuffer b -> glTexSubImage2D(target, level, xOff, yOff, w, h, f, t, b);
            case IntBuffer b   -> glTexSubImage2D(target, level, xOff, yOff, w, h, f, t, b);
            case ShortBuffer b -> glTexSubImage2D(target, level, xOff, yOff, w, h, f, t, b);
            default -> throw new IllegalArgumentException("Unsupported buffer: " + data.getClass().getName());
        } Jgen.glCheckError();
    }

    private void texSubImage3D(int level, int xOff, int yOff, int zOff, int w, int h, int d, Buffer data) {
        int f = format.format;
        int t = format.dataType;
        switch (data) {
            case ByteBuffer b  -> glTexSubImage3D(target, level, xOff, yOff, zOff, w, h, d, f, t, b);
            case FloatBuffer b -> glTexSubImage3D(target, level, xOff, yOff, zOff, w, h, d, f, t, b);
            case IntBuffer b   -> glTexSubImage3D(target, level, xOff, yOff, zOff, w, h, d, f, t, b);
            case ShortBuffer b -> glTexSubImage3D(target, level, xOff, yOff, zOff, w, h, d, f, t, b);
            default -> throw new IllegalArgumentException("Unsupported buffer: " + data.getClass().getName());
        } Jgen.glCheckError();
    }

    private void getTexImage(int level, Buffer dst) {
        int f = format.format;
        int t = format.dataType;
        switch (dst) {
            case ByteBuffer b  -> glGetTexImage(target, level, f, t, b);
            case FloatBuffer b -> glGetTexImage(target, level, f, t, b);
            case IntBuffer b   -> glGetTexImage(target, level, f, t, b);
            case ShortBuffer b -> glGetTexImage(target, level, f, t, b);
            default -> throw new IllegalArgumentException("Unsupported buffer: " + dst.getClass().getName());
        } Jgen.glCheckError();
    }


    public static long memoryUsed() {
        return MEMORY_USED;
    }

    public static long memoryPeak() {
        return MEMORY_PEAK;
    }

    public static boolean isSupportedTarget(int target) {
        for (int supported : SUPPORTED_TEXTURE_TARGETS) {
            if (target == supported) return true;
        } return false;
    }

    private static int calcMipLevels(int width, int height, int depth) {
        int maxDim = Math.max(width, Math.max(height, depth));
        return JgenMath.log2iFloor(maxDim) + 1;
    }

    private static long sizeOf1D(TextureFormat format, int width, boolean mipmap) {
        if (width <= 0) return 0;
        long total = 0;
        int w = width;
        while (true) {
            total += (long) w * format.bytesPerPixel;
            if (!mipmap || w == 1) break;
            w = Math.max(1, w / 2);
        } return total;
    }

    private static long sizeOf2D(TextureFormat format, int width, int height, boolean mipmap) {
        if (width <= 0 || height <= 0) return 0;
        long total = 0;
        int w = width;
        int h = height;
        while (true) {
            total += (long) w * h * format.bytesPerPixel;
            if (!mipmap || (w == 1 && h == 1)) break;
            w = Math.max(1, w / 2);
            h = Math.max(1, h / 2);
        } return total;
    }

    private static long sizeOf3D(TextureFormat format, int width, int height, int depth, boolean mipmap) {
        if (width <= 0 || height <= 0 || depth <= 0) return 0;
        long total = 0;
        int w = width;
        int h = height;
        int d = depth;
        while (true) {
            total += (long) w * h * d * format.bytesPerPixel;
            if (!mipmap || (w == 1 && h == 1 && d == 1)) break;
            w = Math.max(1, w / 2);
            h = Math.max(1, h / 2);
            d = Math.max(1, d / 2);
        } return total;
    }

}
