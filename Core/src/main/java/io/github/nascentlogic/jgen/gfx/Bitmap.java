package io.github.nascentlogic.jgen.gfx;

import io.github.nascentlogic.jgen.utils.Disposable;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.tinylog.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * F.Dahl, 6/19/2026
 */
public class Bitmap implements Disposable {


    private ByteBuffer pixels;
    private final int width;
    private final int height;
    private final int channels;
    private boolean stbAllocated;

    public Bitmap(int width, int height, int channels) {
        assertValidDimensions(width, height, channels);
        this.pixels = MemoryUtil.memCalloc(width * height * channels);
        this.width = width;
        this.height = height;
        this.channels = channels;
        this.stbAllocated = false;
    }

    /**
     * @param pixels pixel data != null && remaining == w * h * channels
     * @param width w > 0
     * @param height h > 0
     * @param channels channels > 0 && channels <= 4;
     */
    public Bitmap(ByteBuffer pixels, int width, int height, int channels) {
        Objects.requireNonNull(pixels, "pixels argument cannot be null.");
        assertValidDimensions(width, height, channels);
        int expectedSize = width * height * channels;
        if (pixels.remaining() != expectedSize) {
            Logger.error("Bitmap size mismatch: expected: {}, buffer remaining: {}", expectedSize, pixels.remaining());
            throw new IllegalArgumentException("Provided ByteBuffer remaining does not match specified dimensions.");
        }
        this.width = width;
        this.height = height;
        this.channels = channels;
        this.stbAllocated = false;
        if (pixels.isDirect()) {
            this.pixels = pixels;
        } else {
            this.pixels = MemoryUtil.memAlloc(expectedSize);
            Buffers.blit(pixels, this.pixels);
        }
    }

