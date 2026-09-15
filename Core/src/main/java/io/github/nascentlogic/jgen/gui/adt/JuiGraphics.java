package io.github.nascentlogic.jgen.gui.adt;

import io.github.nascentlogic.jgen.gfx.Color;
import io.github.nascentlogic.jgen.gfx.Texture;
import io.github.nascentlogic.jgen.gui.Font;
import io.github.nascentlogic.jgen.io.Disk;
import io.github.nascentlogic.jgen.text.Text;
import io.github.nascentlogic.jgen.utils.Disposable;
import org.joml.Vector4f;
import org.joml.primitives.Rectanglef;
import org.joml.primitives.Rectanglei;

import java.util.List;

/**
 * F.Dahl, 9/11/2026
 */
public interface JuiGraphics extends Disposable {

    int SCISSOR_STACK_CAP = 64;
    /* ----------------------------------------
     * vertex: | pos | uv | color | id | data |
     * ----------------------------------------
     * size:   | 2   | 2  | 1     | 1  | 1    |
     * ----------------------------------------*/
    int SPRITE_TEXTURE_SLOTS = 8;
    int SPRITE_TEXTURE_UNIT_OFFSET = 5;
    int SPRITE_BATCH_CAP = 512;
    int SPRITE_VERTEX_SIZE_FLOAT = 7;
    int SPRITE_VERTEX_SIZE_BYTES = SPRITE_VERTEX_SIZE_FLOAT * Float.BYTES;
    int SPRITE_SIZE_FLOAT = SPRITE_VERTEX_SIZE_FLOAT * 4;
    int SPRITE_BATCH_SIZE_FLOAT = SPRITE_BATCH_CAP * SPRITE_SIZE_FLOAT;
    int SPRITE_BATCH_SIZE_BYTES = SPRITE_BATCH_SIZE_FLOAT * Float.BYTES;
    /* ------------------------------
     * vertex: | pos | color | data |
     * ------------------------------
     * size:   | 2   | 1     | 1    |
     * ------------------------------*/
    int TEXT_BLOCK_BINDING = 8;
    int TEXT_BATCH_CAP = 1024;
    int TEXT_VERTEX_SIZE_FLOAT = 4;
    int TEXT_BATCH_SIZE_FLOAT = TEXT_BATCH_CAP * TEXT_VERTEX_SIZE_FLOAT;
    int TEXT_BATCH_SIZE_BYTES = TEXT_BATCH_SIZE_FLOAT * Float.BYTES;

    String SHADER_RESOURCE_DIR = "jgen/gui/glsl";
    String SPRITE_PROGRAM_NAME = "jgen-gui-sprite";
    String TEXT_PROGRAM_NAME = "jgen-gui-text";


    /**
     * Rendeer a colored quad in screen space.
     * @param rect sprite transform on screen
     * @param color linear rgba tint of sprite
     * @param glow normalized strength of the color (0: color, 1: color + color + glow * MAX_GLOW)
     * @param rot rotation in radians of the sprite around it's center (0 for no rotation)
     * @param id 32-bit id associated with the sprite
     * @param transparentID if true the sprite will output the id even if sprite is stansparent (default == true)
     */
    void drawRectRot(Rectanglef rect, Color color, float glow, float rot, int id, boolean transparentID);

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
    void drawRectRot(float x, float y, float w, float h, Color color, float glow, float rot, int id, boolean transparentID);

    /**
     * SINK<p>
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
    void drawSpriteRot(Texture texture, Rectanglef rect, float u, float v, float u2, float v2, Color color, float glow, float rot, int id, boolean transparentID, boolean pixelAAA);

    /**
     * SINK<p>
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
    void drawSpriteRot(Texture texture, float x, float y, float w, float h, float u, float v, float u2, float v2, Color color, float glow, float rot, int id, boolean transparentID, boolean pixelAAA);

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



    /** Load entire directory under: {@link Disk#gameRootDirectory()}
     * If a named font already exist it will not be replaced.
     * Fonts are freed automaically on exit */
    void fontLoadLibrary(String first, String... more);
    /** Adds a font to stored fonts if no font already exist with the same name.
     * @return false if a font already exist under the same name.
     * Added Fonts are freed automaically on gui exit */
    boolean fontAdd(Font font);
    /** Binds a stored font for use. Will flush the current batch.
     * @param index 0 to 4 (index wraps)
     * @param name name of the font (file name without the .ttf extension)
     * @return true if the font is a stored font (and therefore was bound)*/
    boolean fontBind(int index, String name);
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


}
