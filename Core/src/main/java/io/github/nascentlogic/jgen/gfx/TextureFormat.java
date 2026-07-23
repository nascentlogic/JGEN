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
    // =========================================================================
    // 1. DEPTH & STENCIL FORMATS
    // =========================================================================
    STENCIL8(GL_STENCIL_INDEX8, GL_STENCIL_INDEX, GL_UNSIGNED_BYTE, 1, 1, false),
    DEPTH16(GL_DEPTH_COMPONENT16, GL_DEPTH_COMPONENT, GL_UNSIGNED_SHORT, 1, 2, false),
    DEPTH32(GL_DEPTH_COMPONENT32, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, 1, 4, false),
    DEPTH32F(GL_DEPTH_COMPONENT32F, GL_DEPTH_COMPONENT, GL_FLOAT, 1, 4, false),
    DEPTH24_STENCIL8(GL_DEPTH24_STENCIL8, GL_DEPTH_STENCIL, GL_UNSIGNED_INT_24_8, 1, 4, false),
    // =========================================================================
    // 2. SPECIAL / PACKED & COLOR-SPACE FORMATS
    // =========================================================================
    SRGB8(GL_SRGB8, GL_RGB, GL_UNSIGNED_BYTE, 3, 3, true),
    SRGBA8(GL_SRGB8_ALPHA8, GL_RGBA, GL_UNSIGNED_BYTE, 4, 4, true),
    RGBA4(GL_RGBA4, GL_RGBA, GL_UNSIGNED_SHORT_4_4_4_4, 4, 2, true),
    R11F_G11F_B10F(GL_R11F_G11F_B10F, GL_RGB, GL_UNSIGNED_INT_10F_11F_11F_REV, 3, 4, true),
    // =========================================================================
    // 3. NORMALIZED COLOR FORMATS (UNORM / SNORM)
    // =========================================================================
    // --- 8-Bit UNORM / SNORM ---
    R8(GL_R8, GL_RED, GL_UNSIGNED_BYTE, 1, 1, true),
    R8_SNORM(GL_R8_SNORM, GL_RED, GL_BYTE, 1, 1, true),
    RG8(GL_RG8, GL_RG, GL_UNSIGNED_BYTE, 2, 2, true),
    RG8_SNORM(GL_RG8_SNORM, GL_RG, GL_BYTE, 2, 2, true),
    RGB8(GL_RGB8, GL_RGB, GL_UNSIGNED_BYTE, 3, 3, true),
    RGB8_SNORM(GL_RGB8_SNORM, GL_RGB, GL_BYTE, 3, 3, true),
    RGBA8(GL_RGBA8, GL_RGBA, GL_UNSIGNED_BYTE, 4, 4, true),
    RGBA8_SNORM(GL_RGBA8_SNORM, GL_RGBA, GL_BYTE, 4, 4, true),
    // --- 16-Bit UNORM / SNORM ---
    R16(GL_R16, GL_RED, GL_UNSIGNED_SHORT, 1, 2, true),
    R16_SNORM(GL_R16_SNORM, GL_RED, GL_SHORT, 1, 2, true),
    RG16(GL_RG16, GL_RG, GL_UNSIGNED_SHORT, 2, 4, true),
    RG16_SNORM(GL_RG16_SNORM, GL_RG, GL_SHORT, 2, 4, true),
    RGB16(GL_RGB16, GL_RGB, GL_UNSIGNED_SHORT, 3, 6, true),
    RGB16_SNORM(GL_RGB16_SNORM, GL_RGB, GL_SHORT, 3, 6, true),
    // =========================================================================
    // 4. FLOATING-POINT FORMATS (16F / 32F)
    // =========================================================================
    // --- 16-Bit Half-Float ---
    R16F(GL_R16F, GL_RED, GL_HALF_FLOAT, 1, 2, true),
    RG16F(GL_RG16F, GL_RG, GL_HALF_FLOAT, 2, 4, true),
    RGB16F(GL_RGB16F, GL_RGB, GL_HALF_FLOAT, 3, 6, true),
    RGBA16F(GL_RGBA16F, GL_RGBA, GL_HALF_FLOAT, 4, 8, true),
    // --- 32-Bit Full-Float ---
    R32F(GL_R32F, GL_RED, GL_FLOAT, 1, 4, true),
    RG32F(GL_RG32F, GL_RG, GL_FLOAT, 2, 8, true),
    RGB32F(GL_RGB32F, GL_RGB, GL_FLOAT, 3, 12, true),
    RGBA32F(GL_RGBA32F, GL_RGBA, GL_FLOAT, 4, 16, true),
    // =========================================================================
    // 5. UNSIGNED INTEGER FORMATS (UI)
    // =========================================================================
    // --- Red (UI) ---
    R8UI(GL_R8UI, GL_RED_INTEGER, GL_UNSIGNED_BYTE, 1, 1, true),
    R16UI(GL_R16UI, GL_RED_INTEGER, GL_UNSIGNED_SHORT, 1, 2, true),
    R32UI(GL_R32UI, GL_RED_INTEGER, GL_UNSIGNED_INT, 1, 4, true),
    // --- RG (UI) ---
    RG8UI(GL_RG8UI, GL_RG_INTEGER, GL_UNSIGNED_BYTE, 2, 2, true),
    RG16UI(GL_RG16UI, GL_RG_INTEGER, GL_UNSIGNED_SHORT, 2, 4, true),
    RG32UI(GL_RG32UI, GL_RG_INTEGER, GL_UNSIGNED_INT, 2, 8, true),
    // --- RGB (UI) ---
    RGB8UI(GL_RGB8UI, GL_RGB_INTEGER, GL_UNSIGNED_BYTE, 3, 3, true),
    RGB16UI(GL_RGB16UI, GL_RGB_INTEGER, GL_UNSIGNED_SHORT, 3, 6, true),
    RGB32UI(GL_RGB32UI, GL_RGB_INTEGER, GL_UNSIGNED_INT, 3, 12, true),
    // --- RGBA (UI) ---
    RGBA8UI(GL_RGBA8UI, GL_RGBA_INTEGER, GL_UNSIGNED_BYTE, 4, 4, true),
    RGBA16UI(GL_RGBA16UI, GL_RGBA_INTEGER, GL_UNSIGNED_SHORT, 4, 8, true),
    RGBA32UI(GL_RGBA32UI, GL_RGBA_INTEGER, GL_UNSIGNED_INT, 4, 16, true),
    // =========================================================================
    // 6. SIGNED INTEGER FORMATS (I)
    // =========================================================================
    // --- Red (I) ---
    R8I(GL_R8I, GL_RED_INTEGER, GL_BYTE, 1, 1, true),
    R16I(GL_R16I, GL_RED_INTEGER, GL_SHORT, 1, 2, true),
    R32I(GL_R32I, GL_RED_INTEGER, GL_INT, 1, 4, true),
    // --- RG (I) ---
    RG8I(GL_RG8I, GL_RG_INTEGER, GL_BYTE, 2, 2, true),
    RG16I(GL_RG16I, GL_RG_INTEGER, GL_SHORT, 2, 4, true),
    RG32I(GL_RG32I, GL_RG_INTEGER, GL_INT, 2, 8, true),
    // --- RGB (I) ---
    RGB8I(GL_RGB8I, GL_RGB_INTEGER, GL_BYTE, 3, 3, true),
    RGB16I(GL_RGB16I, GL_RGB_INTEGER, GL_SHORT, 3, 6, true),
    RGB32I(GL_RGB32I, GL_RGB_INTEGER, GL_INT, 3, 12, true),
    // --- RGBA (I) ---
    RGBA8I(GL_RGBA8I, GL_RGBA_INTEGER, GL_BYTE, 4, 4, true),
    RGBA16I(GL_RGBA16I, GL_RGBA_INTEGER, GL_SHORT, 4, 8, true),
    RGBA32I(GL_RGBA32I, GL_RGBA_INTEGER, GL_INT, 4, 16, true);

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
     * @return The largest valid power-of-two byte division alignment parameter (1, 2, or 4).
     */
    public int alignment() {
        if (this.bytesPerPixel % 4 == 0) return 4;
        if (this.bytesPerPixel % 2 == 0) return 2;
        return 1;
    }
}