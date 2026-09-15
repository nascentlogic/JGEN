package io.github.nascentlogic.jgen.gui.adt;

/**
 * F.Dahl, 9/12/2026
 */
public interface GlyphStream {
    /**
     * @param penX glyph pen position x
     * @param penY glyph pen position x
     * @param data packed glyph date
     * @param color packed 32-bit RGBA
     */
    void put(float penX, float penY, float data, float color);

}
