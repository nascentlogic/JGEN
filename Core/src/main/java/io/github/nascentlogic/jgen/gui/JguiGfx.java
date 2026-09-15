package io.github.nascentlogic.jgen.gui;

import io.github.nascentlogic.jgen.Jgen;
import io.github.nascentlogic.jgen.Window;
import io.github.nascentlogic.jgen.gfx.*;
import io.github.nascentlogic.jgen.gui.util.FontUtils;
import io.github.nascentlogic.jgen.io.Disk;
import io.github.nascentlogic.jgen.text.*;
import io.github.nascentlogic.jgen.utils.Disposable;
import io.github.nascentlogic.jgen.utils.Pool;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.joml.primitives.Rectanglef;
import org.joml.primitives.Rectanglei;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.glBlendEquation;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_RED_INTEGER;

/**
 * F.Dahl, 9/6/2026
 */
public class JguiGfx implements Disposable {

    public static final int SPRITE_CAP = 512;
    public static final int TEXT_CHAR_CAP = 1024;
    public static final String SPRITE_PROGRAM_NAME = "jgen-gui-sprite";
    public static final String SPRITE_PROGRAM_DIR = "jgen/gui/glsl";
    public static final String TEXT_PROGRAM_NAME = "jgen-gui-text";
    public static final String TEXT_PROGRAM_DIR = "jgen/gui/glsl";

    private Framebuffer framebuffer;
    private final SpriteBatch spriteBatch;
    private final JgenGuiTextBatch textBatch;
    private final FontStore fontStore;
    private final TextBlock textBlockInternal;
    private final TextBuffer textBufferInternal;
    // DEBUGGING
    private int frameCounter;
    private int drawCallsHigh;
    private int drawCalls;
    private int spritesRendered;
    private int spritesRenderedHigh;
    private int charsRendered;
    private int charsRenderedHigh;
    // STATE
    public enum Batch { NONE, TEXT, SPRITE; }
    private Batch activeBatch = Batch.NONE;
    private boolean rendering = false;
    private boolean paused = false;
    private boolean idEnabled = true;
    // SCISSOR STACK
    private static final int SCISSOR_STACK_CAP = 64;
    private final Pool<Rectanglei> scissorPool = Pool.of(16,SCISSOR_STACK_CAP,Rectanglei::new);
    private final Rectanglei[] scissorStack = new Rectanglei[SCISSOR_STACK_CAP];
    private int scissorStackCount = 0;

    JguiGfx() throws Exception {
        Window window = Jgen.get().window();
        int gameW = window.gameResolutionWidth();
        int gameH = window.gameResolutionHeight();
        fontStore = new FontStore();
        framebuffer = createFramebuffer(gameW,gameH);
        spriteBatch = new SpriteBatch(gameW,gameH);
        textBatch = new JgenGuiTextBatch(fontStore,gameW,gameH);
        scissorPool.preFill(16);
        textBlockInternal = new TextBlock(512);
        textBufferInternal = new TextBuffer(2048);
    }

    public int getDrawCalls() { return drawCallsHigh; }
    public int getCharsRendered() { return charsRenderedHigh; }
    public int getSpritesRendered() { return spritesRenderedHigh; }
    public Batch getActiveBatch() { return activeBatch; }

    public void draw(Color color, float glow, Rectanglef rect, int pID) {
        draw(null, color, glow, rect, 0, 0, 1, 1 ,pID);
    } public void draw(Texture texture, float glow, Rectanglef rect, Vector4f uv, int pID) {
        draw(texture, null, glow, rect, uv.x, uv.y, uv.z, uv.w ,pID);
    } public void draw(Texture texture, Color color, float glow, Rectanglef rect, Vector4f uv, int pID) {
        draw(texture, color, glow, rect, uv.x, uv.y, uv.z, uv.w ,pID);
    } public void draw(Texture texture, float glow, Rectanglef rect, float u1, float v1, float u2, float v2, int pID) {
        draw(texture,null, glow, rect, u1, v1, u2, v2, pID);
    } public void draw(Texture texture, Color color, float glow, Rectanglef rect, float u1, float v1, float u2, float v2, int pID) {
        if (rendering && !paused) { useSpriteBatch(pID != JguiState.NO_ID);
            spriteBatch.push(texture, color, glow, rect.minX, rect.minY, rect.maxX, rect.maxY, u1, v1, u2, v2, pID);
        } else throw new IllegalStateException("Gui is paused or not in a a rendering state");
    }

