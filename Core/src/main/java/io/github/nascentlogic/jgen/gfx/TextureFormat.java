package io.github.nascentlogic.jgen.gfx;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT16;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT32;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

/**
 * Metadata representation of OpenGL texture formats using standard graphics API short naming conventions.
 * Consolidates internal sized specifications, data pixel formats, memory sizes,
 * and layout traits required for robust GPU allocations and pixel stream operations.
 * <p>F.Dahl, 6/30/2026</p>
 */
public enum TextureFormat {

    /** Fallback or uninitialized format token. */
    INVALID(0, 0, 0, 0, 0, false),

    // --- Depth / Stencil Formats ---
    STENCIL8(GL_STENCIL_INDEX8, GL_STENCIL_INDEX, GL_UNSIGNED_BYTE, 1, 1, false),
    DEPTH16(GL_DEPTH_COMPONENT16, GL_DEPTH_COMPONENT, GL_UNSIGNED_SHORT, 1, 2, false),
    DEPTH32(GL_DEPTH_COMPONENT32, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, 1, 4, false),
    DEPTH32F(GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT, GL_FLOAT, 1, 4, false),
    DEPTH24_STENCIL8(GL_DEPTH24_STENCIL8, GL_DEPTH_STENCIL, GL_UNSIGNED_INT_24_8, 2, 4, false),

    // --- 16-Bit Floating Point Formats (Half-Floats) ---
    R16F(GL_R16F, GL_R, GL_HALF_FLOAT, 1, 2, true),
    RG16F(GL_RG16F, GL_RG, GL_HALF_FLOAT, 2, 4, true),
    RGB16F(GL_RGB16F, GL_RGB, GL_HALF_FLOAT, 3, 6, true),
    RGBA16F(GL_RGBA16F, GL_RGBA, GL_HALF_FLOAT, 4, 8, true),

    // --- 32-Bit Floating Point Formats ---
    R32F(GL_R32F, GL_R, GL_FLOAT, 1, 4, true),
    RG32F(GL_RG32F, GL_RG, GL_FLOAT, 2, 8, true),
    RGB32F(GL_RGB32F, GL_RGB, GL_FLOAT, 3, 12, true),
    RGBA32F(GL_RGBA32F, GL_RGBA, GL_FLOAT, 4, 16, true),

    // --- Special Packed Floating Point Formats ---
    // Packs RGB into 32-bits total (11 bits Red, 11 bits Green, 10 bits Blue).
    // Unsigned, no alpha channel. Exceptional choice for high-perf HDR color targets.
    R11F_G11F_B10F(GL_R11F_G11F_B10F, GL_RGB, GL_UNSIGNED_INT_10F_11F_11F_REV, 3, 4, true),

    // --- 8-Bit Channels (Red) ---
    R8_SNORM(GL_R8_SNORM, GL_RED, GL_BYTE, 1, 1, true),
    R8(GL_R8, GL_RED, GL_UNSIGNED_BYTE, 1, 1, true),
    R8UI(GL_R8UI, GL_RED_INTEGER, GL_UNSIGNED_BYTE, 1, 1, true),

    // --- 16-Bit / 32-Bit Channels (Red) ---
    R16_SNORM(GL_R16_SNORM, GL_RED, GL_SHORT, 1, 2, true),
    R16(GL_R16, GL_RED, GL_UNSIGNED_SHORT, 1, 2, true),
    R32I(GL_R32I, GL_RED_INTEGER, GL_INT, 1, 4, true),
    R32UI(GL_R32UI, GL_RED_INTEGER, GL_UNSIGNED_INT, 1, 4, true),

    // --- 8-Bit & 16-Bit Channels (RG) ---
    RG8_SNORM(GL_RG8_SNORM, GL_RG, GL_BYTE, 2, 2, true),
    RG8(GL_RG8, GL_RG, GL_UNSIGNED_BYTE, 2, 2, true),
    RG16_SNORM(GL_RG16_SNORM, GL_RG, GL_SHORT, 2, 4, true),
    RG16(GL_RG16, GL_RG, GL_UNSIGNED_SHORT, 2, 4, true),

    // --- 8-Bit & 16-Bit Channels (RGB) ---
    RGB8UI(GL_RGB8UI, GL_RGB_INTEGER, GL_UNSIGNED_BYTE, 3, 3, true),
    RGB8_SNORM(GL_RGB8_SNORM, GL_RGB, GL_BYTE, 3, 3, true),
    RGB8(GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE, 3, 3, true),
    SRGB8(GL_SRGB8, GL_RGB, GL_UNSIGNED_BYTE, 3, 3, true),
    RGB16_SNORM(GL_RGB16_SNORM, GL_RGB, GL_SHORT, 3, 6, true),
    RGB16(GL_RGB16, GL_RGB, GL_UNSIGNED_SHORT, 3, 6, true),

    // --- Packed & 8-Bit Channels (RGBA) ---
    RGBA4(GL_RGBA4, GL_RGBA, GL_UNSIGNED_SHORT_4_4_4_4, 4, 2, true),
    RGBA8UI(GL_RGBA8UI, GL_RGBA_INTEGER, GL_UNSIGNED_BYTE, 4, 4, true),
    RGBA8_SNORM(GL_RGBA8_SNORM, GL_RGBA, GL_BYTE, 4, 4, true),
    RGBA8(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, 4, 4, true),
    SRGBA8(GL_SRGB8_ALPHA8, GL_RGBA, GL_UNSIGNED_BYTE, 4, 4, true);

    /** The explicit sized allocation format token on the GPU (e.g., GL_RGBA8, GL_R32F). */
    public final int sizedFormat;
    /** The base unsized component structural format layout token (e.g., GL_RGBA, GL_RED, GL_DEPTH_STENCIL). */
    public final int format;
    /** The primitive data type token describing each pixel data chunk in CPU memory (e.g., GL_UNSIGNED_BYTE, GL_FLOAT). */
    public final int dataType;
    /** The number of active logical color or depth channels present inside a single pixel element. */
    public final int channels;
    /** The exact, definitive memory space required by a single pixel element in bytes. */
    public final int bytesPerPixel;
    /** Indicates if the format represents color data (true) or specialized structural context like depth/stencil (false). */
    public final boolean isColorFormat;

    TextureFormat(int sizedFormat, int format, int dataType, int channels,
                  int bytesPerPixel, boolean isColorFormat) {
        this.sizedFormat = sizedFormat;
        this.format = format;
        this.dataType = dataType;
        this.channels = channels;
        this.bytesPerPixel = bytesPerPixel;
        this.isColorFormat = isColorFormat;
    }

    /**
     * Calculates the mathematically correct pixel stream transfer alignment (1, 2, or 4).
     * This value must be supplied directly to {@code glPixelStorei} using the targets
     * {@code GL_UNPACK_ALIGNMENT} when uploading data or {@code GL_PACK_ALIGNMENT} when downloading data.
     * * @return The largest valid power-of-two byte division alignment parameter (1, 2, or 4).
     */
    public int alignment() {
        if (this.bytesPerPixel % 4 == 0) return 4;
        if (this.bytesPerPixel % 2 == 0) return 2;
        return 1;
    }
}
