package io.github.nascentlogic.jgen.gui.api;

import io.github.nascentlogic.jgen.gfx.Color;
import io.github.nascentlogic.jgen.gfx.Texture;
import io.github.nascentlogic.jgen.gui.Font;
import io.github.nascentlogic.jgen.gui.TextLayout;
import io.github.nascentlogic.jgen.gui.text.Text;
import io.github.nascentlogic.jgen.utils.Disposable;
import org.joml.Vector4f;
import org.joml.primitives.Rectanglef;
import org.joml.primitives.Rectanglei;

import java.util.List;

/**
 * F.Dahl, 10/3/2026
 */
public interface JuiGFX extends Disposable {


    int resolutionWidth();
    int resolutionHeight();

    int infoDrawCalls();
    int infoSpritesRendered();
    int infoCharsRendered();
    int infoDeferredCallsMax();

    /** Calculate layout*/
    void textLayout(Text text, Rectanglef bounds, int font, int size, boolean wrap, TextLayout dst);
    /** Height of free text (no-wrap) */
    float textHeight(Text text, int font, int size);

    /** Adds a font to stored fonts if no font already exist with the same name.
     * @return false if a font already exist under the same name.
     * Added Fonts are freed automaically on gui exit */
    boolean fontAdd(Font font);
    /** Binds a stored font for use. Will flush the current batch.
     * @param index 0 to 4
     * @param name name of the font (file name without the .ttf extension)
     * @return true if the font is a stored font (and therefore was bound)*/
    boolean fontBind(String name, int index);
    /** @param name name of the font (file name without the .ttf extension)
     * @return true if font is stored in library */
    boolean fontIsStored(String name);
    /** Returns the currently bound font for index.
     * By default it's the GUI default font.
     * @param index 0 to 4
     * @return bound font (never null)  */
    Font fontGetBound(int index);
    /** @param name name of the font (file name without the .ttf extension)
     * @return null if font by name does not exist in library */
    Font fontGetStored(String name);
    /** @return a list of all fonts added to library */
    List<Font> fontStoredFonts();
    /** Number of fonts in library */
    int fontNumStored();

    /** Push a glScissor rectangle on the stack. Geometry will be culled outside it's bounds.
     * Does not take previously pushed rectangles into accont (no intersection with previous). */
    void scissorPush(float x1, float y1, float x2, float y2);
    /** @see #scissorPush(float, float, float, float) */
    default void scissorPush(Rectanglei scissor) {
        scissorPush(scissor.minX,scissor.minY,scissor.maxX,scissor.maxY);
    } /** @see #scissorPush(float, float, float, float) */
    default void scissorPush(Rectanglef scissor) {
        scissorPush(scissor.minX,scissor.minY,scissor.maxX,scissor.maxY);
    } /** Pop the current scissor rectangle on the stack. If the pppped ractangle was the last,
     * GL_SCISSOR_TEST is disabled */
    void scissorPop();
    /** Number of scissor rectangles currently on the stack */
    int scissorStackSize();