    public void draw(Color color, float glow, float x, float y, float w, float h, int pID) {
        draw(null,color, glow, x, y, w, h, 0, 0, 1, 1, pID);
    } public void draw(Texture texture, float glow, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int pID) {
        draw(texture,null, glow, x, y, w, h, u1, v1, u2, v2, pID);
    } public void draw(Texture texture, Color color, float glow, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int pID) {
        if (rendering && !paused) { useSpriteBatch(pID != JguiState.NO_ID);
            spriteBatch.push(texture, color, glow, x, y, x + w, y + h, u1, v1, u2, v2, pID);
        } else throw new IllegalStateException("Gui is paused or not in a a rendering state");
    }

    public void drawRotated(Texture texture, float glow, Rectanglef rect, Vector4f uv, int pID, float rot) {
        drawRotated(texture, null, glow, rect, uv.x, uv.y, uv.z, uv.w ,pID, rot);
    } public void drawRotated(Texture texture, Color color, float glow, Rectanglef rect, Vector4f uv, int pID, float rot) {
        drawRotated(texture, color, glow, rect, uv.x, uv.y, uv.z, uv.w ,pID, rot);
    } public void drawRotated(Color color, float glow, Rectanglef rect, int pID, float rot) {
        drawRotated(null, color, glow, rect, 0, 0, 1, 1, pID, rot);
    } public void drawRotated(Texture texture, float glow, Rectanglef rect, float u1, float v1, float u2, float v2, int pID, float rot) {
        drawRotated(texture,null, glow, rect, u1, v1, u2, v2, pID, rot);
    } public void drawRotated(Texture texture, Color color, float glow, Rectanglef rect, float u1, float v1, float u2, float v2, int pID, float rot) {
        if (rendering && !paused) { useSpriteBatch(pID != JguiState.NO_ID);
            spriteBatch.pushRotated(texture, color, glow, rect.minX, rect.minY, rect.maxX, rect.maxY, u1, v1, u2, v2, pID, rot);
        } else throw new IllegalStateException("Gui is paused or not in a a rendering state");
    }

    public void drawRotated(Color color, float glow, float x, float y, float w, float h, int pID, float rot) {
        drawRotated(null,color, glow, x, y, w, h, 0, 0, 1, 1, pID, rot);
    } public void drawRotated(Texture texture, float glow, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int pID, float rot) {
        drawRotated(texture,null, glow, x, y, w, h, u1, v1, u2, v2, pID, rot);
    } public void drawRotated(Texture texture, Color color, float glow, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int pID, float rot) {
        if (rendering && !paused) { useSpriteBatch(pID != JguiState.NO_ID);
            spriteBatch.pushRotated(texture, color, glow, x, y, x + w, y + h, u1, v1, u2, v2, pID, rot);
        } else throw new IllegalStateException("Gui is paused or not in a a rendering state");
    }




    public void drawChar(float penX, float penY, byte character, int fontSize, int font, Color color) {
        if (rendering && !paused) { useTextBatch();
            textBatch.push(penX, penY, character, fontSize, font, color);
        } else throw new IllegalStateException("Gui is paused or not in a a rendering state");
    }

    public void drawString(String string, float x, float y, int fontSize, int font, Color color) {
        drawText(normalizeString(string),x,y,fontSize,font,color);
    }

    public void drawText(Text text, float x, float y, int fontSize, int fontIndex, Color color) {
        if (rendering && !paused) {
            if (text == null || text.length() == 0 || fontSize <= 0) return;
            useTextBatch();
            Font font = fontStore.boundFont(fontIndex);
            FontUtils.streamBoundless(text, x, y, color, font, fontSize, fontIndex, textBatch::push);
            //drawTextNoWrap(text,x,y,fontSize,fontIndex,color == null ? Color.WHITE : color);
        } else throw new IllegalStateException("Gui is paused or not in a a rendering state");
    }

