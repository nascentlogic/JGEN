package io.github.nascentlogic.jgen.gui;

import io.github.nascentlogic.jgen.*;
import io.github.nascentlogic.jgen.gfx.*;
import io.github.nascentlogic.jgen.gui.api.JuiGraphicsAPI;
import io.github.nascentlogic.jgen.gui.api.JuiLayoutAPI;
import io.github.nascentlogic.jgen.gui.api.JuiStateAPI;
import io.github.nascentlogic.jgen.gui.util.*;
import io.github.nascentlogic.jgen.io.Disk;
import io.github.nascentlogic.jgen.gui.text.ManagedText;
import io.github.nascentlogic.jgen.gui.text.Text;
import io.github.nascentlogic.jgen.gui.text.TextBlock;
import io.github.nascentlogic.jgen.gui.text.TextBuffer;
import io.github.nascentlogic.jgen.utils.Disposable;
import io.github.nascentlogic.jgen.utils.Pool;
import org.joml.*;
import org.joml.primitives.Rectanglef;
import org.joml.primitives.Rectanglei;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.tinylog.Logger;

import java.lang.Math;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.glBlendEquation;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * F.Dahl, 9/14/2026
 */
public class JuiCore implements JuiStateAPI, JuiLayoutAPI, JuiGraphicsAPI {

    private static final int DEFERRED_QUEUE_CAP     = 512;
    private static final int CONTAINER_STACK_CAP    = 128;
    private static final int SCISSOR_STACK_CAP      = 64;
    private static final int ID_STACK_CAP           = 128;
    private static final int BATCH_NONE             = 0;
    private static final int SPRITE_BATCH           = 1;
    private static final int TEXT_BATCH             = 2;
    private static final int TEXT_BATCH_CAP         = 1024;
    private static final int TEXT_BLOCK_BINDING     = 8;
    private static final int SPRITE_BATCH_CAP       = 512;

    private Framebuffer framebuffer;
    private final FontLibrary fontLibrary;
    private final TextBatch textBatch;
    private final SpriteBatch spriteBatch;
    private final DeferredCalls deferredCalls;
    private final TextBlock textBlockInternal;
    private final TextBuffer textBufferInternal;

    // RENDER STATE
    private int currentBatch;
    private boolean rendering;
    private boolean rendererPaused;
    private boolean idBufferEnabled;
    private boolean deferredState;

    // DEBUGGING
    private int frameCounter;
    private int drawCalls;
    private int drawCallsHigh;
    private int charsRendered;
    private int spritesRendered;
    private int charsRenderedHigh;
    private int spritesRenderedHigh;
    private int deferredCallsMax;

    private  int hoveredID           = NULL;
    private  int pressedID           = NULL;
    private  int selectedID          = NULL;
    private  int draggedID           = NULL;
    private  int focusedID           = NULL;
    private  int focusRequest        = NULL;

    private  int lastHoveredID       = NULL;
    private  int lastPressedID       = NULL;
    private  int lastDraggedID       = NULL;
    private  int lastFocusedID       = NULL;

    private  int activeMouseBtn      = MOUSE_NONE;
    private  int lastActiveMouseBtn  = MOUSE_NONE;
    private  int navigationBtn       = NULL;

    private  long hoveredDurationNS  = 0L;
    private  long pressedDurationNS  = 0L;
    private  long focusedDurationNS  = 0L;

    private  float mousePositionX    = 0.0f;
    private  float mousePositionY    = 0.0f;
    private  float mousePressOriginX = 0.0f;
    private  float mousePressOriginY = 0.0f;
    private  float mouseFrameDeltaX  = 0.0f;
    private  float mouseFrameDeltaY  = 0.0f;
    private  float mouseDragVectorX  = 0.0f;
    private  float mouseDragVectorY  = 0.0f;
    private  float mouseLastFrameX   = 0.0f;
    private  float mouseLastFrameY   = 0.0f;


    public JuiCore() throws Exception {
        Window window = Jgen.get().window();
        int gameW = window.gameResolutionWidth();
        int gameH = window.gameResolutionHeight();
        fontLibrary = new FontLibrary();
        framebuffer = createFramebuffer(gameW,gameH);
        textBatch = new TextBatch(fontLibrary,gameW,gameH);
        spriteBatch = new SpriteBatch(gameW,gameH);
        textBlockInternal = new TextBlock(512);
        textBufferInternal = new TextBuffer(2048);
        deferredCalls = new DeferredCalls();
        reset();
    }

    public void free() {
        Disposable.free(
                fontLibrary,
                textBatch,
                spriteBatch,
                framebuffer,
                textBlockInternal,
                textBufferInternal
        );
    }


