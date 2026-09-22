package io.github.nascentlogic.jgen.gui.util;

import io.github.nascentlogic.jgen.gfx.Color;
import io.github.nascentlogic.jgen.gui.Font;
import io.github.nascentlogic.jgen.gui.text.Text;
import org.joml.primitives.Rectanglef;


/**
 * F.Dahl, 9/11/2026
 */
public class FontUtils {

    public static final int TAB_LEN = 4;



    public static int calcTabIndents(int column, int tabLen) {
        return tabLen - (column % tabLen);
    }

    public static float allignLabelX(Rectanglef bounds, TextAlignment alignment, float textWidth) {
        return switch (alignment) {
            case LEFT -> bounds.minX;
            case CENTER, RIGHT -> {
                if (alignment == TextAlignment.CENTER) {
                    yield bounds.minX + (bounds.lengthX() - textWidth) * 0.5f;
                }  else yield bounds.minX + bounds.lengthX() - textWidth;
            }
        };
    }

    public static float allignLabelX(Text text, Rectanglef bounds, TextAlignment alignment, Font font, float fontScale) {
        return switch (alignment) {
            case LEFT -> bounds.minX;
            case CENTER, RIGHT -> {
                float textWidth = unscaledLabelWidth(text, font) * fontScale;
                if (alignment == TextAlignment.CENTER) {
                    yield bounds.minX + (bounds.lengthX() - textWidth) * 0.5f;
                }  else yield bounds.minX + bounds.lengthX() - textWidth;
            }
        };
    }

    public static float allignLabelY(Rectanglef bounds, Font font, float fontScale) {
        return bounds.minY + 0.5f * (bounds.lengthY() - (font.ascent - font.descent) * fontScale);
    }

    /**
     * Calculates the unscaled width of a "label".
     * A "label" is a single line string of text.
     * LINEFEED's and TAB's are meassuered as a single SPACE character.
     * @param text text
     * @param font font
     * @return unscaled width of the label string
     */
    public static float unscaledLabelWidth(Text text, Font font) {
        if (text == null || text.isEmpty()) return 0.0f;
        final float spaceAdvance = font.glyph(Text.SPACE).advance();
        if (font.monospaced) return text.length() * spaceAdvance;
        final int textLen = text.length();
        float width = 0.0f;
        byte previousChar = 0;
        for (int i = 0; i < textLen; i++) {
            byte c = text.get(i);
            if (c == Text.LINE_FEED || c == Text.TAB) c = Text.SPACE;
            float advance = font.glyph(c).advance();
            if (i != 0) advance += font.kerning(previousChar,c);
            width += advance;
            previousChar = c;
        } return width;
    }

    public static void streamLabel(Text text, Rectanglef bounds, Font font, int fontSlot,
    int fontSize, Color color, float glow, boolean outlined, TextAlignment alignment, GlyphStream out) {
        float fontScale = fontSize / font.size;
        float startX = allignLabelX(text,bounds,alignment,font,fontScale);
        float startY = allignLabelY(bounds,font,fontScale);
        streamLabel(text,startX,startY,font,fontSlot,fontSize,color,glow,outlined,out);
    }