    private void drawTextNoWrap(Text text, float x, float y, int fontSize, int fontIndex, Color color) {
        Font font = fontStore.boundFont(fontIndex);
        final float scale = fontSize / font.size;
        final float spaceAdvance = font.glyph(Text.SPACE).advance() * scale;
        final float tabAdvance = font.indentAdvance() * scale;
        final float lineHeight = font.lineHeight() * scale;
        final int textLength = text.length();
        final int tabLen = 4;
        int col = 0;
        int row = 0;
        int i = 0;

        int fontBits = 0;
        float fColor = color.packedFormat();
        fontBits |= ((fontSize & 0xFF) << 8);
        fontBits |= ((fontIndex & 0xFF) << 16);

        if (font.monospaced) {
            while (i < textLength) {
                byte c = text.get(i);
                if (c == Text.LINE_FEED) {
                    row++;
                    col = 0;
                } else if (c == Text.SPACE) {
                    col++;
                } else if (c == Text.TAB) {
                    int indents = tabLen - (col % tabLen);
                    col += indents;
                } else {
                    float penX = x + col * spaceAdvance;
                    float penY = y - row * lineHeight;
                    int iData = fontBits | (c & 0xFF);
                    float fData = Float.intBitsToFloat(iData);
                    textBatch.push(penX,penY,fData,fColor);
                    col++;
                }
                i++;
            }
        } else {


        }
    }


    private Text normalizeString(String string) {
        String str = string == null ? "" : string;
        ManagedText text;
        if (str.length() > textBlockInternal.capacity()) {
            text = textBufferInternal;
        } else text = textBlockInternal;
        text.set(str);
        return text;
    }

    void beginFrame() {
        if (!rendering) {
            if (frameCounter == 60) {
                drawCallsHigh = drawCalls;
                spritesRenderedHigh = spritesRendered;
                charsRenderedHigh = charsRendered;
                frameCounter = 0;
            }
            // Resize to game resolution (rare)
            Window window = Jgen.get().window();
            int gameW = window.gameResolutionWidth();
            int gameH = window.gameResolutionHeight();
            int buffW = framebuffer.width();
            int buffH = framebuffer.height();
            if (gameW != buffW || gameH != buffH) {
                Disposable.free(framebuffer);
                framebuffer = createFramebuffer(gameW,gameH);
                spriteBatch.onResize(gameW,gameH);
                textBatch.onResize(gameW,gameH);
            }
            framebuffer.bindDraw();
            framebuffer.viewport();
            framebuffer.drawbuffers(0,1);
            framebuffer.clearColor(0,0,0,0,0);
            framebuffer.clearColorUint(1, JguiState.NO_ID);
            glEnable(GL_BLEND);
            glBlendEquation(GL_FUNC_ADD);
            glBlendFunc(GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_SCISSOR_TEST);
            activeBatch = Batch.NONE;
            rendering = true;
        }
    }

    void endFrame() {
        if (rendering) {
            resume();
            flushActiveBatch();
            clearScissorStack();
            drawCalls = spriteBatch.resetDrawCalls();
            drawCalls += textBatch.resetDrawCalls();
            drawCallsHigh = Math.max(drawCallsHigh,drawCalls);
            spritesRendered = spriteBatch.resetCountAccum();
            spritesRenderedHigh = Math.max(spritesRenderedHigh,spritesRendered);
            charsRendered = textBatch.resetCountAccum();
            charsRenderedHigh = Math.max(charsRenderedHigh,charsRendered);
            frameCounter++;
            activeBatch = Batch.NONE;
            rendering = false;
        }
    }


    public void pause() {
        if (rendering && !paused) {
            paused = true;
        }
    }