    public void beginFrame() {
        if (rendering) return;
        if (idStackDepth != 0) throw new IllegalStateException("Mismatched gui id stack depth");
        if (scissorStackDepth != 0) throw new IllegalStateException("Mismatched gui container stack depth");
        if (containerStackDepth != 0) throw new IllegalStateException("Mismatched gui container stack depth");

        Window window = Jgen.get().window();
        Mouse mouse = Jgen.get().mouse();
        Keyboard keys = Jgen.get().keys();
        Gamepads gamepads = Jgen.get().gamepads(); // later

        // =============================================================================
        // FRAME STATE RESET
        // =============================================================================
        // Previous frame state snapshot
        lastHoveredID = hoveredID; hoveredID = readIdBuffer();
        lastPressedID = pressedID;
        lastDraggedID = draggedID;
        lastFocusedID = focusedID; focusedID = focusRequest;
        lastActiveMouseBtn = activeMouseBtn;
        // Reset single-frame pulse events
        navigationBtn = NULL;
        selectedID = NULL;
        // mouse variables
        mouseLastFrameX = mousePositionX;
        mouseLastFrameY = mousePositionY;
        mousePositionX = mouse.position().x;
        mousePositionY = mouse.position().y;
        mouseFrameDeltaX = mousePositionX - mouseLastFrameX;
        mouseFrameDeltaY = mousePositionY - mouseLastFrameY;
        // =============================================================================
        // RELEASE / END INTERACTION
        // =============================================================================
        if (pressedID != NULL && activeMouseBtn != MOUSE_NONE) {
            if (mouse.justReleased(activeMouseBtn)) {
                // Register a selection click ONLY if it wasn't converted into a drag operation
                if (hoveredID == pressedID && draggedID == NULL) selectedID = pressedID;
                draggedID = NULL;
                pressedID = NULL;
                activeMouseBtn = MOUSE_NONE;
            }
        }
        // =============================================================================
        // CAPTURE NEW PRESS
        // =============================================================================
        if (pressedID == NULL && hoveredID != NULL) {
            for (int button = 0; button < MOUSE_BUTTONS; button++) {
                if (mouse.justPressed(button)) {
                    pressedID = hoveredID;
                    activeMouseBtn = button;
                    mousePressOriginX = mousePositionX;
                    mousePressOriginY = mousePositionY;
                    mouseDragVectorX = 0f;
                    mouseDragVectorY = 0f;
                    break;
                }
            }
        }
        // =============================================================================
        // DRAG THRESHOLD & DELTA EVALUATION
        // =============================================================================
        if (pressedID != NULL) {
            mouseDragVectorX = mousePositionX - mousePressOriginX;
            mouseDragVectorY = mousePositionY - mousePressOriginY;
            if (draggedID == NULL) {
                float xSqr = mouseDragVectorX * mouseDragVectorX;
                float ySqr = mouseDragVectorY * mouseDragVectorY;
                if (xSqr + ySqr >= (DRAG_THRESHOLD * DRAG_THRESHOLD)) draggedID = pressedID;
            }
        } else {
            mouseDragVectorX = 0f;
            mouseDragVectorY = 0f;
        }
        // =============================================================================
        // KEYBOARD / GAMEPAD SELECTION
        // =============================================================================

        if (focusedID != NULL) {
            if (keys.justPressed(GLFW.GLFW_KEY_ENTER)) {
                selectedID = focusedID;
                navigationBtn = NAV_SELECT;
            } else if (keys.justPressed(GLFW.GLFW_KEY_UP)) {
                navigationBtn = NAV_UP;
            } else if (keys.justPressed(GLFW.GLFW_KEY_RIGHT)) {
                navigationBtn = NAV_RIGHT;
            } else if (keys.justPressed(GLFW.GLFW_KEY_DOWN)) {
                navigationBtn = NAV_DOWN;
            } else if (keys.justPressed(GLFW.GLFW_KEY_LEFT)) {
                navigationBtn = NAV_LEFT;
            } else if (keys.justPressed(GLFW.GLFW_KEY_PAGE_UP)) {
                navigationBtn = NAV_START;
            } else if (keys.justPressed(GLFW.GLFW_KEY_PAGE_DOWN)) {
                navigationBtn = NAV_END;
            } else if (keys.justPressed(GLFW.GLFW_KEY_ESCAPE)) {
                navigationBtn = NAV_EXIT;
            }
        }
        // =============================================================================
        // DURATION TRACKING
        // =============================================================================
        long deltaTimeNS = Jgen.get().time().deltaTimeNS();
        hoveredDurationNS = (hoveredID != NULL && hoveredID == lastHoveredID) ? hoveredDurationNS + deltaTimeNS : 0L;
        pressedDurationNS = (pressedID != NULL && pressedID == lastPressedID) ? pressedDurationNS + deltaTimeNS : 0L;
        focusedDurationNS = (focusedID != NULL && focusedID == lastFocusedID) ? focusedDurationNS + deltaTimeNS : 0L;

        // Resize to game resolution (rare, if ever)
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
        framebuffer.clearColorUint(1, NULL);
        glEnable(GL_BLEND);
        glBlendEquation(GL_FUNC_ADD);
        glBlendFunc(GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_SCISSOR_TEST);
        glDisable(GL_STENCIL_TEST);

        fontLibrary.bindTextures(0);
        fontLibrary.ubo().bindBufferBase(TEXT_BLOCK_BINDING);


        if (frameCounter == 60) {
            drawCallsHigh = drawCalls;
            spritesRenderedHigh = spritesRendered;
            charsRenderedHigh = charsRendered;
            frameCounter = 0;
        }

        currentBatch = SPRITE_BATCH;
        idBufferEnabled = true;
        rendering = true;
    }

    public Texture endFrame() {
        if (!rendering) throw new IllegalStateException();
        resumeRenderer();

        flushCurrentBatch();
        glDisable(GL_SCISSOR_TEST);
        drawCalls = spriteBatch.resetDrawCalls();
        drawCalls += textBatch.resetDrawCalls();
        drawCallsHigh = Math.max(drawCallsHigh,drawCalls);
        spritesRendered = spriteBatch.resetCountAccumulator();
        spritesRenderedHigh = Math.max(spritesRenderedHigh,spritesRendered);
        charsRendered = textBatch.resetCountAccumulator();
        charsRenderedHigh = Math.max(charsRenderedHigh,charsRendered);
        frameCounter++;
        rendering = false;
        return framebuffer.attachment(0);
    }

    public void pauseRenderer() {
        if (rendering && !rendererPaused) {
            rendererPaused = true;
        }
    }