    public static void streamLabel(Text text, float startX, float startY, Font font,
    int fontSlot, int fontSize, Color color, float glow, boolean outlined, GlyphStream out) {
        if (text == null || text.isEmpty() || fontSize <= 0) return;
        final float spaceAdvance = font.glyph(Text.SPACE).advance();
        final float scale = fontSize / font.size;
        final int textLen = text.length();
        final float fColor = color.packedFormat();
        float penX = startX;
        int fontBits = ((fontSize & 0xFF) << 8);
        fontBits |= (fontSlot & 0x07) << 16;
        fontBits |= outlined ? (1 << 19) : 0;
        fontBits |= (glow > 0 ? ((int) (4095f * Math.min(glow, 1.0f))) << 20: 0); // 12 bit
        if (font.monospaced) {
            final float advanceScaled = spaceAdvance * scale;
            for (int i = 0; i < textLen; i++) {
                byte c = text.get(i);
                if (c != Text.LINE_FEED && c != Text.TAB && c != Text.SPACE) {
                    float fData = Float.intBitsToFloat(fontBits | (c & 0xFF));
                    out.put(penX,startY,fData,fColor);
                } penX += advanceScaled;
            }
        } else {
            byte previousChar = 0;
            for (int i = 0; i < textLen; i++) {
                byte c = text.get(i);
                if (c == Text.LINE_FEED || c == Text.TAB) c = Text.SPACE;
                if (c != Text.SPACE) {
                    float fData = Float.intBitsToFloat(fontBits | (c & 0xFF));
                    out.put(penX,startY,fData,fColor);
                } float advanceUnscaled = font.glyph(c).advance();
                if (i != 0) advanceUnscaled += font.kerning(previousChar,c);
                penX += advanceUnscaled * scale;
                previousChar = c;
            }
        }
    }

    public static void streamText(Text text, Rectanglef bounds, Font font,
    int fontSlot, int fontSize, Color color, float glow, boolean outlined, boolean wordWrap, GlyphStream out) {
        if (text == null || text.isEmpty() || fontSize <= 0) return;


    }

    public static void streamText(Text text, float startX, float startY, Font font,
    int fontSlot, int fontSize, Color color, float glow, boolean outlined, GlyphStream out) {
        if (text == null || text.isEmpty() || fontSize <= 0) return;
        final float scale = fontSize / font.size;
        final float spaceAdvance = font.glyph(Text.SPACE).advance();
        final float tabAdvance = font.indentAdvance();
        final float lineHeight = font.lineHeight();
        final float spaceAdvanceScaled = spaceAdvance * scale;
        final float tabAdvanceScaled = tabAdvance * scale;
        final float lineHeightScaled = lineHeight * scale;
        final int textLen = text.length();
        final float fColor = color.packedFormat();

        int fontBits = ((fontSize & 0xFF) << 8);
        fontBits |= (fontSlot & 0x07) << 16;
        fontBits |= outlined ? (1 << 19) : 0;
        fontBits |= (glow > 0 ? ((int) (4095f * Math.min(glow, 1.0f))) << 20: 0); // 12 bit

        int col = 0;
        int index = 0;

        if (font.monospaced) {
            int row = 0;
            while (index < textLen) {
                byte c = text.get(index);
                if (c == Text.LINE_FEED) {
                    col = 0;
                    row++;
                } else if (c == Text.SPACE) {
                    col++;
                } else if (c == Text.TAB) {
                    col += calcTabIndents(col,TAB_LEN);
                } else {
                    float penX = startX + col * spaceAdvanceScaled;
                    float penY = startY - row * lineHeightScaled;
                    float fData = Float.intBitsToFloat(fontBits | (c & 0xFF));
                    out.put(penX,penY,fData,fColor);
                    col++;
                } index++;
            }
        } else {
            float penX = startX;
            float penY = startY;
            byte prevChar = 0;
            while (index < textLen) {
                byte c = text.get(index);
                if (c == Text.LINE_FEED) {
                    penX = startX;
                    penY -= lineHeightScaled;
                    prevChar = 0;
                    col = 0;
                } else if (c == Text.SPACE) {
                    penX += spaceAdvanceScaled;
                    prevChar = 0;
                    col++;
                } else if (c == Text.TAB) {
                    int indents = calcTabIndents(col,TAB_LEN);
                    penX += indents * tabAdvanceScaled;
                    col += indents;
                    prevChar = 0;
                } else {
                    float fData = Float.intBitsToFloat(fontBits | (c & 0xFF));
                    out.put(penX,penY,fData,fColor);
                    float adv = font.glyph(c).advance();
                    if (prevChar != 0) adv += font.kerning(prevChar, c);
                    penX += adv * scale;
                    prevChar = c;
                    col++;
                } index++;
            }
        }
    }



}