    public void resume() {
        if (rendering && paused) {
            framebuffer.bindDraw();
            framebuffer.viewport();
            glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glBlendEquation(GL_FUNC_ADD);
            glBlendFunc(GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
            if (scissorStackCount == 0) glDisable(GL_SCISSOR_TEST);
            else { Rectanglei scissor = scissorStack[scissorStackCount - 1];
                if (scissor.isValid()) {
                    glScissor(scissor.minX, scissor.minY, scissor.lengthX(), scissor.lengthY());
                } else glScissor(0, 0, 0, 0);
                glEnable(GL_SCISSOR_TEST);
            } paused = false;
        }
    }

    int readIdBuffer(Vector2f mouse) {
        int mouseX = Math.clamp(Math.round(mouse.x),0,framebuffer.width()  - 1);
        int mouseY = Math.clamp(Math.round(mouse.y),0,framebuffer.height() - 1);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            framebuffer.bindRead();
            IntBuffer buf = stack.callocInt(1);
            glReadPixels(mouseX,mouseY,1,1, GL_RED_INTEGER, GL_UNSIGNED_INT,buf);
            return buf.get(0);
        }
    }

    Texture colorAttachement() {
        return framebuffer.attachment(0);
    }

    // =============================================================================
    // SCISSOR STACK
    // =============================================================================


    public void scissorPush(float x1, float y1, float x2, float y2) {
        if (rendering && !paused) {
            if (scissorStackCount == SCISSOR_STACK_CAP) throw new BufferOverflowException();
            flushActiveBatch(); // Always flush before state change
            int minX = (int) Math.floor(Math.min(x1, x2));
            int minY = (int) Math.floor(Math.min(y1, y2));
            int maxX = (int) Math.ceil(Math.max(x1, x2));
            int maxY = (int) Math.ceil(Math.max(y1, y2));
            Rectanglei scissor = scissorPool.obtain();
            scissor.setMin(minX, minY);
            scissor.setMax(maxX, maxY);
            Rectanglei screen = scissorPool.obtain();
            screen.setMin(0, 0);
            screen.setMax(framebuffer.width(),framebuffer.height());
            scissor.intersection(screen);
            scissorPool.free(screen);
            scissorStack[scissorStackCount++] = scissor;
            if (scissor.isValid()) glScissor(scissor.minX, scissor.minY, scissor.lengthX(), scissor.lengthY());
            else glScissor(0, 0, 0, 0);
            glEnable(GL_SCISSOR_TEST);
        } else throw new IllegalStateException("Cannot push/pop scissors while renderer is paused or not rendering");
    }

    public void scissorPop() {
        if (rendering && !paused) {
            if (scissorStackCount == 0) throw new BufferUnderflowException();
            flushActiveBatch(); // before changing GL state
            int idx = --scissorStackCount;
            Rectanglei current = scissorStack[idx];
            scissorStack[idx] = null; // Clear array slot reference
            if (scissorStackCount == 0) glDisable(GL_SCISSOR_TEST);
            else { Rectanglei scissor = scissorStack[scissorStackCount - 1];
                if (scissor.isValid()) glScissor(scissor.minX, scissor.minY, scissor.lengthX(), scissor.lengthY());
                else glScissor(0, 0, 0, 0);
                glEnable(GL_SCISSOR_TEST);
            } scissorPool.free(current);
        } else throw new IllegalStateException("Cannot push/pop scissors while renderer is paused or not rendering");
    }

    public Rectanglei scissorPeek() {
        if (scissorStackCount == 0) return null;
        return scissorStack[scissorStackCount - 1];
    }

    private void clearScissorStack() {
        while (scissorStackCount > 0) {
            int idx = --scissorStackCount;
            scissorPool.free(scissorStack[idx]);
            scissorStack[idx] = null;
        } glDisable(GL_SCISSOR_TEST);
    }

    // =============================================================================
    // FontStore
    // =============================================================================

