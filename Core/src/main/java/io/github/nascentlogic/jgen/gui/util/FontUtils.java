package io.github.nascentlogic.jgen.gui.util;

import io.github.nascentlogic.jgen.gfx.Color;
import io.github.nascentlogic.jgen.gui.Font;
import io.github.nascentlogic.jgen.gui.adt.GlyphStream;
import io.github.nascentlogic.jgen.text.Text;

/**
 * F.Dahl, 9/11/2026
 */
public class FontUtils {

    public static final int TAB_LEN = 4;



    public static int calcTabIndents(int column, int tabLen) {
        return tabLen - (column % tabLen);
    }

    // all label non printable characters count as space? tab, newline count as space, rendered as space without checking?
    // Alternative: skip them without adding to width
    public static float calcLabelWidth(Text text, Font font, int fontSize) {
        if (text == null || text.isEmpty() || fontSize <= 0) return 0.0f;
        final float scale = fontSize / font.size;
        final float spaceAdvance = font.glyph(Text.SPACE).advance();
        float width = 0.0f;
        if (font.monospaced) {

        } else {

        }

        return width * scale;
    }


    public static void streamBoundless(Text text, float penX, float penY, Color color, Font font, int fontSize, int fontIndex, GlyphStream out) {
        if (text == null || text.isEmpty() || fontSize <= 0) return;
        final float scale = fontSize / font.size;
        final float spaceAdvance = font.glyph(Text.SPACE).advance();
        final float tabAdvance = font.indentAdvance();
        final float lineHeight = font.lineHeight();
        final float spaceAdvanceScaled = spaceAdvance * scale;
        final float tabAdvanceScaled = tabAdvance * scale;
        final float lineHeightScaled = lineHeight * scale;

        final int textLength = text.length();
        final int tabLen = TAB_LEN;

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
                    col += calcTabIndents(col,tabLen);
                } else {
                    float pX = penX + col * spaceAdvanceScaled;
                    float pY = penY - row * lineHeightScaled;
                    int iData = fontBits | (c & 0xFF);
                    float fData = Float.intBitsToFloat(iData);
                    out.put(pX,pY,fData,fColor);
                    col++;
                }
                i++;
            }
        } else {

            final int nullChar = 0;
            byte prevChar = nullChar;
            float xOffScaled = 0; // Track directly in scaled pixels
            float yOffScaled = 0;
            while (i < textLength) {
                byte c = text.get(i);
                if (c == Text.LINE_FEED) {
                    yOffScaled += lineHeightScaled;
                    xOffScaled = 0;
                    col = 0;
                    prevChar = nullChar;
                } else if (c == Text.SPACE) {
                    xOffScaled += spaceAdvanceScaled;
                    col++;
                    prevChar = nullChar;
                } else if (c == Text.TAB) {
                    int indents = calcTabIndents(col,tabLen);
                    xOffScaled += indents * tabAdvanceScaled;
                    col += indents;
                    prevChar = nullChar;
                } else {
                    int iData = fontBits | (c & 0xFF);
                    float fData = Float.intBitsToFloat(iData);
                    float pX = penX + xOffScaled;
                    float pY = penY - yOffScaled;
                    out.put(pX,pY,fData,fColor);

                    float adv = font.glyph(c).advance();
                    if (prevChar != nullChar) {
                       adv += font.kerning(prevChar, c);
                    }
                    xOffScaled += adv * scale; // Fixed: scale glyph advance + kerning
                    col++;
                    prevChar = c;
                }
                i++;
            }

        }















    }

}