    /** Enables deferred rendering.
     * Future draw calls are queued instead of rendered immediatly.
     * Calling: {@link #deferredFlush()} will flush the current commands
     * to the appropriate batches. Call {@link #deferredDisable()} to return
     * to "immediate mode" rendering.<p>
     * Note: disable will not flush the deferred calls. */
    void deferredEnable();
    /** {@link #deferredEnable()} */
    void deferredDisable();
    /** {@link #deferredEnable()} */
    void deferredFlush();



    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRect(Rectanglef rect, Color color) { drawRectRot(rect,color,0,0,0,true); }
    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRect(Rectanglef rect, Color color, int id) { drawRectRot(rect,color,0,0,id,true); }
    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRect(Rectanglef rect, Color color, float glow, int id) { drawRectRot(rect,color,glow,0,id,true); }
    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRect(Rectanglef rect, Color color, float glow, int id, boolean transparentID) { drawRectRot(rect,color,glow,0,id,transparentID); }
    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRectRot(Rectanglef rect, Color color, float rot) { drawRectRot(rect,color,0,rot,0,true); }
    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRectRot(Rectanglef rect, Color color, float rot, int id) { drawRectRot(rect,color,0,rot,id,true); }
    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRectRot(Rectanglef rect, Color color, float glow, float rot, int id) { drawRectRot(rect,color,glow,rot,id,true); }
    /**
     * Rendeer a colored quad in screen space.
     * @param rect sprite transform on screen
     * @param color linear rgba tint of sprite
     * @param glow normalized strength of the color (0: color, 1: color + color + glow * MAX_GLOW)
     * @param rot rotation in radians of the sprite around it's center (0 for no rotation)
     * @param id 32-bit id associated with the sprite
     * @param transparentID if true the sprite will output the id even if sprite is stansparent (default == true)
     */
    default void drawRectRot(Rectanglef rect, Color color, float glow, float rot, int id, boolean transparentID) {
        drawSpriteSuper(null,rect.minX,rect.minY,rect.maxX,rect.maxY,0,0,1,1,color,glow,rot,id,transparentID,false);
    }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRect(float x, float y, float w, float h, Color color) { drawRectRot(x,y,w,h,color,0,0,0,true); }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRect(float x, float y, float w, float h, Color color, int id) { drawRectRot(x,y,w,h,color,0,0,id,true); }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRect(float x, float y, float w, float h, Color color, float glow, int id) { drawRectRot(x,y,w,h,color,glow,0,id,true); }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRect(float x, float y, float w, float h, Color color, float glow, int id, boolean transparentID) { drawRectRot(x,y,w,h,color,glow,0,id,transparentID); }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRectRot(float x, float y, float w, float h, Color color, float rot) { drawRectRot(x,y,w,h,color,0,rot,0,true); }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRectRot(float x, float y, float w, float h, Color color, float rot, int id) { drawRectRot(x,y,w,h,color,0,rot,id,true); }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRectRot(float x, float y, float w, float h, Color color, float glow, float rot, int id) { drawRectRot(x,y,w,h,color,glow,rot,id,true); }
    /**
     * Rendeer a colored quad in screen space.
     * @param x sprite botom left position on screen
     * @param y sprite botom left position on screen
     * @param w sprite width in screen pixels
     * @param h sprite height in screen pixels
     * @param color linear rgba tint of sprite
     * @param glow normalized strength of the color (0: color, 1: color + color + glow * MAX_GLOW)
     * @param rot rotation in radians of the sprite around it's center (0 for no rotation)
     * @param id 32-bit id associated with the sprite
     * @param transparentID if true the sprite will output the id even if sprite is stansparent (default == true)
     */
    default void drawRectRot(float x, float y, float w, float h, Color color, float glow, float rot, int id, boolean transparentID) {
        drawSpriteSuper(null,x,y,x + w, y + h,0,0,1,1, color, glow, rot,  id, transparentID, false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, Rectanglef rect, Vector4f uv, float rot) {
        drawSpriteRot(texture,rect,uv.x,uv.y,uv.z,uv.w,Color.WHITE,0,rot,0,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, Rectanglef rect, Vector4f uv, float rot, int id) {
        drawSpriteRot(texture,rect,uv.x,uv.y,uv.z,uv.w,Color.WHITE,0,rot,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, Rectanglef rect, Vector4f uv, Color color, float rot, int id) {
        drawSpriteRot(texture,rect,uv.x,uv.y,uv.z,uv.w,color,0,rot,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, Rectanglef rect, Vector4f uv, Color color, float glow, float rot, int id) {
        drawSpriteRot(texture,rect,uv.x,uv.y,uv.z,uv.w,color,glow,rot,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, Rectanglef rect, Vector4f uv, Color color, float glow, float rot, int id, boolean transparentID) {
        drawSpriteRot(texture,rect,uv.x,uv.y,uv.z,uv.w,color,glow,rot,id,transparentID,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, Rectanglef rect, float rot) {
        drawSpriteRot(texture,rect,0,0,1,1,Color.WHITE,0,rot,0,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, Rectanglef rect, float u, float v, float u2, float v2, float rot) {
        drawSpriteRot(texture,rect,u,v,u2,v2,Color.WHITE,0,rot,0,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, Rectanglef rect, float u, float v, float u2, float v2, float rot, int id) {
        drawSpriteRot(texture,rect,u,v,u2,v2,Color.WHITE,0,rot,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, Rectanglef rect, float u, float v, float u2, float v2, Color color, float rot, int id) {
        drawSpriteRot(texture,rect,u,v,u2,v2,color,0,rot,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, Rectanglef rect, float u, float v, float u2, float v2, Color color, float glow, float rot, int id) {
        drawSpriteRot(texture,rect,u,v,u2,v2,color,glow,rot,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, Rectanglef rect, float u, float v, float u2, float v2, Color color, float glow, float rot, int id, boolean transparentID) {
        drawSpriteRot(texture,rect,u,v,u2,v2,color,glow,rot,id,transparentID,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, Rectanglef rect, Vector4f uv) {
        drawSpriteRot(texture,rect,uv.x,uv.y,uv.z,uv.w,Color.WHITE,0,0,0,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, Rectanglef rect, Vector4f uv, int id) {
        drawSpriteRot(texture,rect,uv.x,uv.y,uv.z,uv.w,Color.WHITE,0,0,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, Rectanglef rect,Vector4f uv, Color color, int id) {
        drawSpriteRot(texture,rect,uv.x,uv.y,uv.z,uv.w,color,0,0,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, Rectanglef rect, Vector4f uv, Color color, float glow, int id) {
        drawSpriteRot(texture,rect,uv.x,uv.y,uv.z,uv.w,color,glow,0,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, Rectanglef rect, Vector4f uv, Color color, float glow, int id, boolean transparentID) {
        drawSpriteRot(texture,rect,uv.x,uv.y,uv.z,uv.w,color,glow,0,id,transparentID,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, Rectanglef rect) {
        drawSpriteRot(texture,rect,0,0,1,1,Color.WHITE,0,0,0,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, Rectanglef rect, float u, float v, float u2, float v2) {
        drawSpriteRot(texture,rect,u,v,u2,v2,Color.WHITE,0,0,0,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, Rectanglef rect, float u, float v, float u2, float v2, int id) {
        drawSpriteRot(texture,rect,u,v,u2,v2,Color.WHITE,0,0,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, Rectanglef rect,float u, float v, float u2, float v2, Color color, int id) {
        drawSpriteRot(texture,rect,u,v,u2,v2,color,0,0,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, Rectanglef rect, float u, float v, float u2, float v2, Color color, float glow, int id) {
        drawSpriteRot(texture,rect,u,v,u2,v2,color,glow,0,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, Rectanglef rect, float u, float v, float u2, float v2, Color color, float glow, int id, boolean transparentID) {
        drawSpriteRot(texture,rect,u,v,u2,v2,color,glow,0,id,transparentID,false);
    }
    /** {@link #drawSpriteRot(Texture, Rectanglef, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, Rectanglef rect, float u, float v, float u2, float v2, Color color, float glow, int id, boolean transparentID, boolean pixelAAA) {
        drawSpriteRot(texture,rect,u,v,u2,v2,color,glow,0,id,transparentID,pixelAAA);
    }
    /**
     * Rendeer a textured quad in screen space.
     * @param texture texture or null
     * @param rect sprite transform on screen
     * @param u texture u - left uv coordinate of texture region
     * @param v texture v - top uv coordinate of texture region
     * @param u2 texture u2 - right uv coordinate of texture region
     * @param v2 texture v2 - bottom uv coordinate of texture region
     * @param color linear rgba tint of sprite
     * @param glow normalized strength of the color (0: color, 1: color + color + glow * MAX_GLOW)
     * @param rot rotation in radians of the sprite around it's center (0 for no rotation)
     * @param id 32-bit id associated with the sprite
     * @param transparentID if true the sprite will output the id even if sprite is stansparent (default == true)
     * @param pixelAAA Pixel-Art Anti-Aliasing. Useful for reendering upscaled "pixelated" sprites.
     *                Only works with bi-linear texture sampling (default == false)
     */
    default void drawSpriteRot(Texture texture, Rectanglef rect, float u, float v, float u2, float v2, Color color, float glow, float rot, int id, boolean transparentID, boolean pixelAAA) {
        drawSpriteSuper(texture,rect.minX,rect.minY,rect.maxX,rect.maxY,u,v,u2,v2,color,glow,rot,id,transparentID,pixelAAA);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, float x, float y, float w, float h, Vector4f uv, float rot) {
        drawSpriteRot(texture,x,y,w,h,uv.x,uv.y,uv.z,uv.w,Color.WHITE,0,rot,0,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, float x, float y, float w, float h, Vector4f uv, float rot, int id) {
        drawSpriteRot(texture,x,y,w,h,uv.x,uv.y,uv.z,uv.w,Color.WHITE,0,rot,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, float x, float y, float w, float h, Vector4f uv, Color color, float rot, int id) {
        drawSpriteRot(texture,x,y,w,h,uv.x,uv.y,uv.z,uv.w,color,0,rot,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, float x, float y, float w, float h, Vector4f uv, Color color, float glow, float rot, int id) {
        drawSpriteRot(texture,x,y,w,h,uv.x,uv.y,uv.z,uv.w,color,glow,rot,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, float x, float y, float w, float h, Vector4f uv, Color color, float glow, float rot, int id, boolean transparentID) {
        drawSpriteRot(texture,x,y,w,h,uv.x,uv.y,uv.z,uv.w,color,glow,rot,id,transparentID,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, float x, float y, float w, float h, float u, float v, float u2, float v2, float rot) {
        drawSpriteRot(texture,x,y,w,h,u,v,u2,v2,Color.WHITE,0,rot,0,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, float x, float y, float w, float h, float u, float v, float u2, float v2, float rot, int id) {
        drawSpriteRot(texture,x,y,w,h,u,v,u2,v2,Color.WHITE,0,rot,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, float x, float y, float w, float h, float u, float v, float u2, float v2, Color color, float rot, int id) {
        drawSpriteRot(texture,x,y,w,h,u,v,u2,v2,color,0,rot,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, float x, float y, float w, float h, float u, float v, float u2, float v2, Color color, float glow, float rot, int id) {
        drawSpriteRot(texture,x,y,w,h,u,v,u2,v2,color,glow,rot,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSpriteRot(Texture texture, float x, float y, float w, float h, float u, float v, float u2, float v2, Color color, float glow, float rot, int id, boolean transparentID) {
        drawSpriteRot(texture,x,y,w,h,u,v,u2,v2,color,glow,rot,id,transparentID,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, float x, float y, float w, float h, Vector4f uv) {
        drawSpriteRot(texture,x,y,w,h,uv.x,uv.y,uv.z,uv.w,Color.WHITE,0,0,0,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, float x, float y, float w, float h, Vector4f uv, int id) {
        drawSpriteRot(texture,x,y,w,h,uv.x,uv.y,uv.z,uv.w,Color.WHITE,0,0,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, float x, float y, float w, float h, Vector4f uv, Color color, int id) {
        drawSpriteRot(texture,x,y,w,h,uv.x,uv.y,uv.z,uv.w,color,0,0,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, float x, float y, float w, float h, Vector4f uv, Color color, float glow, int id) {
        drawSpriteRot(texture,x,y,w,h,uv.x,uv.y,uv.z,uv.w,color,glow,0,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, float x, float y, float w, float h, Vector4f uv, Color color, float glow, int id, boolean transparentID) {
        drawSpriteRot(texture,x,y,w,h,uv.x,uv.y,uv.z,uv.w,color,glow,0,id,transparentID,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, float x, float y, float w, float h, float u, float v, float u2, float v2) {
        drawSpriteRot(texture,x,y,w,h,u,v,u2,v2,Color.WHITE,0,0,0,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, float x, float y, float w, float h, float u, float v, float u2, float v2, int id) {
        drawSpriteRot(texture,x,y,w,h,u,v,u2,v2,Color.WHITE,0,0,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, float x, float y, float w, float h, float u, float v, float u2, float v2, Color color, int id) {
        drawSpriteRot(texture,x,y,w,h,u,v,u2,v2,color,0,0,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, float x, float y, float w, float h, float u, float v, float u2, float v2, Color color, float glow, int id) {
        drawSpriteRot(texture,x,y,w,h,u,v,u2,v2,color,glow,0,id,true,false);
    }
    /** {@link #drawSpriteRot(Texture, float, float, float, float, float, float, float, float, Color, float, float, int, boolean, boolean)}*/
    default void drawSprite(Texture texture, float x, float y, float w, float h, float u, float v, float u2, float v2, Color color, float glow, int id, boolean transparentID) {
        drawSpriteRot(texture,x,y,w,h,u,v,u2,v2,color,glow,0,id,transparentID,false);
    }
    /**
     * Rendeer a textured quad in screen space.
     * @param texture texture or null
     * @param x sprite botom left position on screen
     * @param y sprite botom left position on screen
     * @param w sprite width in screen pixels
     * @param h sprite height in screen pixels
     * @param u texture u - left uv coordinate of texture region
     * @param v texture v - top uv coordinate of texture region
     * @param u2 texture u2 - right uv coordinate of texture region
     * @param v2 texture v2 - bottom uv coordinate of texture region
     * @param color linear rgba tint of sprite
     * @param glow normalized strength of the color (0: color, 1: color + color + glow * MAX_GLOW)
     * @param rot rotation in radians of the sprite around it's center (0 for no rotation)
     * @param id 32-bit id associated with the sprite
     * @param transparentID if true the sprite will output the id even if sprite is stansparent (default == true)
     * @param pixelAAA Pixel-Art Anti-Aliasing. Useful for reendering upscaled "pixelated" sprites.
     *                Only works with bi-linear texture sampling (default == false)
     */
    default void drawSpriteRot(Texture texture, float x, float y, float w, float h, float u, float v, float u2, float v2, Color color, float glow, float rot, int id, boolean transparentID, boolean pixelAAA) {
        drawSpriteSuper(texture, x, y, x + w, y + h, u, v, u2, v2, color, glow, rot, id, transparentID, pixelAAA);
    }


    void drawSpriteSuper(Texture texture, float x1, float y1, float x2, float y2, float u, float v, float u2, float v2, Color color, float glow, float rot, int id, boolean transparentID, boolean pixelAAA);






    default void drawLabel(CharSequence label, float penX, float penY, int font, int size, Color color) { drawLabel(label,penX,penY,font,size,color,0,false); }
    default void drawLabel(CharSequence label, float penX, float penY, int font, int size, Color color, boolean outlined) { drawLabel(label,penX,penY,font,size,color,0,outlined); }
    void drawLabel(CharSequence label, float penX, float penY, int font, int size, Color color, float glow, boolean outlined);

    default void drawLabel(CharSequence label, Rectanglef bounds, int font, Color color) { drawLabel(label,bounds,font,color,0,false,TextAlignment.LEFT); }
    default void drawLabel(CharSequence label, Rectanglef bounds, int font, Color color, TextAlignment align) { drawLabel(label,bounds,font,color,0,false,align); }
    default void drawLabel(CharSequence label, Rectanglef bounds, int font, Color color, boolean outlined, TextAlignment align) { drawLabel(label,bounds,font,color,0,outlined,align); }
    void drawLabel(CharSequence label, Rectanglef bounds, int font, Color color, float glow, boolean outlined, TextAlignment align);

    default void drawLabelInt(int value, float penX, float penY, int font, int size, Color color) { drawLabelInt(value,penX,penY,font,size,color,0,false); }
    default void drawLabelInt(int value, float penX, float penY, int font, int size, Color color, boolean outlined) { drawLabelInt(value,penX,penY,font,size,color,0,outlined); }
    void drawLabelInt(int value, float penX, float penY, int font, int size, Color color, float glow, boolean outlined);

    default void drawLabelInt(int value, Rectanglef bounds, int font, Color color) { drawLabelInt(value,bounds,font,color,0,false,TextAlignment.LEFT); }
    default void drawLabelInt(int value, Rectanglef bounds, int font, Color color, TextAlignment align) { drawLabelInt(value,bounds,font,color,0,false,align); }
    default void drawLabelInt(int value, Rectanglef bounds, int font, Color color, boolean outlined, TextAlignment align) { drawLabelInt(value,bounds,font,color,0,outlined,align); }
    void drawLabelInt(int value, Rectanglef bounds, int font, Color color, float glow, boolean outlined, TextAlignment align);

    default void drawLabelFloat(double value, float penX, float penY, int font, int size, Color color) { drawLabelFloat(value,2,penX,penY,font,size,color,0,false); }
    default void drawLabelFloat(double value, int deci, float penX, float penY, int font, int size, Color color) { drawLabelFloat(value,deci,penX,penY,font,size,color,0,false); }
    default void drawLabelFloat(double value, int deci, float penX, float penY, int font, int size, Color color, boolean outlined) { drawLabelFloat(value,deci,penX,penY,font,size,color,0,outlined); }
    void drawLabelFloat(double value, int deci, float penX, float penY, int font, int size, Color color, float glow, boolean outlined);

    default void drawLabelFloat(double value, Rectanglef bounds, int font, Color color) { drawLabelFloat(value,2,bounds,font,color,0,false,TextAlignment.LEFT); }
    default void drawLabelFloat(double value, int deci, Rectanglef bounds, int font, Color color) { drawLabelFloat(value,deci,bounds,font,color,0,false,TextAlignment.LEFT); }
    default void drawLabelFloat(double value, int deci, Rectanglef bounds, int font, Color color, TextAlignment align) { drawLabelFloat(value,deci,bounds,font,color,0,false,align); }
    default void drawLabelFloat(double value, int deci, Rectanglef bounds, int font, Color color, boolean outlined, TextAlignment align) { drawLabelFloat(value,deci,bounds,font,color,0,outlined,align); }
    void drawLabelFloat(double value, int deci, Rectanglef bounds, int font, Color color, float glow, boolean outlined, TextAlignment align);

    default void drawLabelBool(boolean value, float penX, float penY, int font, int size, Color color) { drawLabelBool(value,penX,penY,font,size,color,0,false); }
    default void drawLabelBool(boolean value, float penX, float penY, int font, int size, Color color, boolean outlined) { drawLabelBool(value,penX,penY,font,size,color,0,outlined); }
    void drawLabelBool(boolean value, float penX, float penY, int font, int size, Color color, float glow, boolean outlined);

    default void drawLabelBool(boolean value, Rectanglef bounds, int font, Color color) { drawLabelBool(value,bounds,font,color,0,false,TextAlignment.LEFT); }
    default void drawLabelBool(boolean value, Rectanglef bounds, int font, Color color, TextAlignment align) { drawLabelBool(value,bounds,font,color,0,false,align); }
    default void drawLabelBool(boolean value, Rectanglef bounds, int font, Color color, boolean outlined, TextAlignment align) { drawLabelBool(value,bounds,font,color,0,outlined,align); }
    void drawLabelBool(boolean value, Rectanglef bounds, int font, Color color, float glow, boolean outlined, TextAlignment align);

    default void drawText(CharSequence text, float penX, float penY, int font, int size, Color color) { drawText(text,penX,penY,font,size,color,0,false); }
    default void drawText(CharSequence text, float penX, float penY, int font, int size, Color color, boolean outlined) { drawText(text,penX,penY,font,size,color,0,outlined); }
    void drawText(CharSequence text, float penX, float penY, int font, int size, Color color, float glow, boolean outlined);

    default void drawText(CharSequence text, Rectanglef bounds, int font, int size, Color color) { drawText(text,bounds,font,size,color,0,false,false); }
    default void drawText(CharSequence text, Rectanglef bounds, int font, int size, Color color, boolean wrap) { drawText(text,bounds,font,size,color,0,false,wrap); }
    default void drawText(CharSequence text, Rectanglef bounds, int font, int size, Color color, boolean outlined, boolean wrap) { drawText(text,bounds,font,size,color,0,outlined,wrap); }
    void drawText(CharSequence text, Rectanglef bounds, int font, int size, Color color, float glow, boolean outlined, boolean wrap);

    default void drawTextField(Text text, TextLayout layout, float xOff, float yOff, Color color) { drawTextField(text,layout,xOff,yOff,color,0,false,TextAlignment.LEFT); }
    default void drawTextField(Text text, TextLayout layout, float xOff, float yOff, Color color, TextAlignment alignment) { drawTextField(text,layout,xOff,yOff,color,0,false,alignment); }
    default void drawTextField(Text text, TextLayout layout, float xOff, float yOff, Color color, float glow, TextAlignment alignment) { drawTextField(text,layout,xOff,yOff,color,glow,false,alignment); }
    void drawTextField(Text text, TextLayout layout, float xOff, float yOff, Color color, float glow, boolean outlined, TextAlignment alignment);




}