    public void resumeRenderer() {
        if (rendering && rendererPaused) {
            framebuffer.bindDraw();
            framebuffer.viewport();
            glDisable(GL_DEPTH_TEST);
            glDisable(GL_STENCIL_TEST);
            glEnable(GL_BLEND);
            glBlendEquation(GL_FUNC_ADD);
            glBlendFunc(GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
            scissorUseCurrent();
            fontLibrary.bindTextures(0);
            fontLibrary.ubo().bindBufferBase(TEXT_BLOCK_BINDING);
            rendererPaused = false;
        }
    }


    /** Jui fresh start.
     * <p>Clears state, persistent storage, framebuffer etc.</p>
     * Useful for game state changes, like transitioning from main menu into game and back.*/
    public void reset() {
        if (rendering) throw new IllegalStateException("Cannot reset Jui while rendering!");

        deferredCalls.clear();
        for (int i = 0; i < SCISSOR_STACK_CAP; i++)     scissorStack[i] = new Rectanglei();
        for (int i = 0; i < CONTAINER_STACK_CAP; i++) containerStack[i] = new Container();
        containerStackDepth = 0;
        scissorStackDepth   = 0;
        idStackDepth        = 0;

        frameCounter        = 0;
        drawCalls           = 0;
        drawCallsHigh       = 0;
        charsRendered       = 0;
        spritesRendered     = 0;
        charsRenderedHigh   = 0;
        spritesRenderedHigh = 0;
        deferredCallsMax    = 0;

        hoveredID           = NULL;
        pressedID           = NULL;
        selectedID          = NULL;
        draggedID           = NULL;
        focusedID           = NULL;
        focusRequest        = NULL;

        lastHoveredID       = NULL;
        lastPressedID       = NULL;
        lastDraggedID       = NULL;
        lastFocusedID       = NULL;

        activeMouseBtn      = MOUSE_NONE;
        lastActiveMouseBtn  = MOUSE_NONE;
        navigationBtn       = NULL;

        hoveredDurationNS  = 0L;
        pressedDurationNS  = 0L;
        focusedDurationNS  = 0L;

        mousePositionX      = 0.0f;
        mousePositionY      = 0.0f;
        mousePressOriginX   = 0.0f;
        mousePressOriginY   = 0.0f;
        mouseFrameDeltaX    = 0.0f;
        mouseFrameDeltaY    = 0.0f;
        mouseDragVectorX    = 0.0f;
        mouseDragVectorY    = 0.0f;
        mouseLastFrameX     = 0.0f;
        mouseLastFrameY     = 0.0f;

        rendering           = false;
        rendererPaused      = false;
        idBufferEnabled     = true;
        deferredState       = false;
        currentBatch        = SPRITE_BATCH;

        persistentStorage.clear();

        framebuffer.bindDraw();
        framebuffer.drawbuffers(0,1);
        framebuffer.clearColorUint(1, NULL);
    }


    public final void drawSpriteSink(Texture texture, float x1, float y1, float x2, float y2, float u, float v, float u2, float v2,
        Color color, float glow, float rot, int id, boolean transparentID, boolean pixelAAA) {
        if (!rendering || rendererPaused) throw new IllegalStateException("gui is paused or not in a rendering state!");
        if (deferredState) deferredCalls.spriteCall().set(texture,x1,y1,x2,y2,u,v,u2,v2,color,glow,rot,id,transparentID,pixelAAA);
        else { useSpriteBatch(id != NULL);
            spriteBatch.push(texture,x1,y1,x2,y2,u,v,u2,v2,color,glow,rot,id,transparentID,pixelAAA);
        }
    }
    public void drawLabel(CharSequence text, float penX, float penY, int font, int size, Color color, float glow, boolean outlined) {
        if (!rendering || rendererPaused) throw new IllegalStateException("gui is paused or not in a rendering state!");
        Objects.checkIndex(font,FontLibrary.MAX_FONT_SLOTS);
        if (text.isEmpty() || size <= 0) return;
        GlyphStream out;
        if (deferredState) out = deferredCalls.textCall();
        else { out = textBatch;
            useTextBatch();
        } FontUtils.streamLabel(toText(text),penX,penY,fontGetBound(font),font,size,color,glow,outlined,out);
    }
    public void drawLabel(CharSequence text, Rectanglef bounds, int font, int size, Color color, float glow, boolean outlined, TextAlignment alignment) {
        if (!rendering || rendererPaused) throw new IllegalStateException("gui is paused or not in a rendering state!");
        Objects.checkIndex(font,FontLibrary.MAX_FONT_SLOTS);
        if (text.isEmpty() || size <= 0) return;
        GlyphStream out;
        if (deferredState) out = deferredCalls.textCall();
        else { out = textBatch;
            useTextBatch();
        } FontUtils.streamLabel(toText(text),bounds,fontGetBound(font),font,size,color,glow,outlined,alignment,out);
    }
    public void drawLabelInt(int value, float penX, float penY, int font, int size, Color color, float glow, boolean outlined) {
        if (!rendering || rendererPaused) throw new IllegalStateException("gui is paused or not in a rendering state!");
        Objects.checkIndex(font,FontLibrary.MAX_FONT_SLOTS);
        if (size <= 0) return;
        GlyphStream out;
        if (deferredState) out = deferredCalls.textCall();
        else { out = textBatch;
            useTextBatch();
        } textBlockInternal.setInt(value);
        FontUtils.streamLabel(textBlockInternal,penX,penY,fontGetBound(font),font,size,color,glow,outlined,out);
    }
    public void drawLabelInt(int value, Rectanglef bounds, int font, int size, Color color, float glow, boolean outlined, TextAlignment alignment) {
        if (!rendering || rendererPaused) throw new IllegalStateException("gui is paused or not in a rendering state!");
        Objects.checkIndex(font,FontLibrary.MAX_FONT_SLOTS);
        if (size <= 0) return;
        GlyphStream out;
        if (deferredState) out = deferredCalls.textCall();
        else { out = textBatch;
            useTextBatch();
        } textBlockInternal.setInt(value);
        FontUtils.streamLabel(textBlockInternal,bounds,fontGetBound(font),font,size,color,glow,outlined,alignment,out);
    }
    public void drawLabelFloat(double value, float penX, float penY, int font, int size, Color color, float glow, boolean outlined) {
        if (!rendering || rendererPaused) throw new IllegalStateException("gui is paused or not in a rendering state!");
        Objects.checkIndex(font,FontLibrary.MAX_FONT_SLOTS);
        if (size <= 0) return;
        GlyphStream out;
        if (deferredState) out = deferredCalls.textCall();
        else { out = textBatch;
            useTextBatch();
        } textBlockInternal.setFloat(value,2);
        FontUtils.streamLabel(textBlockInternal,penX,penY,fontGetBound(font),font,size,color,glow,outlined,out);
    }
    public void drawLabelFloat(double value, Rectanglef bounds, int font, int size, Color color, float glow, boolean outlined, TextAlignment alignment) {
        if (!rendering || rendererPaused) throw new IllegalStateException("gui is paused or not in a rendering state!");
        Objects.checkIndex(font,FontLibrary.MAX_FONT_SLOTS);
        if (size <= 0) return;
        GlyphStream out;
        if (deferredState) out = deferredCalls.textCall();
        else { out = textBatch;
            useTextBatch();
        } textBlockInternal.setFloat(value,2);
        FontUtils.streamLabel(textBlockInternal,bounds,fontGetBound(font),font,size,color,glow,outlined,alignment,out);
    }
    public void drawText(Text text, float penX, float penY, int font, int size, Color color, float glow, boolean outlined) {
        if (!rendering || rendererPaused) throw new IllegalStateException("gui is paused or not in a rendering state!");
        Objects.checkIndex(font,FontLibrary.MAX_FONT_SLOTS);
        if (text.isEmpty() || size <= 0) return;
        GlyphStream out;
        if (deferredState) out = deferredCalls.textCall();
        else { out = textBatch;
            useTextBatch();
        } FontUtils.streamText(text,penX,penY,fontGetBound(font),font,size,color,glow,outlined,out);
    }
    public void drawText(Text text, Rectanglef bounds, int font, int size, Color color,
                         float glow, boolean outlined, boolean wordWrap) {

    }


    public int resolutionWidth() { return framebuffer.width(); }
    public int resolutionHeight() { return framebuffer.height(); }

    public int debugDrawCalls() { return drawCallsHigh; }
    public int debugSpritesRendered() { return spritesRenderedHigh; }
    public int debugCharsRendered() { return charsRenderedHigh; }
    public int debugDeferredCallsMax() { return deferredCallsMax; }

    public Font fontGetBound(int index) { return fontLibrary.boundFont(index); }
    public Font fontGetStored(String name) { return fontLibrary.storedFont(name); }
    public List<Font> fontStoredFonts() { return fontLibrary.storedFonts(); }
    public int fontNumStored() { return fontLibrary.numStoredFonts(); }
    public boolean fontIsStored(String name) { return fontLibrary.storedFont(name) != null; }


    public boolean fontAdd(Font font) { return fontLibrary.addFont(font); }
    public boolean fontBind(String name, int index) {
        Font font = fontLibrary.storedFont(name);
        if (font == null) return false;
        Font currentFont = fontLibrary.boundFont(index);
        if (font != currentFont) {
            if (rendering) flushCurrentBatch();
            fontLibrary.bindFont(font.name,index);
            if (rendering) fontLibrary.bindTextures(0);
        } return true;
    }

    private final Rectanglei[] scissorStack = new Rectanglei[SCISSOR_STACK_CAP];
    private int scissorStackDepth = 0;
    public void scissorPush(float x1, float y1, float x2, float y2) {
        if (!rendering || rendererPaused) throw new IllegalStateException("can't push scissors while renderer is paused or not rendering!");
        if (scissorStackDepth >= SCISSOR_STACK_CAP) throw new IllegalStateException("scissor stack overflow!");
        flushCurrentBatch(); // Always flush before state change
        int minX = Math.max((int) Math.floor(Math.min(x1, x2)), 0);
        int minY = Math.max((int) Math.floor(Math.min(y1, y2)), 0);
        int maxX = Math.min((int) Math.ceil(Math.max(x1, x2)), framebuffer.width());
        int maxY = Math.min((int) Math.ceil(Math.max(y1, y2)), framebuffer.height());
        scissorStack[scissorStackDepth++].setMin(minX,minY).setMax(maxX,maxY);
        scissorUseCurrent();
    } public void scissorPop() {
        if (!rendering || rendererPaused) throw new IllegalStateException("can't pop scissors while renderer is paused or not rendering!");
        if (scissorStackDepth <= 0) throw new IllegalStateException("scissor stack underflow!");
        flushCurrentBatch(); // Always flush before state change
        scissorStackDepth--;
        scissorUseCurrent();
    } public int scissorStackSize() { return scissorStackDepth; }
    private void scissorUseCurrent() {
        if (scissorStackDepth <= 0) glDisable(GL_SCISSOR_TEST);
        else { Rectanglei scissor = scissorStack[scissorStackDepth - 1];
            glScissor(scissor.minX, scissor.minY, scissor.lengthX(), scissor.lengthY());
            glEnable(GL_SCISSOR_TEST);
        }
    }


    public void deferredEnable() { deferredState = true; }
    public void deferredDisable() { deferredState = false; }
    public void deferredFlush() {
        if (!rendering || rendererPaused)
            throw new IllegalStateException("can't flush deferred calls while renderer is paused or not rendering!");
        deferredCallsMax = Math.max(deferredCallsMax,deferredCalls.size());
        for (DeferredCalls.DeferredCall call : deferredCalls) {
            if (call instanceof DeferredCalls.DeferredSprite s) {
                useSpriteBatch(s.id != NULL);
                spriteBatch.push(s.texture,s.x1,s.y1,s.x2,s.y2,s.u1,s.v1,s.u2,s.v2,s.color,s.glow,s.rot,s.id,s.transparentID,s.pixelAAA);
            } else if (call instanceof DeferredCalls.DeferredText t) {
                int numGlyphs = t.count / 4;
                if (numGlyphs > 0) {
                    useTextBatch();
                    textBatch.put(t.vertices, 0, numGlyphs);
                }
            }
        }
    }


    public int currentHoveredID() { return hoveredID; }
    public int currentPressedID() { return pressedID; }
    public int currentDraggedID() { return draggedID; }
    public int currentSelectedID() { return selectedID; }
    public int currentFocusedID() { return focusedID; }
    public int lastFrameHoveredID() { return lastHoveredID; }
    public int lastFramePressedID() { return lastPressedID; }
    public int lastFrameDraggedID() { return lastDraggedID; }
    public int lastFrameFocusedID() { return lastFocusedID; }
    public int navigationBtn() { return navigationBtn; }
    public int mouseActiveBtn() { return activeMouseBtn; }
    public int mouseLastActiveBtn() { return lastActiveMouseBtn; }
    public float hoveredDuration() { return (float) (hoveredDurationNS / 1_000_000_000d); }
    public float pressedDuration() { return (float) (pressedDurationNS / 1_000_000_000d); }
    public float focusedDuration() { return (float) (focusedDurationNS / 1_000_000_000d); }
    public float mousePosX() { return mousePositionX; }
    public float mousePosY() { return mousePositionY; }
    public float mousePressOriginX() { return mousePressOriginX; }
    public float mousePressOriginY() { return mousePressOriginY; }
    public float mouseFrameDeltaX() { return mouseFrameDeltaX; }
    public float mouseFrameDeltaY() { return mouseFrameDeltaY; }
    public float mouseDragVectorX() { return mouseDragVectorX; }
    public float mouseDragVectorY() { return mouseDragVectorY; }
    public void focusSteal(int id) { focusRequest = id; }
    public void focusYield() { focusRequest = NULL; }


    // =============================================================================
    // ID STACK / HASHING
    // =============================================================================
    final int[] idStack = new int[ID_STACK_CAP];
    int idStackDepth = 0;
    public int scopeID() {
        return idStackDepth == 0 ? FNV_OFFSET_32 : idStack[idStackDepth - 1];
    } public int pushID(String scope) {
        if (idStackDepth >= ID_STACK_CAP) {
            throw new IllegalStateException("id stack overflow!");
        } int newScope = JuiStateAPI.hash(scope, scopeID());
        idStack[idStackDepth++] = newScope;
        return newScope;
    } public int pushID(int scope) {
        if (idStackDepth >= ID_STACK_CAP) {
            throw new IllegalStateException("id stack overflow!");
        } int newScope = JuiStateAPI.hash(scope, scopeID());
        idStack[idStackDepth++] = newScope;
        return newScope;
    } public void popID() {
        if (idStackDepth == 0) {
            throw new IllegalStateException("id stack underflow!");
        } idStackDepth--;
    }

    // =============================================================================
    // CORE LAYOUT
    // =============================================================================
    private final Rectanglef contBounds = new Rectanglef();
    private final Container[] containerStack = new Container[CONTAINER_STACK_CAP];
    private int containerStackDepth;
    public int containerStackDepth() { return containerStackDepth; }
    public Container currentContainer() { return containerStackDepth == 0 ? null : containerStack[containerStackDepth - 1]; }
    public Container containerRoot() { return containerStackDepth == 0 ? null : containerStack[0];}
    private Container peekContainerUnchecked() { return containerStack[containerStackDepth - 1]; }
    private Container pushContainerInternal() {
        if (containerStackDepth >= CONTAINER_STACK_CAP) {
            throw new IllegalStateException("container stack overflow!");
        } return containerStack[containerStackDepth++];
    } private Container popContainerInternal() {
        if (containerStackDepth == 0) {
            throw new IllegalStateException("container stack underflow!");
        } return containerStack[--containerStackDepth];
    } public Rectanglef allocateSpace(float size, Rectanglef dst) {
        if (containerStackEmpty()) throw new IllegalStateException("empty container stack alloc!");
        if (size < 0.0f) throw new IllegalArgumentException("container negative size alloc!");
        return peekContainerUnchecked().allocate(size,dst);
    } public Rectanglef allocateRemaining(Rectanglef dst) {
        if (containerStackEmpty()) throw new IllegalStateException("empty container stack alloc!");
        Container container = peekContainerUnchecked();
        return container.allocate(container.availableSpace(),dst);
    } public void pushContainerAbsolute(float x, float y, float w, float h, float spacing, Axis axis, boolean inverseLayout) {
        if (spacing < 0.0f) throw new IllegalArgumentException("negative container spacing request!");
        if (w < 0.0f || h < 0.0f) throw new IllegalArgumentException("negative container area request!");
        pushContainerInternal().init(x,y,w,h,spacing,axis,inverseLayout, Container.SIZE_ABSOLUTE);
    } public void pushContainerFixed(float size, float spacing, Axis axis, boolean inverseLayout) {
        if (containerStackEmpty()) throw new IllegalStateException("no parent container on stack!");
        if (spacing < 0.0f) throw new IllegalArgumentException("negative container spacing request!");
        if (size < 0.0f) throw new IllegalArgumentException("negative container size request!");
        peekContainerUnchecked().computePlacement(size, contBounds);
        pushContainerInternal().init(contBounds.minX, contBounds.minY,
                contBounds.lengthX(), contBounds.lengthY(), spacing, axis, inverseLayout, size);
    } public void pushContainerRemaining(float spacing, Axis axis, boolean inverseLayout) {
        if (containerStackEmpty()) throw new IllegalStateException("no parent container on stack!");
        pushContainerFixed(peekContainerUnchecked().availableSpace(), spacing, axis, inverseLayout);
    } public void pushContainerAuto(float spacing, Axis axis, boolean inverseLayout) {
        if (containerStackEmpty()) throw new IllegalStateException("no parent container on stack!");
        if (spacing < 0.0f) throw new IllegalArgumentException("negative container spacing request!");
        Container parent = peekContainerUnchecked();
        parent.computePlacement(parent.availableSpace(), contBounds);
        pushContainerInternal().init(contBounds.minX, contBounds.minY,
                contBounds.lengthX(), contBounds.lengthY(), spacing, axis, inverseLayout, Container.SIZE_AUTO);
    } public Container popContainer() {
        Container container = popContainerInternal();
        if (!containerStackEmpty() && !container.isAbsolute()) {
            Container parent = peekContainerUnchecked();
            float spaceToCommit = (container.requestedSize == Container.SIZE_AUTO )
                    ? container.contentSize()
                    : container.requestedSize;
            if (spaceToCommit > 0.0f) {
                if (parent.itemCount > 0) parent.offset += parent.spacing;
                parent.offset += spaceToCommit;
                parent.itemCount++;
            }
        } return container;
    }

    // =============================================================================
    // PERSISTENT INTERNAL STORAGE
    // =============================================================================
    private final Map<Integer, Object> persistentStorage = HashMap.newHashMap(1024);
    private void logCollision(int id, Class<?> expected, Class<?> found) {
        Logger.warn("Persistent object collision [ID: {}]! Expected {}, found {}. Overwriting state.",
                id, expected.getSimpleName(), found.getSimpleName());
    } public void persistentPut(int id, Object object) { persistentStorage.put(id, object); }
    public void persistentRemove(int id) { persistentStorage.remove(id); }
    public void persistentClear() { persistentStorage.clear(); }
    public boolean persistentContains(int id) { return persistentStorage.containsKey(id); }
    @SuppressWarnings("unchecked")
    public <T> T getObj(int id, Class<T> clazz, Supplier<T> supplier) {
        Object val = persistentStorage.get(id);
        if (clazz.isInstance(val)) return (T) val;
        if (val != null) logCollision(id, clazz, val.getClass());
        T newInstance = supplier.get();
        persistentStorage.put(id, newInstance);
        return newInstance;
    } public int getInt(int id, int defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Integer i) return i;
        if (val != null) logCollision(id, Integer.class, val.getClass());
        persistentStorage.put(id, defaultValue);
        return defaultValue;
    } public long getLong(int id, long defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Long l) return l;
        if (val != null) logCollision(id, Long.class, val.getClass());
        persistentStorage.put(id, defaultValue);
        return defaultValue;
    } public float getFloat(int id, float defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Float f) return f;
        if (val != null) logCollision(id, Float.class, val.getClass());
        persistentStorage.put(id, defaultValue);
        return defaultValue;
    } public double getDouble(int id, double defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Double d) return d;
        if (val != null) logCollision(id, Double.class, val.getClass());
        persistentStorage.put(id, defaultValue);
        return defaultValue;
    } public boolean getBool(int id, boolean defaultValue) {
        Object val = persistentStorage.get(id);
        if (val instanceof Boolean b) return b;
        if (val != null) logCollision(id, Boolean.class, val.getClass());
        persistentStorage.put(id, defaultValue);
        return defaultValue;
    } public Vector2f getVec2f(int id, float x, float y) {
        Object val = persistentStorage.get(id);
        if (val instanceof Vector2f v) return v;
        if (val != null) logCollision(id, Vector2f.class, val.getClass());
        Vector2f copy = new Vector2f(x, y);
        persistentStorage.put(id, copy);
        return copy;
    } public Vector3f getVec3f(int id, float x, float y, float z) {
        Object val = persistentStorage.get(id);
        if (val instanceof Vector3f v) return v;
        if (val != null) logCollision(id, Vector3f.class, val.getClass());
        Vector3f copy = new Vector3f(x, y, z);
        persistentStorage.put(id, copy);
        return copy;
    } public Vector4f getVec4f(int id, float x, float y, float z, float w) {
        Object val = persistentStorage.get(id);
        if (val instanceof Vector4f v) return v;
        if (val != null) logCollision(id, Vector4f.class, val.getClass());
        Vector4f copy = new Vector4f(x, y, z, w);
        persistentStorage.put(id, copy);
        return copy;
    } public Vector2i getVec2i(int id, int x, int y) {
        Object val = persistentStorage.get(id);
        if (val instanceof Vector2i v) return v;
        if (val != null) logCollision(id, Vector2i.class, val.getClass());
        Vector2i copy = new Vector2i(x, y);
        persistentStorage.put(id, copy);
        return copy;
    } public Vector3i getVec3i(int id, int x, int y, int z) {
        Object val = persistentStorage.get(id);
        if (val instanceof Vector3i v) return v;
        if (val != null) logCollision(id, Vector3i.class, val.getClass());
        Vector3i copy = new Vector3i(x, y, z);
        persistentStorage.put(id, copy);
        return copy;
    } public Vector4i getVec4i(int id, int x, int y, int z, int w) {
        Object val = persistentStorage.get(id);
        if (val instanceof Vector4i v) return v;
        if (val != null) logCollision(id, Vector4i.class, val.getClass());
        Vector4i copy = new Vector4i(x, y, z, w);
        persistentStorage.put(id, copy);
        return copy;
    } public Rectanglef getRectf(int id, float minX, float minY, float maxX, float maxY) {
        Object val = persistentStorage.get(id);
        if (val instanceof Rectanglef r) return r;
        if (val != null) logCollision(id, Rectanglef.class, val.getClass());
        Rectanglef copy = new Rectanglef(minX, minY, maxX, maxY);
        persistentStorage.put(id, copy);
        return copy;
    } public Rectanglei getRecti(int id, int minX, int minY, int maxX, int maxY) {
        Object val = persistentStorage.get(id);
        if (val instanceof Rectanglei r) return r;
        if (val != null) logCollision(id, Rectanglei.class, val.getClass());
        Rectanglei copy = new Rectanglei(minX, minY, maxX, maxY);
        persistentStorage.put(id, copy);
        return copy;
    }

    private void useSpriteBatch(boolean enableID) {
        if (currentBatch == SPRITE_BATCH && idBufferEnabled == enableID) return;
       flushCurrentBatch();
       currentBatch = SPRITE_BATCH;
       idBufferEnabled = enableID;
       if (enableID) framebuffer.drawbuffers(0,1);
       else framebuffer.drawbuffer(0);
    }

    private void useTextBatch() {
        if (currentBatch == TEXT_BATCH) return;
        flushCurrentBatch();
        currentBatch = TEXT_BATCH;
        framebuffer.drawbuffer(0);
        idBufferEnabled = false;
    }

    private void flushCurrentBatch() {
        switch (currentBatch) {
            case SPRITE_BATCH -> spriteBatch.flush();
            case TEXT_BATCH -> textBatch.flush();
        }
    }

    private int readIdBuffer() {
        Vector2f mouse = Jgen.get().mouse().position();
        int mouseX = Math.clamp(Math.round(mouse.x),0,framebuffer.width()  - 1);
        int mouseY = Math.clamp(Math.round(mouse.y),0,framebuffer.height() - 1);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            framebuffer.bindRead();
            IntBuffer buf = stack.callocInt(1);
            glReadPixels(mouseX,mouseY,1,1, GL_RED_INTEGER, GL_UNSIGNED_INT,buf);
            return buf.get(0);
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
        // framebuffer.clearColorUint(1, NULL);
        Framebuffer.checkReadStatus();
        Framebuffer.checkDrawStatus();
        return framebuffer;
    }



    private Text toText(CharSequence string) {
        if (string instanceof Text text) return text;
        String str = string == null ? "" : string.toString();
        ManagedText text;
        if (str.length() > textBlockInternal.capacity()) {
            text = textBufferInternal;
        } else text = textBlockInternal;
        text.set(str);
        return text;
    }

    private static final class DeferredCalls implements Iterable<DeferredCalls.DeferredCall> {


        private final Pool<DeferredSprite> deferredSpritePool = Pool.of(DEFERRED_QUEUE_CAP / 4,DEFERRED_QUEUE_CAP,DeferredSprite::new);
        private final Pool<DeferredText> deferredTextPool = Pool.of(DEFERRED_QUEUE_CAP / 4,DEFERRED_QUEUE_CAP,DeferredText::new);
        private final DeferredCall[] deferredQueue = new DeferredCall[DEFERRED_QUEUE_CAP];
        private final ConsumingIterator iterator = new ConsumingIterator();
        private int count;

        DeferredCalls() {
            deferredSpritePool.preFill(deferredSpritePool.capacity());
            deferredTextPool.preFill(deferredTextPool.capacity());
        }

        DeferredSprite spriteCall() {
            if (count == DEFERRED_QUEUE_CAP)
                throw new IllegalStateException("gui deferred draw calls overflow!");
            DeferredSprite sprite = deferredSpritePool.obtain();
            deferredQueue[count++] = sprite;
            return sprite;
        }

        DeferredText textCall() {
            if (count == DEFERRED_QUEUE_CAP)
                throw new IllegalStateException("gui deferred draw calls overflow!");
            DeferredText text = deferredTextPool.obtain();
            deferredQueue[count++] = text;
            return text;
        }

        @SuppressWarnings("all")
        void clear() { for (DeferredCall call : this) { /* intentional */ } }
        int size() { return count; }
        public Iterator<DeferredCall> iterator() {
            iterator.reset();
            return iterator;
        }

        private final class ConsumingIterator implements Iterator<DeferredCall> {
            private int index;
            private DeferredCall lastReturned;
            void reset() { index = 0;
                lastReturned = null;
            } public boolean hasNext() {
                if (index >= count) {
                    if (lastReturned != null) {
                        free(lastReturned);
                        lastReturned = null;
                    } count = 0;
                    return false;
                } return true;

            }
            public DeferredCall next() {
                if (index >= count) throw new NoSuchElementException();
                if (lastReturned != null) free(lastReturned);
                lastReturned = deferredQueue[index];
                deferredQueue[index++] = null;
                return lastReturned;
            }

            private void free(DeferredCall call) {
                if (call instanceof DeferredSprite s) {
                    deferredSpritePool.free(s);
                } else if (call instanceof DeferredText t) {
                    deferredTextPool.free(t);
                }
            }
        }

        interface DeferredCall extends Pool.Poolable{ }

        static final class DeferredSprite implements DeferredCall {
            Texture texture; Color color; int id;
            float x1, y1, x2, y2;
            float u1, v1, u2, v2;
            float glow, rot;
            boolean transparentID; boolean pixelAAA;
            public void set(Texture texture, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2,
                            Color color, float glow, float rot, int id, boolean transparentID, boolean pixelAAA) {
                this.texture = texture; this.color = color; this.id = id; this.glow = glow; this.rot = rot;
                this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
                this.u1 = u1; this.v1 = v1; this.u2 = u2; this.v2 = v2;
                this.transparentID = transparentID; this.pixelAAA = pixelAAA;
            } public void poolableOnFree() { texture = null; }
        }

        static final class DeferredText implements DeferredCall, GlyphStream {
            int count; float[] vertices = new float[256]; // multiple of 4
            public void put(float penX, float penY, float data, float color) {
                if (count == vertices.length) {
                    float[] copy = new float[vertices.length * 2];
                    System.arraycopy(vertices,0,copy,0,count);
                    vertices = copy; }
                vertices[count++] = penX;
                vertices[count++] = penY;
                vertices[count++] = data;
                vertices[count++] = color;
            } public void poolableOnFree() { count = 0; }
        }
    }



    static final class TextBatch implements Disposable, GlyphStream {
        /* ------------------------------
         * vertex: | pos | color | data |
         * ------------------------------
         * size:   | 2   | 1     | 1    |
         * ------------------------------*/
        public static final String TEXT_PROGRAM_NAME = "jgen-gui-text";
        public static final String TEXT_PROGRAM_DIR = "jgen/gui/glsl";
        public static final int VERTEX_SIZE_FLOAT = 4;
        public static final int BATCH_SIZE_FLOAT = TEXT_BATCH_CAP * VERTEX_SIZE_FLOAT;
        public static final int BATCH_SIZE_BYTES = BATCH_SIZE_FLOAT * Float.BYTES;
        private final FontLibrary fontStore;
        private final ShaderProgram program;
        private final FloatBuffer vertices;
        private final int vao;
        private final int vbo;
        private int count;
        private int countAccum;
        private int drawCalls;

        TextBatch(FontLibrary fontLibrary, int width, int height) throws Exception {
            this.fontStore = fontLibrary;
            ShaderProgram program = ShaderProgram.getProgramByName(TEXT_PROGRAM_NAME);
            if (program == null) {
                Shader shader = Disk.resourceShader(TEXT_PROGRAM_NAME, TEXT_PROGRAM_DIR);
                program = new ShaderProgram(shader);
            } this.program = program;
            program.use();
            ShaderProgram.setUniformF("uResolution",width,height);
            int[] samplers = new int[FontLibrary.MAX_FONT_SLOTS];
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

        public void put(float penX, float penY, float fData, float fColor) {
            if (count == TEXT_BATCH_CAP) flush();
            vertices.put(penX).put(penY).put(fData).put(fColor);
            count++;
        }

        void put(float[] src, int srcOffset, int numGlyphs) {
            int remaining = numGlyphs;
            int offset = srcOffset;
            while (remaining > 0) {
                int space = TEXT_BATCH_CAP - count;
                if (space == 0) { flush();
                    space = TEXT_BATCH_CAP;
                } int toCopy = Math.min(remaining, space);
                int lenFloat = toCopy * VERTEX_SIZE_FLOAT;
                vertices.put(src, offset, lenFloat);
                offset += lenFloat;
                remaining -= toCopy;
                count += toCopy;
            }
        }

        int resetDrawCalls() {
            int tmp = drawCalls;
            drawCalls = 0;
            return tmp;
        }

        int resetCountAccumulator() {
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

    static final class SpriteBatch implements Disposable {
        /* ----------------------------------------
         * vertex: | pos | uv | color | id | data |
         * ----------------------------------------
         * size:   | 2   | 2  | 1     | 1  | 1    |
         * ----------------------------------------*/
        public static final String SPRITE_PROGRAM_NAME = "jgen-gui-sprite";
        public static final String SPRITE_PROGRAM_DIR = "jgen/gui/glsl";
        public static final int VERTEX_SIZE_FLOAT = 7;
        public static final int VERTEX_SIZE_BYTES = VERTEX_SIZE_FLOAT * Float.BYTES;
        public static final int SPRITE_SIZE_FLOAT = VERTEX_SIZE_FLOAT * 4;
        public static final int BATCH_SIZE_FLOAT = SPRITE_BATCH_CAP * SPRITE_SIZE_FLOAT;
        public static final int BATCH_SIZE_BYTES = BATCH_SIZE_FLOAT * Float.BYTES;
        public static final int TEXTURE_UNIT_OFFSET = FontLibrary.MAX_FONT_SLOTS;
        public static final int TEXTURE_SLOTS = 8; // same as in the shader

        private final Texture[] textureSlots = new Texture[TEXTURE_SLOTS];
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
            ebo = Buffers.generateBindQuadEBO(SPRITE_BATCH_CAP);
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
                vertices.flip();
                Buffers.uploadVBO(vertices);
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

        int resetCountAccumulator() {
            int tmp = countAccum;
            countAccum = 0;
            return tmp;
        }

        void onResize(int width, int height) {
            program.use();
            ShaderProgram.setUniformF("uResolution",width,height);
        }

        void push(Texture texture, float x1, float y1, float x2, float y2, float u1, float v1, float u2, float v2,
                  Color color, float glow, float rot, int id, boolean transparentID, boolean pixelAAA) {
            if (count == SPRITE_BATCH_CAP) flush();
            final float fData = packData(texture, glow, transparentID, pixelAAA);
            final float fColor = (color == null) ? Color.WHITE.packedFormat() : color.packedFormat();
            final float fID = Float.intBitsToFloat(id);
            if (rot == 0) put(x1, y1, x2, y2, u1, v1, u2, v2, fColor, fData, fID);
            else put(x1, y1, x2, y2, u1, v1, u2, v2, fColor, fData, fID, rot);
        }

        private float packData(Texture texture, float glow, boolean transparentID, boolean pixelAAA) {
            int data = resolveTextureSlot(texture);
            data |= (glow > 0 ? ((int) (65535f * Math.min(glow, 1.0f))) << 4 : 0);
            data |= (pixelAAA       ? (1 << 20) : 0);
            data |= (transparentID  ? (1 << 21) : 0);
            return Float.intBitsToFloat(data);
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


}
