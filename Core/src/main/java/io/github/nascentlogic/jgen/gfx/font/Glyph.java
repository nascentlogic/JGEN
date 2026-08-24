package io.github.nascentlogic.jgen.gfx.font;

/**
 * Font glyph
 * F.Dahl, 8/22/2026
 */

/**
 * @param value character
 * @param advance character base advance
 * @param xOff penX -> bottom left of character region
 * @param yOff penY -> bottom left of character region
 * @param x atlas x-position
 * @param y atlas y-position
 * @param w atlas region w
 * @param h atlas region h
 * @param u left UV
 * @param v top UV
 * @param u2 right UV
 * @param v2 bottom UV
 */
public record Glyph(char value, float advance, float xOff, float yOff,
                    int x, int y, int w, int h,
                    float u, float v, float u2, float v2) {
    @Override
    public String toString() {
        return String.format(
                "Glyph'%c' adv=%.2f off=(%.2f,%.2f) atlas=[%d,%d %dx%d] uv=[%.4f,%.4f → %.4f,%.4f]",
                value, advance, xOff, yOff, x, y, w, h, u, v, u2, v2);
    }
}
