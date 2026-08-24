package io.github.nascentlogic.jgen.gfx.font;

import io.github.nascentlogic.jgen.gfx.Bitmap;
import io.github.nascentlogic.jgen.gfx.Buffers;
import io.github.nascentlogic.jgen.utils.AtlasPacker;
import io.github.nascentlogic.jgen.utils.Disposable;
import io.github.nascentlogic.jgen.utils.TextureRegion;
import org.joml.Vector2i;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.msdfgen.MSDFGenBitmap;
import org.lwjgl.util.msdfgen.MSDFGenBounds;
import org.lwjgl.util.msdfgen.MSDFGenTransform;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.stb.STBTruetype.*;
import static org.lwjgl.util.msdfgen.MSDFGen.*;
import static org.lwjgl.util.msdfgen.MSDFGenExt.*;

/**
 * F.Dahl, 8/21/2026
 */
public class Font implements Disposable {

    public static final int FONT_SIZE    = 48;
    public static final int FONT_PADDING = 4;
    public static final int NUM_GLYPHS   = 95;   // ' ' (32) .. '~' (126)
    public static final int FIRST_CHAR   = 32;
    public static final int LAST_CHAR    = 126;

    public final String name;
    public final float size;
    public final float padding;
    public final float ascent;
    public final float descent;
    public final float lineGap;
    public final float maxAdvance;
    public final float avgAdvance;
    public final boolean monospaced;
    public final Glyph cursor;
    private final Glyph[] glyphs;
    private final float[] kerning;
    private transient Bitmap bitmap;

    /* GSON */
    private Font() {
        name = "";
        size = 0f;
        padding = 0f;
        ascent = 0f;
        descent = 0f;
        lineGap = 0f;
        maxAdvance = 0f;
        avgAdvance = 0f;
        monospaced = false;
        cursor = null;
        glyphs = null;
        kerning = null;
    }

    private Font(String name,
                 float size,
                 float padding,
                 float ascent,
                 float descent,
                 float lineGap,
                 float maxAdvance,
                 float avgAdvance,
                 boolean monospaced,
                 Glyph cursor,
                 Glyph[] glyphs,
                 float[] kerning,
                 Bitmap bitmap) {
        this.name = name;
        this.size = size;
        this.padding = padding;
        this.ascent = ascent;
        this.descent = descent;
        this.lineGap = lineGap;
        this.maxAdvance = maxAdvance;
        this.avgAdvance = avgAdvance;
        this.monospaced = monospaced;
        this.cursor = cursor;
        this.glyphs = glyphs;
        this.kerning = kerning;
        this.bitmap = bitmap;
    }

    /**
     * Unchecked.
     * @param c ascii value 32 -> 126 (inclusive)
     * @return glyph of character
     */
    public Glyph glyph(byte c) {
        return glyphs[c - FIRST_CHAR];
    }

    /**
     * Unchecked.
     * Additional advance to apply between the two characters.
     * @param left left character. 32 -> 126 (inclusive)
     * @param right right character. 32 -> 126 (inclusive)
     * @return kerning of character pair
     */
    public float kerning(byte left, byte right) {
        final int li = left  - FIRST_CHAR;
        final int ri = right - FIRST_CHAR;
        return kerning[li * NUM_GLYPHS + ri];
    }

    public float lineHeight() {
        return ascent + descent + lineGap;
    }

    public Bitmap bitmap() {
        return bitmap;
    }

    /** Internal use only (Uses when loading Font as GSON)) */
    public void setBitmap(Bitmap bitmap) {
        if (this.bitmap != null)
            throw new IllegalStateException("setAtlas() is internal use only");
        this.bitmap = bitmap;
    }

    @Override
    public void free() {
        Disposable.free(bitmap);
    }

