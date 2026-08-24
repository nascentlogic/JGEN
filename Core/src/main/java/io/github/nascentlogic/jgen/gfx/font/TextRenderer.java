package io.github.nascentlogic.jgen.gfx.font;

import io.github.nascentlogic.jgen.Jgen;
import io.github.nascentlogic.jgen.gfx.Texture;
import io.github.nascentlogic.jgen.gfx.UniformBuffer;
import io.github.nascentlogic.jgen.utils.Disposable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.glGetIntegerv;
import static org.lwjgl.opengl.GL31.GL_MAX_UNIFORM_BLOCK_SIZE;

/**
 * F.Dahl, 8/23/2026
 */
public class TextRenderer implements Disposable {



    @Override
    public void free() {

    }

    private static final class FontStore implements Disposable {

        private static final int NUM_FONTS = 5;
        private static final int GLYPH_SIZE_FLOAT = 8;
        private static final int FONT_SIZE_FLOAT = (Font.NUM_GLYPHS + 1) * GLYPH_SIZE_FLOAT + 4;
        private static final int FONT_SIZE_BYTES = FONT_SIZE_FLOAT * Float.BYTES;

        private final FloatBuffer uploadBuffer;
        private final UniformBuffer ubo;
        private final Texture[] textures = new Texture[NUM_FONTS];
        private final Font[] fonts = new Font[NUM_FONTS];
        private int numStored;

        FontStore(Font defaultFont) {
            uploadBuffer = MemoryUtil.memAllocFloat(FONT_SIZE_FLOAT);
            ubo = new UniformBuffer(NUM_FONTS * FONT_SIZE_BYTES,false);
            try { set(defaultFont,0);
            } catch (Exception e) {
                free();
                throw e;
            }
        }

        /**
         * Associate font with index.
         * Creates / uploads texture + font data.
         * @param font font to upload
         * @param index font index
         * @return Current font at index OR null if current == (font || null)
         */
        Font set(Font font, int index) {
            Objects.requireNonNull(font);
            Objects.checkIndex(index,NUM_FONTS);
            if (font.bitmap().isDisposed())
                throw new IllegalStateException("Font bitmap is disposed");
            Font existing = fonts[index];
            if (existing == font) return null;
            // accounting
            if (existing != null) {
                textures[index].free();
                textures[index] = null;
                fonts[index] = null;
            } else numStored++;
            // texture gen
            Texture texture = font.bitmap().toTexture(false,false);
            texture.clampToEdge();
            texture.filterLinear();
            textures[index] = texture;
            // font data --> gpu
            fonts[index] = font;
            uploadFont(font,index);
            Jgen.glCheckError();
            return existing;
        }

        private void uploadFont(Font font, int index) {
            uploadBuffer.clear();
            int bufferOffset = index * FONT_SIZE_FLOAT * Float.BYTES;
            for (byte c = Font.FIRST_CHAR; c <= Font.LAST_CHAR; c++) {
                pushGlyph(font.glyph(c), uploadBuffer);
            } pushGlyph(font.cursor, uploadBuffer);
            uploadBuffer.put(Float.intBitsToFloat(index)); // texture slot
            uploadBuffer.put(font.size).put(font.padding).put(0);
            ubo.upload(bufferOffset, uploadBuffer.flip());
        } private void pushGlyph(Glyph glyph, FloatBuffer buffer) {
            buffer.put(glyph.u()).put(glyph.v()).put(glyph.u2()).put(glyph.v2());
            buffer.put(glyph.w()).put(glyph.h()).put(glyph.xOff()).put(glyph.yOff());
        }

        @Override
        public void free() {
            MemoryUtil.memFree(uploadBuffer);
            Disposable.free(ubo);
            Disposable.free(fonts);
            Disposable.free(textures);
        }

        UniformBuffer ubo() { return ubo; }
        Texture[] textures() { return textures; }
        Font getUnchecked(int index) { return fonts[index]; }
        Font defaultFont() { return fonts[0]; }
        int numStored() { return numStored; }

    }



}
