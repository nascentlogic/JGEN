package io.github.nascentlogic.jgen.utils;

import org.joml.Vector4f;

/**
 * F.Dahl, 6/21/2026
 */
public class TextureRegion {

    public int x;
    public int y;
    public int w;
    public int h;

    public TextureRegion() { /* */ }
    public TextureRegion(int w, int h) { this(0,0,w,h); }
    public TextureRegion(int x, int y, int w, int h) { set(x, y, w, h); }

    public void set(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void translate(int x, int y) {
        this.x += x;
        this.y += y;
    }

    public Vector4f uvCoords(int textureW, int textureH, boolean pixelCentered) {
        return uvCoords(textureW,textureH,pixelCentered,new Vector4f());
    }

    public Vector4f uvCoords(int textureW, int textureH, boolean pixelCentered, Vector4f dst) {
        float tw = Math.max(1,textureW);
        float th = Math.max(1,textureH);
        if (pixelCentered) {
            dst.x = (x + 0.5f) / tw;       // u
            dst.y = (y + 0.5f) / th;       // v  (Top Edge of File)
            dst.z = (x + w - 0.5f) / tw;   // u2
            dst.w = (y + h - 0.5f) / th;   // v2 (Bottom Edge of File)
        } else {
            dst.x = x / tw;                // u
            dst.y = y / th;                // v  (Top Edge of File)
            dst.z = (x + w) / tw;          // u2
            dst.w = (y + h) / th;          // v2 (Bottom Edge of File)
        } return dst;
    }

    public TextureRegion[] subDivide(int textureW, int textureH, int spriteW, int spriteH) {
        return subDivide(textureW,textureH,spriteW,spriteH,Integer.MAX_VALUE);
    }

    public TextureRegion[] subDivide(int textureW, int textureH, int spriteW, int spriteH, int numSprites) {
        if (w > 0 && h > 0 && numSprites > 0 && textureW > 0 && textureH > 0 && spriteW > 0 && spriteH > 0) {
            int cols = (int) Math.ceil((float) w / spriteW); // Guaranteed >= 1
            int rows = (int) Math.ceil((float) h / spriteH); // Guaranteed >= 1
            numSprites = Math.min(numSprites, cols * rows);
            TextureRegion[] result = new TextureRegion[numSprites];
            int index = 0;
            for (int row = 0; row < rows; row++) {
                int currentY = this.y + (row * spriteH);
                for (int col = 0; col < cols; col++) {
                    if (index == numSprites) return result;
                    int currentX = this.x + (col * spriteW);
                    result[index] = new TextureRegion(currentX, currentY, spriteW, spriteH);
                    index++;
                }
            } return result;
        } else return new TextureRegion[0];
    }

    @Override
    public String toString() {
        return "("+x+", "+y+", "+w+", "+h+")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TextureRegion other = (TextureRegion) obj;
        return this.x == other.x &&
                this.y == other.y &&
                this.w == other.w &&
                this.h == other.h;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + x;
        result = 31 * result + y;
        result = 31 * result + w;
        result = 31 * result + h;
        return result;
    }
}
