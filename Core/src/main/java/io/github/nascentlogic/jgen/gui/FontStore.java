package io.github.nascentlogic.jgen.gui;

import io.github.nascentlogic.jgen.gfx.Texture;
import io.github.nascentlogic.jgen.gfx.UniformBuffer;
import io.github.nascentlogic.jgen.gui.util.Glyph;
import io.github.nascentlogic.jgen.io.Disk;
import io.github.nascentlogic.jgen.utils.Disposable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * F.Dahl, 9/1/2026
 */
public class FontStore implements Disposable {

    public static final String DEFAULT_FONT_NAME = "JetBrainsMono-Regular";
    public static final String DEFAULT_FONT_PATH = "jgen/gui/font/" + DEFAULT_FONT_NAME + ".ttf";
    public static final int MAX_FONT_SLOTS = 5;

    private static final int GLYPH_SIZE_FLOAT = 8;
    private static final int FONT_SIZE_FLOAT = (Font.NUM_GLYPHS + 1) * GLYPH_SIZE_FLOAT + 4;
    private static final int FONT_SIZE_BYTES = FONT_SIZE_FLOAT * Float.BYTES;
    private static final int INDEX_MAP_OFFSET_BYTES = MAX_FONT_SLOTS * FONT_SIZE_BYTES;
    private static final int INDEX_MAP_SIZE_BYTES = (MAX_FONT_SLOTS * 4) * Float.BYTES;
    private static final int UBO_SIZE_BYTES = FONT_SIZE_BYTES * MAX_FONT_SLOTS + INDEX_MAP_SIZE_BYTES;

    // GPU Storage
    private final UniformBuffer ubo;
    private final FloatBuffer uploadBuffer;
    private final Texture[] textures = new Texture[MAX_FONT_SLOTS];
    private int numUploadedFonts;

    // CPU Storage
    private final Map<String,Font> fontLibrary = new HashMap<>();
    private final Font[] uboFontSlots = new Font[MAX_FONT_SLOTS];
    private final int[] logicalToUboSlotMap = new int[MAX_FONT_SLOTS]; // indices point to default font (0)


    FontStore() throws Exception {
        Font defaultFont = Disk.resourceFont(DEFAULT_FONT_PATH);
        fontLibrary.put(defaultFont.name,defaultFont);
        ubo = new UniformBuffer(UBO_SIZE_BYTES,false);
        uploadBuffer = MemoryUtil.memAllocFloat(FONT_SIZE_FLOAT);
        bindFontInternal(defaultFont,0);
    }

    /** Add font to library. Will not replace existing.
     * @return false is a font by the same name already exists in store. */
    boolean addFont(Font font) {
        Objects.requireNonNull(font,"Font is null");
        if (!fontLibrary.containsKey(font.name)) {
            fontLibrary.put(font.name,font);
            return true;
        } return false;
    }

    /** Binds Font stored by {@code name}.
     * @param name font name
     * @param logicalIndex index bind the font to (0 to 4)
     * @return true if font exist and therefore was bound */
    boolean bindFont(String name, int logicalIndex) {
        Font font = storedFont(name);
        if (font == null) return false;
        bindFontInternal(font, logicalIndex);
        return true;
    }

    private void bindFontInternal(Font font, int logicalIndex) {
        Objects.checkIndex(logicalIndex, MAX_FONT_SLOTS);
        int uboArraySlot = findUboSlotOf(font);
        if (uboArraySlot == -1) {
            // not uploaded
            if (numUploadedFonts == MAX_FONT_SLOTS) {
                // Maximum number of fonts are uploaded.
                // Looks up which GPU slot logical 'index'
                // is currently using, and overwrites ONLY that one
                uboArraySlot = logicalToUboSlotMap[logicalIndex];
                uploadFont(font, uboArraySlot);
                uploadTexture(font, uboArraySlot);
                uboFontSlots[uboArraySlot] = font;
            } else {
                // There IS room to upload font without replacing.
                // we append the font to the ubo and
                // map the cpu index to the gpu index
                uploadFont(font, numUploadedFonts);
                uploadTexture(font, numUploadedFonts);
                uboFontSlots[numUploadedFonts] = font;
                logicalToUboSlotMap[logicalIndex] = numUploadedFonts;
                numUploadedFonts++;
                uploadSlotMapping();
            }
        } else if (logicalToUboSlotMap[logicalIndex] != uboArraySlot) {
            // make the index point to the actual array index.
            logicalToUboSlotMap[logicalIndex] = uboArraySlot;
            uploadSlotMapping();
        }
    }