    public Bitmap(ByteBuffer png) throws IOException {
        Objects.requireNonNull(png, "png argument cannot be null.");
        ByteBuffer directPng = png;
        if (!png.isDirect()) {
            directPng = MemoryUtil.memAlloc(png.remaining());
            Buffers.blit(png, directPng);
        }
        try (MemoryStack stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);
            if (!stbi_info_from_memory(directPng, w, h, c)) {
                throw new IOException(stbi_failure_reason());
            } // stbi_set_flip_vertically_on_load(true);
            this.pixels = stbi_load_from_memory(directPng, w, h, c, 0);
            if (pixels == null) throw new IOException(stbi_failure_reason());
            this.width = w.get(0);
            this.height = h.get(0);
            this.channels = c.get(0);
            this.stbAllocated = true;
        } finally {
            if (!png.isDirect()) {
                MemoryUtil.memFree(directPng);
            }
        }
    }

    public ByteBuffer pixels() { return pixels; }
    public int channels() { return channels; }
    public int stride() { return width * channels; }
    public int sizeOf() { return width * height * channels; }
    public int width() { return width; }
    public int height() { return height; }

    public void setPixel(Color color, int x, int y) {
        setPixel(color.intBits(), x, y);
    }

    public void setPixel(int value, int x, int y) {
        int p = (y * width + x) * channels;
        for (int c = 0; c < channels; c++)
            pixels.put(p + c,(byte) ((value >> (c * 8)) & 0xFF));
    }

    public Color getPixel(int x, int y, Color dst) {
        int bits = getPixel(x, y);
        if (channels == 3) bits |= (0xFF << 24);
        return dst.setIntBits(bits);
    }

    public int getPixel(int x, int y) {
        int p = (y * width + x) * channels;
        int pixel = 0;
        for (int c = 0; c < channels; c++) {
            pixel |= (pixels.get(p + c) & 0xFF) << (c * 8);
        } return pixel;
    }

    public void blitRegion(Bitmap bitmap, int x, int y) {
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) return;
        int dstMinX = Math.clamp(x, 0, width);
        int dstMinY = Math.clamp(y, 0, height);
        int dstMaxX = Math.clamp(x + bitmap.width, dstMinX, width);
        int dstMaxY = Math.clamp(y + bitmap.height, dstMinY, height);
        if (dstMinX >= dstMaxX || dstMinY >= dstMaxY) return;
        if (bitmap.channels == 4) { // ~10x faster
            int rowWidthBytes = (dstMaxX - dstMinX) * channels;
            int oldPosition = pixels.position();
            for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
                int srcY = dstY - y;
                int srcX = dstMinX - x;
                int dstBytePos = (dstY * this.width + dstMinX) * channels;
                int srcBytePos = (srcY * bitmap.width + srcX) * channels;
                pixels.put(dstBytePos, bitmap.pixels, srcBytePos, rowWidthBytes);
            }
        } else for (int dstY = dstMinY; dstY < dstMaxY; dstY++) {
            int srcY = dstY - y;
            for (int dstX = dstMinX; dstX < dstMaxX; dstX++) {
                int srcX = dstX - x;
                int p = bitmap.getPixel(srcX, srcY);
                setPixel(p, dstX, dstY);
            }
        }
    }

    public void vFlip() {
        ByteBuffer src = pixels;
        ByteBuffer dst = MemoryUtil.memAlloc(src.remaining());
        long srcAddr = MemoryUtil.memAddress(src);
        long dstAddr = MemoryUtil.memAddress(dst);
        long stride = stride();
        for (int row = 0; row < height; row++) {
            long dstPointer = dstAddr + stride * row;
            long srcPointer = srcAddr + stride * (height - row - 1);
            MemoryUtil.memCopy(srcPointer, dstPointer, stride);
        } if (stbAllocated) {
            stbi_image_free(pixels);
            stbAllocated = false;
        } else MemoryUtil.memFree(pixels);
        pixels = dst;
    }

    public Bitmap copy() {
        ByteBuffer pixels = MemoryUtil.memAlloc(sizeOf());
        Buffers.blit(this.pixels, pixels);
        return new Bitmap(pixels, width, height, channels);
    }

    /** Compress the pixels to .png format. Heap allocated buffer. */
    public ByteBuffer compress() throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final byte[][] array = new byte[][] { new byte[4096] };
            try (STBIWriteCallback callback = new STBIWriteCallback() {
                public void invoke(long context, long data, int size) {
                    ByteBuffer chunk = STBIWriteCallback.getData(data, size);
                    if (size > array[0].length) array[0] = new byte[size];
                    chunk.get(array[0], 0, size);
                    out.write(array[0], 0, size); }
            }) {
                // stbi_flip_vertically_on_write(true);
                boolean success = STBImageWrite.stbi_write_png_to_func(callback,
                    0L, width, height, channels, pixels, stride());
                if (!success) throw new IOException("Failed to encode bitmap via. stbi_write_png_to_func.");
            } return ByteBuffer.wrap(out.toByteArray());
        }
    }

    /** @see #toTexture(boolean, boolean) */
    public Texture toTexture() { return toTexture(false); }
    /** @see #toTexture(boolean, boolean) */
    public Texture toTexture(boolean mipmap) { return toTexture(mipmap,false); }
    /**
     * @param mipmap allocate mipmap (does not generate)
     * @param srgb use SRGB / SRGBA texture format (if the bitmap channels >= 3)
     * @return Texture with Bitmap pixels uploaded to level 0
     */
    public Texture toTexture(boolean mipmap, boolean srgb) {
        Texture texture = Texture.generate2D(width,height);
        TextureFormat format = switch(channels) {
            case 1 -> TextureFormat.R8;
            case 2 -> TextureFormat.RG8;
            case 3 -> srgb ? TextureFormat.SRGB8 : TextureFormat.RGB8;
            case 4 -> srgb ? TextureFormat.SRGBA8 : TextureFormat.RGBA8;
            default -> throw new IllegalStateException("Channels > 4");
        }; texture.allocate(format,mipmap);
        texture.upload(pixels);
        texture.clampToBorder();
        texture.filterLinear();
        return texture;
    }

    public void free() {
        if (stbAllocated) {
            stbi_image_free(pixels);
        } else MemoryUtil.memFree(pixels);
    }

    private static void assertValidDimensions(int width, int height, int channels) {
        if (width <= 0 || height <= 0 || channels <= 0 || channels > 4) {
            Logger.error("Invalid bitmap args: w({}),h({}),c({})",width,height,channels);
            throw new IllegalArgumentException("Invalid bitmap args");
        }
    }


}