    /** @see FontStore#boundFont(int)  */
    public Font boundFont(int index) { return fontStore.boundFont(index); }
    /** @see FontStore#storedFont(String)  */
    public Font storedFont(String name) { return fontStore.storedFont(name); }
    /** @see FontStore#numStoredFonts()  */
    public int numStoredFonts() { return fontStore.numStoredFonts(); }
    /** @see FontStore#numUploadedFonts() */
    public int numUploadedFonts() { return fontStore.numUploadedFonts(); }
    /** @see FontStore#addFont(Font) */
    public boolean addFont(Font font) { return fontStore.addFont(font); }
    /** @see FontStore#bindFont(String, int) */
    public boolean bindFont(String name, int index) {
        if (rendering) flushActiveBatch();
        return fontStore.bindFont(name,index);
    }



    /** Careful: Only use this when rendering && !paused */
    private void useTextBatch() {
        if (activeBatch == Batch.TEXT) return;
        flushActiveBatch();
        framebuffer.drawbuffer(0);
        activeBatch = Batch.TEXT;
        idEnabled = false;
    }

    /** Careful: Only use this when rendering && !paused */
    private void useSpriteBatch(boolean enableId) {
        if (activeBatch == Batch.SPRITE) {
            if (idEnabled == enableId) return;
            idEnabled = enableId;
            flushActiveBatch();
        } else {
            idEnabled = enableId;
            flushActiveBatch();
            activeBatch = Batch.SPRITE;
        }
        if (idEnabled) framebuffer.drawbuffers(0, 1);
        else framebuffer.drawbuffer(0);
    }

    /** Careful: Only use this when rendering && !paused */
    private void flushActiveBatch() {
        switch (activeBatch) {
            case TEXT -> textBatch.flush();
            case SPRITE -> spriteBatch.flush();
        }
    }


    private Framebuffer createFramebuffer(int width, int height) {
        Texture colorTexture = Texture.generate2D(width,height);
        // RGBA16F is extremly important when STORING premultiplied alpha.
        colorTexture.allocate(TextureFormat.RGBA16F,false);
        colorTexture.filterLinear();
        colorTexture.clampToEdge();
        Texture idTexture = Texture.generate2D(width,height);
        idTexture.allocate(TextureFormat.R32UI,false);
        idTexture.filterNearest();
        idTexture.clampToEdge();
        Framebuffer framebuffer = new Framebuffer(width, height);
        framebuffer.bindBoth();
        framebuffer.attachTexture(colorTexture,0,true);
        framebuffer.attachTexture(idTexture,1,true);
        framebuffer.readbuffer(1);
        framebuffer.drawbuffers(0,1);
        framebuffer.clearColorUint(1, JguiState.NO_ID);
        Framebuffer.checkReadStatus();
        Framebuffer.checkDrawStatus();
        return framebuffer;
    }

    @Override
    public void free() {
        Disposable.free(
                fontStore,
                textBatch,
                spriteBatch,
                framebuffer,
                textBlockInternal,
                textBufferInternal
        );
    }

    public static final class SpriteBatch implements Disposable {
        /* ----------------------------------------
         * vertex: | pos | uv | color | id | data |
         * ----------------------------------------
         * size:   | 2   | 2  | 1     | 1  | 1    |
         * ----------------------------------------*/
        public static final int VERTEX_SIZE_FLOAT = 7;
        public static final int VERTEX_SIZE_BYTES = VERTEX_SIZE_FLOAT * Float.BYTES;
        public static final int SPRITE_SIZE_FLOAT = VERTEX_SIZE_FLOAT * 4;
        public static final int BATCH_SIZE_FLOAT = SPRITE_CAP * SPRITE_SIZE_FLOAT;
        public static final int BATCH_SIZE_BYTES = BATCH_SIZE_FLOAT * Float.BYTES;
        // offset is not really needed. I'm not currently taking advantage of it anyway.
        public static final int TEXTURE_UNIT_OFFSET = FontStore.MAX_FONT_SLOTS;
        public static final int TEXTURE_SLOTS = 8; // same as in the shader

        private final Texture[] textureSlots = new Texture[TEXTURE_SLOTS];  // Auto texture slot assignment
        private int nextSlot;
        private int prevSlot;

