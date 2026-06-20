package io.github.nascentlogic.jgen.gfx;

import org.joml.Vector4f;

/**
 * A texture region is a rectangular area in pixels of a 2D texture.
 * pixel(0,0) is the bottom left corner of the texture.
 * uv(0,0) is the bottom left corner of the texture.
 * Frederik Dahl 3/4/2025
 */
public class TextureRegion {

    private final int tw;
    private final int th;
    private final int x;
    private final int y;
    private final int w;
    private final int h;

    /**
     * @param tw texture width
     * @param th texture height
     * @param x x coordinate
     * @param y y coordinate
     * @param w region width
     * @param h region height
     */
    public TextureRegion(int tw, int th, int x, int y, int w, int h) {
        if (tw <= 0 || th <= 0) throw new RuntimeException("invalid texture region size");
        this.tw = tw;
        this.th = th;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public TextureRegion(int tw, int th) {
        this(tw,th,0,0,tw,th);
    }
    public int textureW() { return tw; }
    public int textureH() { return th; }
    public int width() { return w; }
    public int height() { return h; }
    public int x() { return x; }
    public int y() { return y; }
    public Vector4f uvCoordinates(boolean pixelCentered) {
        return uvCoordinates(new Vector4f(),pixelCentered);
    }

    /**
     * @param dst uv coordinates
     * @param pixelCentered pixel centered (false if unsure)
     * (Could remove "texture bleeding artifacts". It's better to use padding between sprites in atlas)
     * @return dst
     */
    public Vector4f uvCoordinates(Vector4f dst, boolean pixelCentered) {
        float x = this.x % tw;
        float y = this.y % th;
        if (pixelCentered) {
            dst.x = (x + 0.5f) / tw;
            dst.y = (y + 0.5f) / th;
            dst.z = (x + w - 0.5f) / tw;
            dst.w = (y + h - 0.5f) / th;
        } else {
            dst.x = x / tw;
            dst.y = y / th;
            dst.z = (x + w) / tw;
            dst.w = (y + h) / th;
        } return dst;
    }

    /**
     * Get sub-region uv coordinates
     * @param numSprites number of sprites
     * @param spriteW sprite width
     * @param spriteH sprite height
     * @param pixelCentered pixel centered (false if unsure)
     * @return array of sprites uv coordinates
     */
    public Vector4f[] uvCoordinates(int numSprites, int spriteW, int spriteH, boolean pixelCentered) {
        int cols = (int) Math.ceil((float) w / Math.max(spriteW,1));
        int rows = (int) Math.ceil((float) h / Math.max(spriteH,1));
        numSprites = Math.clamp(numSprites, 1, cols * rows);
        Vector4f[] result = new Vector4f[numSprites];
        for (int row = 0; row < rows; row++) {
            float y1 = y + row * spriteH;
            float y2 = y1 + spriteH;
            if (pixelCentered) {
                y1 += 0.5f;
                y2 -= 0.5f;
            }
            for (int col = 0; col < cols; col++) {
                int i = row * cols + col;
                if(i == numSprites) return result;
                float x1 = x + col * spriteW;
                float x2 = x1 + spriteW;
                if (pixelCentered) {
                    x1 += 0.5f;
                    x2 -= 0.5f;
                }
                result[i] = new Vector4f();
                result[i].x = x1 / spriteW;
                result[i].y = y1 / spriteH;
                result[i].z = x2 / spriteW;
                result[i].w = y2 / spriteH;
            }
        } return result;
    }

}
