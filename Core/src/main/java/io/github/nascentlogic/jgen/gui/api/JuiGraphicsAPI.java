package io.github.nascentlogic.jgen.gui.api;

import io.github.nascentlogic.jgen.gfx.Color;
import io.github.nascentlogic.jgen.gfx.Texture;
import io.github.nascentlogic.jgen.gui.Font;
import io.github.nascentlogic.jgen.gui.text.Text;
import io.github.nascentlogic.jgen.gui.util.TextAlignment;
import io.github.nascentlogic.jgen.utils.Disposable;
import org.joml.Vector4f;
import org.joml.primitives.Rectanglef;
import org.joml.primitives.Rectanglei;

import java.util.List;

/**
 * F.Dahl, 9/11/2026
 */
public interface JuiGraphicsAPI extends Disposable {




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
        drawSpriteSink(null,rect.minX,rect.minY,rect.maxX,rect.maxY,0,0,1,1,color,glow,rot,id,transparentID,false);
    }

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
        drawSpriteSink(null,x,y,x + w, y + h,0,0,1,1, color, glow, rot,  id, transparentID, false);
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
        drawSpriteSink(texture,rect.minX,rect.minY,rect.maxX,rect.maxY,u,v,u2,v2,color,glow,rot,id,transparentID,pixelAAA);
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
        drawSpriteSink(texture, x, y, x + w, y + h, u, v, u2, v2, color, glow, rot, id, transparentID, pixelAAA);
    }

    void drawSpriteSink(Texture texture, float x1, float y1, float x2, float y2, float u, float v, float u2, float v2, Color color, float glow, float rot, int id, boolean transparentID, boolean pixelAAA);


    /**
     * Draw generic single line of text from absolute pen position.
     * @param text any charsequence
     * @param penX start x position of pen
     * @param penY start y position of pen
     * @param font font index (bond font slot)
     * @param size target font size (0 - 255)
     * @param color color of text
     * @param glow normalized strength of the color (0: color, 1: color + color + glow * MAX_GLOW)
     * @param outlined if the text should be rendered with outlines
     */
    void drawLabel(CharSequence text, float penX, float penY, int font, int size, Color color, float glow, boolean outlined);

    /**
     * Draw generic single line of text inside a box
     * @param text any charsequence
     * @param bounds bounds of the line. text will be centered vertically and alligned horizontally according to the text allignment.
     * @param font font index (bond font slot)
     * @param size target font size (0 - 255)
     * @param color color of text
     * @param glow normalized strength of the color (0: color, 1: color + color + glow * MAX_GLOW)
     * @param outlined if the text should be rendered with outlines
     */
    void drawLabel(CharSequence text, Rectanglef bounds, int font, int size, Color color, float glow, boolean outlined, TextAlignment alignment);

    /**
     * Draw integer digit from absolute pen position.
     * @param value integer
     * @param penX start x position of pen
     * @param penY start y position of pen
     * @param font font index (bond font slot)
     * @param size target font size (0 - 255)
     * @param color color of text
     * @param glow normalized strength of the color (0: color, 1: color + color + glow * MAX_GLOW)
     * @param outlined if the text should be rendered with outlines
     */
    void drawLabelInt(int value, float penX, float penY, int font, int size, Color color, float glow, boolean outlined);

    /**
     * Draw integer digit inside a box
     * @param value integer
     * @param bounds bounds of the line. text will be centered vertically and alligned horizontally according to the text allignment.
     * @param font font index (bond font slot)
     * @param size target font size (0 - 255)
     * @param color color of text
     * @param glow normalized strength of the color (0: color, 1: color + color + glow * MAX_GLOW)
     * @param outlined if the text should be rendered with outlines
     */
    void drawLabelInt(int value, Rectanglef bounds, int font, int size, Color color, float glow, boolean outlined, TextAlignment alignment);

    /**
     * Draw floating point digit from absolute pen position.
     * @param value float / double
     * @param penX start x position of pen
     * @param penY start y position of pen
     * @param font font index (bond font slot)
     * @param size target font size (0 - 255)
     * @param color color of text
     * @param glow normalized strength of the color (0: color, 1: color + color + glow * MAX_GLOW)
     * @param outlined if the text should be rendered with outlines
     */
    void drawLabelFloat(double value, float penX, float penY, int font, int size, Color color, float glow, boolean outlined);

    /**
     * Draw floating point digit inside a box
     * @param value float / double
     * @param bounds bounds of the line. text will be centered vertically and alligned horizontally according to the text allignment.
     * @param font font index (bond font slot)
     * @param size target font size (0 - 255)
     * @param color color of text
     * @param glow normalized strength of the color (0: color, 1: color + color + glow * MAX_GLOW)
     * @param outlined if the text should be rendered with outlines
     */
    void drawLabelFloat(double value, Rectanglef bounds, int font, int size, Color color, float glow, boolean outlined, TextAlignment alignment);

    /**
     * Draw unbounded left alligned text from absolute pen position.
     * @param text text
     * @param penX start x position of pen
     * @param penY start y position of pen
     * @param font font index (bond font slot)
     * @param size target font size (0 - 255)
     * @param color color of text
     * @param glow normalized strength of the color (0: color, 1: color + color + glow * MAX_GLOW)
     * @param outlined if the text should be rendered with outlines
     */
    void drawText(Text text, float penX, float penY, int font, int size, Color color, float glow, boolean outlined);

    /**
     * Draw left alligned text inside a box.<p>
     * Word Wrap enabled: text will be bounded horizontally, but words will still be rendered
     * outside the bound if the word is wider than the bound width. <p>
     * @param text text
     * @param bounds bounds of the text.
     * @param font font index (bond font slot)
     * @param size target font size (0 - 255)
     * @param color color of text
     * @param glow normalized strength of the color (0: color, 1: color + color + glow * MAX_GLOW)
     * @param outlined if the text should be rendered with outlines
     * @param wordWrap if the text should wrap horizontally.
     */
    void drawText(Text text, Rectanglef bounds, int font, int size, Color color, float glow, boolean outlined, boolean wordWrap);



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

    // =============================================================================
    // COLORED QUADS
    // =============================================================================

    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRect(Rectanglef rect, Color color) {
        drawRectRot(rect,color,0,0,0,true);
    }
    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRect(Rectanglef rect, Color color, int id) {
        drawRectRot(rect,color,0,0,id,true);
    }
    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRect(Rectanglef rect, Color color, float glow, int id) {
        drawRectRot(rect,color,glow,0,id,true);
    }
    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRect(Rectanglef rect, Color color, float glow, int id, boolean transparentID) {
        drawRectRot(rect,color,glow,0,id,transparentID);
    }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRect(float x, float y, float w, float h, Color color) {
        drawRectRot(x,y,w,h,color,0,0,0,true);
    }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRect(float x, float y, float w, float h, Color color, int id) {
        drawRectRot(x,y,w,h,color,0,0,id,true);
    }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRect(float x, float y, float w, float h, Color color, float glow, int id) {
        drawRectRot(x,y,w,h,color,glow,0,id,true);
    }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRect(float x, float y, float w, float h, Color color, float glow, int id, boolean transparentID) {
        drawRectRot(x,y,w,h,color,glow,0,id,transparentID);
    }
    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRectRot(Rectanglef rect, Color color, float rot) {
        drawRectRot(rect,color,0,rot,0,true);
    }
    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRectRot(Rectanglef rect, Color color, float rot, int id) {
        drawRectRot(rect,color,0,rot,id,true);
    }
    /** {@link #drawRectRot(Rectanglef, Color, float, float, int, boolean)} */
    default void drawRectRot(Rectanglef rect, Color color, float glow, float rot, int id) {
        drawRectRot(rect,color,glow,rot,id,true);
    }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRectRot(float x, float y, float w, float h, Color color, float rot) {
        drawRectRot(x,y,w,h,color,0,rot,0,true);
    }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRectRot(float x, float y, float w, float h, Color color, float rot, int id) {
        drawRectRot(x,y,w,h,color,0,rot,id,true);
    }
    /** {@link #drawRectRot(float, float, float, float, Color, float, float, int, boolean)} */
    default void drawRectRot(float x, float y, float w, float h, Color color, float glow, float rot, int id) {
        drawRectRot(x,y,w,h,color,glow,rot,id,true);
    }

    // =============================================================================
    // TEXT
    // =============================================================================

    /** {@link #drawLabel(CharSequence, float, float, int, int, Color, float, boolean)}*/
    default void drawLabel(CharSequence text, float penX, float penY, int font, int size, Color color) {
        drawLabel(text,penX,penY,font,size,color,0,false);
    }
    /** {@link #drawLabel(CharSequence, float, float, int, int, Color, float, boolean)}*/
    default void drawLabel(CharSequence text, float penX, float penY, int font, int size, Color color, boolean outlined) {
        drawLabel(text,penX,penY,font,size,color,0,outlined);
    }
    /** {@link #drawLabel(CharSequence, Rectanglef, int, int, Color, float, boolean, TextAlignment)}*/
    default void drawLabel(CharSequence text, Rectanglef bounds, int font, int size, Color color) {
        drawLabel(text,bounds,font,size,color,0,false,TextAlignment.LEFT);
    }
    /** {@link #drawLabel(CharSequence, Rectanglef, int, int, Color, float, boolean, TextAlignment)}*/
    default void drawLabel(CharSequence text, Rectanglef bounds, int font, int size, Color color, TextAlignment alignment) {
        drawLabel(text,bounds,font,size,color,0,false,alignment);
    }
    /** {@link #drawLabel(CharSequence, Rectanglef, int, int, Color, float, boolean, TextAlignment)}*/
    default void drawLabel(CharSequence text, Rectanglef bounds, int font, int size, Color color, boolean outlined, TextAlignment alignment) {
        drawLabel(text,bounds,font,size,color,0,outlined,alignment);
    }

    /** {@link #drawLabelInt(int, float, float, int, int, Color, float, boolean)}*/
    default void drawLabelInt(int value, float penX, float penY, int font, int size, Color color) {
        drawLabelInt(value,penX,penY,font,size,color,0,false);
    }
    /** {@link #drawLabelInt(int, float, float, int, int, Color, float, boolean)}*/
    default void drawLabelInt(int value, float penX, float penY, int font, int size, Color color, boolean outlined) {
        drawLabelInt(value,penX,penY,font,size,color,0,outlined);
    }
    /** {@link #drawLabelInt(int, Rectanglef, int, int, Color, float, boolean, TextAlignment)}*/
    default void drawLabelInt(int value, Rectanglef bounds, int font, int size, Color color) {
        drawLabelInt(value,bounds,font,size,color,0,false,TextAlignment.LEFT);
    }
    /** {@link #drawLabelInt(int, Rectanglef, int, int, Color, float, boolean, TextAlignment)}*/
    default void drawLabelInt(int value, Rectanglef bounds, int font, int size, Color color, TextAlignment alignment) {
        drawLabelInt(value,bounds,font,size,color,0,false,alignment);
    }
    /** {@link #drawLabelInt(int, Rectanglef, int, int, Color, float, boolean, TextAlignment)}*/
    default void drawLabelInt(int value, Rectanglef bounds, int font, int size, Color color, boolean outlined, TextAlignment alignment) {
        drawLabelInt(value,bounds,font,size,color,0,outlined,alignment);
    }

    /** {@link #drawLabelFloat(double, float, float, int, int, Color, float, boolean)}*/
    default void drawLabelFloat(double value, float penX, float penY, int font, int size, Color color) {
        drawLabelFloat(value,penX,penY,font,size,color,0,false);
    }
    /** {@link #drawLabelFloat(double, float, float, int, int, Color, float, boolean)}*/
    default void drawLabelFloat(double value, float penX, float penY, int font, int size, Color color, boolean outlined) {
        drawLabelFloat(value,penX,penY,font,size,color,0,outlined);
    }
    /** {@link #drawLabelFloat(double, Rectanglef, int, int, Color, float, boolean, TextAlignment)}*/
    default void drawLabelFloat(double value, Rectanglef bounds, int font, int size, Color color) {
        drawLabelFloat(value,bounds,font,size,color,0,false,TextAlignment.LEFT);
    }
    /** {@link #drawLabelFloat(double, Rectanglef, int, int, Color, float, boolean, TextAlignment)}*/
    default void drawLabelFloat(double value, Rectanglef bounds, int font, int size, Color color, TextAlignment alignment) {
        drawLabelFloat(value,bounds,font,size,color,0,false,alignment);
    }
    /** {@link #drawLabelFloat(double, Rectanglef, int, int, Color, float, boolean, TextAlignment)}*/
    default void drawLabelFloat(double value, Rectanglef bounds, int font, int size, Color color, boolean outlined, TextAlignment alignment) {
        drawLabelFloat(value,bounds,font,size,color,0,outlined,alignment);
    }

    /** {@link #drawText(Text, float, float, int, int, Color, float, boolean)}*/
    default void drawText(Text text, float penX, float penY, int font, int size, Color color) {
        drawText(text,penX,penY,font,size,color,0,false);
    }
    /** {@link #drawText(Text, float, float, int, int, Color, float, boolean)}*/
    default void drawText(Text text, float penX, float penY, int font, int size, Color color, boolean outlined) {
        drawText(text,penX,penY,font,size,color,0,outlined);
    }
    /** {@link #drawText(Text, Rectanglef, int, int, Color, float, boolean, boolean)}*/
    default void drawText(Text text, Rectanglef bounds, int font, int size, Color color) {
        drawText(text,bounds,font,size,color,0,false,false);
    }
    /** {@link #drawText(Text, Rectanglef, int, int, Color, float, boolean, boolean)}*/
    default void drawText(Text text, Rectanglef bounds, int font, int size, Color color, boolean wordWrap) {
        drawText(text,bounds,font,size,color,0,false,wordWrap);
    }
    /** {@link #drawText(Text, Rectanglef, int, int, Color, float, boolean, boolean)}*/
    default void drawText(Text text, Rectanglef bounds, int font, int size, Color color, boolean outlined, boolean wordWrap) {
        drawText(text,bounds,font,size,color,0,outlined,wordWrap);
    }


    int resolutionWidth();
    int resolutionHeight();

    int debugDrawCalls();
    int debugSpritesRendered();
    int debugCharsRendered();
    int debugDeferredCallsMax();


    /** Adds a font to stored fonts if no font already exist with the same name.
     * @return false if a font already exist under the same name.
     * Added Fonts are freed automaically on gui exit */
    boolean fontAdd(Font font);
    /** Binds a stored font for use. Will flush the current batch.
     * @param index 0 to 4 (index wraps)
     * @param name name of the font (file name without the .ttf extension)
     * @return true if the font is a stored font (and therefore was bound)*/
    boolean fontBind(String name, int index);
    /** @param name name of the font (file name without the .ttf extension)
     * @return true if font is stored in library */
    boolean fontIsStored(String name);
    /** Returns the currently bound font for index.
     * By default it's the GUI default font.
     * @param index 0 to 4 (index wraps)
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




}