        private final ShaderProgram program;
        private final FloatBuffer vertices;
        private final int vao;
        private final int vbo;
        private final int ebo; // 0,1,2,2,3,0 ...
        private int count;
        private int countAccum;
        private int drawCalls;
        private int flagsMask = (1 << 21); // Default: pixelArtAA = false (0), transparentID = true (1 << 21)
        private final float whitePacked = Color.WHITE.packedFormat();

        SpriteBatch(int width, int height) throws Exception {
            ShaderProgram program = ShaderProgram.getProgramByName(SPRITE_PROGRAM_NAME); // just in case
            if (program == null) {
                Shader shader = Disk.resourceShader(SPRITE_PROGRAM_NAME, SPRITE_PROGRAM_DIR);
                program = new ShaderProgram(shader);
            } this.program = program;
            program.use();
            ShaderProgram.setUniformF("uResolution", width, height);
            int[] samplers = new int[TEXTURE_SLOTS];
            for (int i = 0; i < samplers.length; i++) {
                samplers[i] = i + TEXTURE_UNIT_OFFSET; // 5, 6, 7, ... 12
            } ShaderProgram.setUniformI("uTextures",samplers);
            ShaderProgram.useNone();
            vertices = MemoryUtil.memAllocFloat(BATCH_SIZE_FLOAT);
            vao = Buffers.generateBindVAO();
            ebo = Buffers.generateQuadEBO(SPRITE_CAP);
            vbo = Buffers.generateVBO(GL_DYNAMIC_DRAW,BATCH_SIZE_BYTES);
            int pos = 0;
            int stride = VERTEX_SIZE_FLOAT * Float.BYTES;
            glVertexAttribPointer(0, 2, GL_FLOAT,         false, stride, pos); pos += 2 * Float.BYTES;
            glVertexAttribPointer(1, 2, GL_FLOAT,         false, stride, pos); pos += 2 * Float.BYTES;
            glVertexAttribPointer(2, 4, GL_UNSIGNED_BYTE, true,  stride, pos); pos += Float.BYTES;
            glVertexAttribPointer(3, 1, GL_FLOAT,         false, stride, pos); pos += Float.BYTES;
            glVertexAttribPointer(4, 1, GL_FLOAT,         false, stride, pos);
            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);
            glEnableVertexAttribArray(2);
            glEnableVertexAttribArray(3);
            glEnableVertexAttribArray(4);
            Buffers.bindVBO(0);
            Buffers.bindVAO(0);
        }

        void flush() {
            if (count > 0) {
                program.use();
                bindTextures();
                Buffers.bindVBO(vbo);
                Buffers.uploadVBO(vertices.flip());
                Buffers.bindVAO(vao);
                glDrawElements(GL_TRIANGLES,6 * count,GL_UNSIGNED_SHORT,0);
                vertices.clear();
                drawCalls++;
                countAccum += count;
                count = 0;
            }
        }

        int resetDrawCalls() {
            int tmp = drawCalls;
            drawCalls = 0;
            return tmp;
        }

        int resetCountAccum() {
            int tmp = countAccum;
            countAccum = 0;
            return tmp;
        }

        void onResize(int width, int height) {
            program.use();
            ShaderProgram.setUniformF("uResolution",width,height);
        }

        void enablePixelArtAA(boolean enable) {
            if (enable) flagsMask |= (1 << 20);
            else        flagsMask &= ~(1 << 20);
        }

        boolean pixelArtAAEnabled() {
            return (flagsMask & (1 << 20)) != 0;
        }

        void enableTransparentID(boolean enable) {
            if (enable) flagsMask |= (1 << 21);
            else        flagsMask &= ~(1 << 21);
        }

        boolean transparentIDEnabled() {
            return (flagsMask & (1 << 21)) != 0;
        }

        void push(Texture texture, Color color, float glow, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2, int pID) {
            if (count == SPRITE_CAP) flush();
            int data = resolveTextureSlot(texture) | flagsMask;
            if (glow > 0.0f) {
                int glowBits = (int) (65535f * Math.min(glow, 1.0f));
                data |= (glowBits << 4);
            } // Convert bits & color to raw float representations
            final float fData = Float.intBitsToFloat(data);
            final float fColor = (color != null) ? color.packedFormat() : whitePacked;
            final float fID = Float.intBitsToFloat(pID);
            put(x1, y1, x2, y2, u1, v1, u2, v2, fColor, fData, fID);
        }