    public static Font generate(String name, ByteBuffer ttf) throws Exception {

        Objects.requireNonNull(ttf, "true type font buffer cannot be null");
        if (name == null || name.isBlank()) name = "unnamedFont";
        if (!ttf.isDirect()) {
            ByteBuffer ttfDirect = ByteBuffer.allocateDirect(ttf.remaining());
            Buffers.blit(ttf,ttfDirect);
            ttf = ttfDirect;
        }

        try (STBTTFontinfo stbInfo = STBTTFontinfo.create()) {

            if (!stbtt_InitFont(stbInfo, ttf)) throw new Exception("STB failed to initialise font");
            float stbScale = stbtt_ScaleForPixelHeight(stbInfo, FONT_SIZE);

            float ascent;
            float descent;
            float lineGap;

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer ascentBuf  = stack.mallocInt(1);
                IntBuffer descentBuf = stack.mallocInt(1);
                IntBuffer lineGapBuf = stack.mallocInt(1);
                stbtt_GetFontVMetrics(stbInfo, ascentBuf, descentBuf, lineGapBuf);
                ascent  =  ascentBuf.get(0) * stbScale;
                descent = -descentBuf.get(0) * stbScale; // turn positive
                lineGap =  lineGapBuf.get(0) * stbScale;
            }

            long freeTypeHandle = 0L;
            long fontHandle = 0L;

            try {

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    PointerBuffer ftPtr = stack.mallocPointer(1);
                    if (msdf_ft_init(ftPtr) != MSDF_SUCCESS) { throw new Exception("msdf_ft_init failed"); }
                    freeTypeHandle = ftPtr.get(0);
                    PointerBuffer fontPtr = stack.mallocPointer(1);
                    if (msdf_ft_load_font_data(freeTypeHandle, ttf, fontPtr) != MSDF_SUCCESS) {
                        throw new Exception("msdf_ft_load_font_data failed – invalid or unsupported TTF");
                    } fontHandle = fontPtr.get(0);
                }

                // =============================================================================
                // MSDF GLYPH IMAGE GENERATION
                // =============================================================================

                GlyphImage[] glyphImages = new GlyphImage[NUM_GLYPHS];
                boolean monospaced = true;
                float totalAdvance = 0f;
                float maxAdvance = 0f;
                int initialAdvance = -1; // unscaled STB units (FONT_SIZE independent)
                for (int codePoint = FIRST_CHAR; codePoint <= LAST_CHAR; codePoint++) {
                    GlyphImage image = generateGlyph(codePoint, fontHandle, stbInfo, stbScale);
                    if (image == null) throw new Exception("Font is missing required ASCII glyph for codepoint: " + codePoint);
                    glyphImages[codePoint - FIRST_CHAR] = image;
                    totalAdvance += image.advance;
                    maxAdvance = Math.max(maxAdvance, image.advance);
                    if (monospaced) {
                        // Monospaced test in design units (not pixels) so it does not depend on FONT_SIZE
                        try (MemoryStack stack = MemoryStack.stackPush()) {
                            IntBuffer adv = stack.mallocInt(1);
                            IntBuffer lsb = stack.mallocInt(1);
                            stbtt_GetCodepointHMetrics(stbInfo, codePoint, adv, lsb);
                            int units = adv.get(0);
                            if (initialAdvance < 0) initialAdvance = units;
                            else if (units != initialAdvance) monospaced = false;
                        }
                    }
                }
                float avgAdvance = totalAdvance / NUM_GLYPHS;

                // =============================================================================
                // ATLAS PACKING
                // =============================================================================

                List<AtlasPacker.Rectangle> toPack = new ArrayList<>(NUM_GLYPHS);
                for (int i = 0; i < NUM_GLYPHS; i++) {
                    GlyphImage image = glyphImages[i];
                    toPack.add(new AtlasPacker.Rectangle(i,image.bitmap.width(),image.bitmap.height()));
                } Vector2i atlasSize = new Vector2i();
                List<AtlasPacker.Region> packedResult = AtlasPacker.pack(toPack, atlasSize);
                if (packedResult.size() != NUM_GLYPHS) throw new Exception("Atlas packer failed to pack all glyph regions");

                Bitmap atlas = new Bitmap(atlasSize.x,atlasSize.y,3);
                Glyph[] glyphs = new Glyph[NUM_GLYPHS];
                Vector4f uvCoords = new Vector4f();
                for (AtlasPacker.Region packed : packedResult) {
                    int index = packed.id();
                    GlyphImage image = glyphImages[index];
                    TextureRegion region = packed.r();
                    region.uvCoords(atlasSize.x,atlasSize.y,uvCoords);
                    glyphs[index] = new Glyph(
                            (char) image.codepoint,
                            image.advance,
                            image.offsetX,
                            image.offsetY,
                            region.x,
                            region.y,
                            region.w,
                            region.h,
                            uvCoords.x,
                            uvCoords.y,
                            uvCoords.z,
                            uvCoords.w);
                    atlas.blitRegion(image.bitmap,region.x,region.y);
                    image.bitmap.free();
                }

                // =============================================================================
                // KERNING
                // =============================================================================

                float[] kerningTable = new float[NUM_GLYPHS * NUM_GLYPHS];
                for (int i = 0; i < NUM_GLYPHS; i++) {
                    int left = FIRST_CHAR + i;
                    for (int j = 0; j < NUM_GLYPHS; j++) {
                        int right = FIRST_CHAR + j;
                        int kern = stbtt_GetCodepointKernAdvance(stbInfo, left, right);
                        kerningTable[i * NUM_GLYPHS + j] = kern * stbScale;
                    }
                }

                Glyph cursor = createBlockCursor(avgAdvance,ascent,descent);

                return new Font(
                        name,
                        FONT_SIZE,
                        FONT_PADDING,
                        ascent,
                        descent,
                        lineGap,
                        maxAdvance,
                        avgAdvance,
                        monospaced,
                        cursor,
                        glyphs,
                        kerningTable,
                        atlas
                );

            } finally {
                if (fontHandle != 0L) msdf_ft_font_destroy(fontHandle);
                if (freeTypeHandle != 0L) msdf_ft_deinit(freeTypeHandle);
            }
        }
    }

    /**
     * Generate a single padded 3-channel MSDF glyph image + metrics.
     * <p>
     * Coordinate model (Y-up, pen at origin on the baseline):
     * <ul>
     *   <li>msdfgen shape origin = pen</li>
     *   <li>bounds are ink box in <b>em</b></li>
     *   <li>we expand by FONT_PADDING (converted to em) → image box in em</li>
     *   <li>quantize to integer pixel edges → image size and placement</li>
     *   <li>xOffset/yOffset = pen → bottom-left of that image (pixels at FONT_SIZE)</li>
     * </ul>
     * Advance comes from STB (pixels at FONT_SIZE). Placement offsets come from the
     * same MSDF frame that produced the pixels, so they cannot drift apart.
     * <p>
     * Space (codepoint 32) gets a black bitmap of size {@code 1 + 2*FONT_PADDING}
     * and zero offsets; it is still stored so the glyph table stays dense.
     *
     * @return GlyphImage or null if the codepoint cannot be loaded
     */
    private static GlyphImage generateGlyph(int codePoint, long fontHandle,
                                            STBTTFontinfo stbInfo, float stbScale) {

        // ----- Advance from STB (design units → pixels at FONT_SIZE) -----
        float advance;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer adv = stack.mallocInt(1);
            IntBuffer lsb = stack.mallocInt(1); // unused; placement comes from msdf frame
            stbtt_GetCodepointHMetrics(stbInfo, codePoint, adv, lsb);
            advance = adv.get(0) * stbScale;
        }

        // Space has no outline — skip MSDF and emit a small black placeholder.
        if (codePoint == ' ') {
            int size = 1 + FONT_PADDING * 2;
            ByteBuffer black = MemoryUtil.memCalloc(size * size * 3);
            Bitmap bitmap = new Bitmap(black, size, size, 3);
            return new GlyphImage(codePoint, 0f, 0f, advance, bitmap);
        }

        // ----- Shape + MSDF from msdfgen -----
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer shapePtr = stack.mallocPointer(1);
            int result = msdf_ft_font_load_glyph(
                    fontHandle,
                    codePoint,
                    MSDF_FONT_SCALING_EM_NORMALIZED,
                    shapePtr);
            if (result != MSDF_SUCCESS || shapePtr.get(0) == 0L) return null;
            long shape = shapePtr.get(0);

            try {

                if (msdf_shape_normalize(shape) != MSDF_SUCCESS) return null;
                msdf_shape_edge_colors_simple(shape, 3.0);
                // Ink bounds in em (Y-up, origin = pen)
                MSDFGenBounds bounds = MSDFGenBounds.malloc(stack);
                if (msdf_shape_get_bounds(shape, bounds) != MSDF_SUCCESS) {
                    return null;
                }

                double xMin = bounds.l();
                double yMin = bounds.b();
                double xMax = bounds.r();
                double yMax = bounds.t();
                // Expand to image box: ink + distance-field padding on every side.
                double padEm = (double) FONT_PADDING / (double) FONT_SIZE;
                double imageLEm = xMin - padEm;
                double imageBEm = yMin - padEm;
                double imageREm = xMax + padEm;
                double imageTEm = yMax + padEm;
                // Quantize to integer pixel edges (generation pixel space).
                int imageX = (int) Math.floor(imageLEm * FONT_SIZE);
                int imageY = (int) Math.floor(imageBEm * FONT_SIZE);
                int imageR = (int) Math.ceil(imageREm * FONT_SIZE);
                int imageT = (int) Math.ceil(imageTEm * FONT_SIZE);
                int imageW = Math.max(1, imageR - imageX);
                int imageH = Math.max(1, imageT - imageY);
                float xOffset = (float) imageX;
                float yOffset = (float) imageY;

                MSDFGenBitmap msdfBitmap = MSDFGenBitmap.malloc(stack);
                if (msdf_bitmap_alloc(MSDF_BITMAP_TYPE_MSDF, imageW, imageH, msdfBitmap) != MSDF_SUCCESS) return null;

                try {
                    // Map image box onto the bitmap: scale em→pixels, translate so
                    // (imageX, imageY) lands on bitmap pixel (0, 0).
                    double transX = -imageX / (double) FONT_SIZE;
                    double transY = -imageY / (double) FONT_SIZE;

                    MSDFGenTransform transform = MSDFGenTransform.malloc(stack);
                    transform.scale().x(FONT_SIZE).y(FONT_SIZE);
                    transform.translation().x(transX).y(transY);
                    transform.distance_mapping().lower(-padEm).upper(padEm);

                    if (msdf_generate_msdf(msdfBitmap, shape, transform) != MSDF_SUCCESS) return null;
                    PointerBuffer pixelsPtr = stack.mallocPointer(1);
                    if (msdf_bitmap_get_pixels(msdfBitmap, pixelsPtr) != MSDF_SUCCESS) return null;

                    // View only — memory owned by msdfBitmap; do not free.
                    FloatBuffer src = MemoryUtil.memFloatBuffer(pixelsPtr.get(0), imageW * imageH * 3);
                    // float MSDF → unsigned byte. msdfgen row0 = bottom (Y-up);
                    // our Bitmap row0 = top → copy rows reversed.
                    ByteBuffer bytePixels = MemoryUtil.memAlloc(imageW * imageH * 3);
                    int rowFloats = imageW * 3;
                    for (int row = 0; row < imageH; row++) {
                        int srcRow = (imageH - 1 - row) * rowFloats;
                        for (int i = 0; i < rowFloats; i++) {
                            float v = src.get(srcRow + i);
                            int bv = Math.clamp(Math.round(v * 255f), 0, 255);
                            bytePixels.put((byte) bv);
                        }
                    }
                    Bitmap bitmap = new Bitmap(bytePixels.flip(), imageW, imageH, 3);
                    return new GlyphImage(codePoint, xOffset, yOffset, advance, bitmap);
                } finally {
                    msdf_bitmap_free(msdfBitmap);
                }
            } finally {
                msdf_shape_free(shape);
            }
        }
    }

    /**
     * Synthetic block cursor (not drawn from the atlas). <p>
     * Sits on the baseline covering one cell:
     * width ≈ {@code avgAdvance}, height = ascent + descent.
     * Bottom-left relative to pen is {@code (0, -descent)} (Y-up).
     * Atlas/UV fields are dummies — the renderer should ignore sampling.
     */
    private static Glyph createBlockCursor(float avgAdvance, float ascent, float descent) {
        int w = Math.max(1, Math.round(avgAdvance));
        int h = Math.max(1, Math.round(ascent + descent));
        float xOff = 0f;
        float yOff = -descent;
        // Dummy atlas rect + UVs (not sampled for cursor)
        return new Glyph(
                '|', // marker only; not used for lookup
                avgAdvance, xOff, yOff,
                0, 0, w, h,
                0f, 0f, 0f, 0f
        );
    }

    /**
     * Temporary holder for a single generated MSDF glyph.
     * Exists only during the loading process.
     * {@code bitmap} is a 3-channel byte Bitmap that owns its pixel memory. */
    private static final class GlyphImage {
        final int codepoint;    // character codepoint
        final float offsetX;    // pen → bottom-left of padded sprite (pixels)
        final float offsetY;    // pen → bottom-left of padded sprite (pixels)
        final float advance;    // character advance
        final Bitmap bitmap;    // padded MSDF image (3 channels)
        GlyphImage(int codepoint, float offsetX, float offsetY,
                   float advance, Bitmap bitmap) {
            this.codepoint = codepoint;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.advance = advance;
            this.bitmap = bitmap;
        }
    }

}