    private void uploadTexture(Font font, int targetSlot) {
        if (font.bitmap().isDisposed()) throw new IllegalStateException("Font bitmap is disposed");
        Texture texture = font.bitmap().toTexture(false, false);
        texture.clampToEdge();
        texture.filterLinear();
        if (textures[targetSlot] != null) {
            textures[targetSlot].free();
        } textures[targetSlot] = texture;
    }

    private void uploadFont(Font font, int targetSlot) {
        uploadBuffer.clear();
        int bufferOffset = targetSlot * FONT_SIZE_FLOAT * Float.BYTES;
        for (byte c = Font.FIRST_CHAR; c <= Font.LAST_CHAR; c++) {
            pushGlyph(font.glyph(c), uploadBuffer);
        } pushGlyph(font.cursor, uploadBuffer);
        uploadBuffer.put(font.size).put(font.msdfRange).put(0).put(0);
        ubo.upload(bufferOffset, uploadBuffer.flip());
    } private void pushGlyph(Glyph glyph, FloatBuffer buffer) {
        buffer.put(glyph.u()).put(glyph.v()).put(glyph.u2()).put(glyph.v2());
        buffer.put(glyph.w()).put(glyph.h()).put(glyph.xOff()).put(glyph.yOff());
    }

    private void uploadSlotMapping() {
        try (MemoryStack stack = MemoryStack.stackPush()){
            int sizeInt = logicalToUboSlotMap.length * 4;
            IntBuffer buffer = stack.mallocInt(sizeInt);
            for (int idx : logicalToUboSlotMap) buffer.put(idx).put(0).put(0).put(0);
            ubo.upload(INDEX_MAP_OFFSET_BYTES,buffer.flip());
        }
    }

    /** Get the raw upload index for font, or -1 if not uploaded */
    private int findUboSlotOf(Font font) {
        for (int i = 0; i < uboFontSlots.length; i++) {
            if (uboFontSlots[i] == font) return i;
        } return -1;
    }

    Font boundFont(int logicalIndex) {
        return uboFontSlots[logicalToUboSlotMap[logicalIndex]];
    }

    Texture boundFontTexture(int logicalIndex) {
        return textures[logicalToUboSlotMap[logicalIndex]];
    }

    /**
     * Binds the textures assigned to logical slots 0 through 4 to consecutive
     * texture units starting at the given base offset.
     * @param baseOffset The starting texture unit (e.g. 12 for GL_TEXTURE12)
     */
    void bindTextures(int baseOffset) {
        for (int logicalSlot = 0; logicalSlot < MAX_FONT_SLOTS; logicalSlot++) {
            boundFontTexture(logicalSlot).bindToSlot(baseOffset + logicalSlot);
        }
    }

    UniformBuffer ubo() {
        return ubo;
    }

    Font storedFont(String name) {
        return fontLibrary.get(name);
    }

    int numStoredFonts() {
        return fontLibrary.size();
    }

    int numUploadedFonts() {
        return numUploadedFonts;
    }

    @Override
    public void free() {
        for (var entry : fontLibrary.entrySet()) {
            entry.getValue().free();
        } MemoryUtil.memFree(uploadBuffer);
        Disposable.free(textures);
        Disposable.free(ubo);
    }
}