        void pushRotated(Texture texture, Color color, float glow, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2, int pID, float rot) {
            if (count == SPRITE_CAP) flush();
            int data = resolveTextureSlot(texture) | flagsMask;
            if (glow > 0.0f) {
                int glowBits = (int) (65535f * Math.min(glow, 1.0f));
                data |= (glowBits << 4);
            } // Convert bits & color to raw float representations
            final float fData = Float.intBitsToFloat(data);
            final float fColor = (color != null) ? color.packedFormat() : whitePacked;
            final float fID = Float.intBitsToFloat(pID);
            put(x1, y1, x2, y2, u1, v1, u2, v2, fColor, fData, fID, rot);
        }

        private void put(float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2, float color, float data, float pID) {
            vertices.put(x1).put(y2).put(u1).put(v1).put(color).put(pID).put(data); // v0: Top-Left (x1, y2) -> (u1, v1)
            vertices.put(x1).put(y1).put(u1).put(v2).put(color).put(pID).put(data); // v1: Bottom-Left (x1, y1) -> (u1, v2)
            vertices.put(x2).put(y1).put(u2).put(v2).put(color).put(pID).put(data); // v2: Bottom-Right (x2, y1) -> (u2, v2)
            vertices.put(x2).put(y2).put(u2).put(v1).put(color).put(pID).put(data); // v3: Top-Right (x2, y2) -> (u2, v1)
            count++;
        }

        private void put(float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2, float color, float data, float pID, float rot) {
            float wh = (x2 - x1) * 0.5f;
            float hh = (y2 - y1) * 0.5f;
            float cx = x1 + wh;
            float cy = y1 + hh;
            float sin = (float) Math.sin(rot);
            float cos = (float) Math.cos(rot);
            float sinWh = sin * wh;
            float sinHh = sin * hh;
            float cosWh = cos * wh;
            float cosHh = cos * hh;
            vertices.put(-cosWh - sinHh + cx).put(-sinWh + cosHh + cy).put(u1).put(v1).put(color).put(pID).put(data);
            vertices.put(-cosWh + sinHh + cx).put(-sinWh - cosHh + cy).put(u1).put(v2).put(color).put(pID).put(data);
            vertices.put( cosWh + sinHh + cx).put( sinWh - cosHh + cy).put(u2).put(v2).put(color).put(pID).put(data);
            vertices.put( cosWh - sinHh + cx).put( sinWh + cosHh + cy).put(u2).put(v1).put(color).put(pID).put(data);
            count++;
        }

        private int resolveTextureSlot(Texture texture) {
            // Invalid or missing texture -> use white pixel / dummy slot
            if (texture == null || texture.target() != GL_TEXTURE_2D || texture.isDisposed()) return TEXTURE_SLOTS;
            // Fast Path: Check recently bound slot cache sentinel
            if (textureSlots[prevSlot] == texture) return prevSlot;
            // Linear Scan: Check if already registered in active slots
            for (int i = 0; i < nextSlot; i++) {
                if (textureSlots[i] == texture) {
                    prevSlot = i;
                    return i;
                }
            } // All texture slots full -> flush batch to free up slots
            if (nextSlot == TEXTURE_SLOTS) {
                nextSlot = 0;
                prevSlot = 0;
                flush();
            } // Register texture into next available slot
            int slot = nextSlot;
            textureSlots[slot] = texture;
            prevSlot = slot;
            nextSlot = slot + 1;
            return slot;
        }

        private void bindTextures() {
            for (int i = 0; i < nextSlot; i++) {  // Bind Textures to offset texture units (currently: 5..12)
                int textureUnit = i + TEXTURE_UNIT_OFFSET;
                textureSlots[i].bindToSlot(textureUnit); // GL_TEXTURE0 + (i + 5)
                textureSlots[i] = null;  // clear array slot for the next batch
            } nextSlot = prevSlot = 0;
        }

        @Override
        public void free() {
            // program is disposed by engine
            MemoryUtil.memFree(vertices);
            Buffers.deleteVAO(vao);
            Buffers.deleteBuffers(vbo,ebo);
            Arrays.fill(textureSlots, null);
            prevSlot = 0;
            nextSlot = 0;
        }
    }

    public static final class JgenGuiTextBatch implements Disposable {

        public static final int TEXT_BLOCK_BINDING = 8;
        public static final int VERTEX_SIZE_FLOAT = 4;
        public static final int BATCH_SIZE_FLOAT = TEXT_CHAR_CAP * VERTEX_SIZE_FLOAT;
        public static final int BATCH_SIZE_BYTES = BATCH_SIZE_FLOAT * Float.BYTES;

        private final FontStore fontStore;
        private final ShaderProgram program;
        private final FloatBuffer vertices;
        private final int vao;
        private final int vbo;
        private int count;
        private int countAccum;
        private int drawCalls;

        JgenGuiTextBatch(FontStore fontStore, int width, int height) throws Exception {
            this.fontStore = fontStore;
            ShaderProgram program = ShaderProgram.getProgramByName(TEXT_PROGRAM_NAME);
            if (program == null) {
                Shader shader = Disk.resourceShader(TEXT_PROGRAM_NAME, TEXT_PROGRAM_DIR);
                program = new ShaderProgram(shader);
            } this.program = program;
            program.use();
            ShaderProgram.setUniformF("uResolution",width,height);
            int[] samplers = new int[FontStore.MAX_FONT_SLOTS];
            for (int i = 0; i < samplers.length; i++) samplers[i] = i;
            ShaderProgram.setUniformI("uTextures",samplers);
            ShaderProgram.useNone();
            vertices = MemoryUtil.memAllocFloat(BATCH_SIZE_FLOAT);
            vao = Buffers.generateBindVAO();
            vbo = Buffers.generateVBO(GL_DYNAMIC_DRAW,BATCH_SIZE_BYTES);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 4 * Float.BYTES, 0);
            glVertexAttribPointer(1,4,GL_UNSIGNED_BYTE,true,4 * Float.BYTES,3 * Float.BYTES);
            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);
            Buffers.bindVBO(0);
            Buffers.bindVAO(0);
        }

        void push(float penX, float penY, byte character, int fontSize, int font, Color color) {
            int iData = character & 0xFF;
            iData |= ((fontSize & 0xFF) << 8);
            iData |= ((font & 0xFF) << 16);
            float fData = Float.intBitsToFloat(iData);
            float fColor = color == null ? Color.WHITE.packedFormat() : color.packedFormat();
            push(penX, penY, fData, fColor);
        }

        void push(float penX, float penY, float fData, float fColor) {
            if (count == TEXT_CHAR_CAP) flush();
            vertices.put(penX).put(penY).put(fData).put(fColor);
            count++;
        }

        int resetDrawCalls() {
            int tmp = drawCalls;
            drawCalls = 0;
            return tmp;
        } int resetCountAccum() {
            int tmp = countAccum;
            countAccum = 0;
            return tmp;
        }

        void onResize(int width, int height) {
            program.use();
            ShaderProgram.setUniformF("uResolution",width,height);
        }

        void flush() {
            if (count > 0) {
                program.use();
                fontStore.bindTextures(0);
                fontStore.ubo().bindBufferBase(TEXT_BLOCK_BINDING);
                Buffers.bindVBO(vbo);
                Buffers.uploadVBO(vertices.flip());
                Buffers.bindVAO(vao);
                glDrawArrays(GL_POINTS, 0, count);
                vertices.clear();
                drawCalls++;
                countAccum += count;
                count = 0;
            }
        }
        @Override
        public void free() {
            MemoryUtil.memFree(vertices);
            Buffers.deleteVAO(vao);
            Buffers.deleteBuffer(vbo);
        }
    }


}
