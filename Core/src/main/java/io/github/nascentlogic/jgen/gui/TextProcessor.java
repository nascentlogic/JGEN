package io.github.nascentlogic.jgen.gui;

import io.github.nascentlogic.jgen.gfx.Color;
import io.github.nascentlogic.jgen.gui.api.GlyphStream;
import io.github.nascentlogic.jgen.gui.api.TextAlignment;
import io.github.nascentlogic.jgen.gui.text.ManagedText;
import io.github.nascentlogic.jgen.gui.text.Text;
import io.github.nascentlogic.jgen.gui.text.TextBlock;
import io.github.nascentlogic.jgen.gui.text.TextBuffer;
import io.github.nascentlogic.jgen.utils.Disposable;
import org.joml.primitives.Rectanglef;


/**
 * F.Dahl, 10/3/2026
 */
public class TextProcessor implements Disposable {

    private final FontLibrary fonts;
    private final TextBlock textBlock;
    private final TextBlock textBlockTrue;
    private final TextBlock textBlockFalse;
    private final TextBuffer textBuffer;


    TextProcessor(FontLibrary fonts) {
        this.fonts = fonts;
        this.textBlock = new TextBlock(512);
        this.textBuffer = new TextBuffer(2048);
        this.textBlockTrue = new TextBlock("True");
        this.textBlockFalse = new TextBlock("False");
    }


    void labelFreeBool(boolean value, float penX, float penY, int fontSlot, int fontSize, Color color, float glow, boolean outlined, GlyphStream out) {
        textFree(value ? textBlockTrue : textBlockFalse,penX,penY,fontSlot,fontSize,color,glow,outlined,out);
    }
    void labelFreeInt(int value, float penX, float penY, int fontSlot, int fontSize, Color color, float glow, boolean outlined, GlyphStream out) {
        textBlock.setInt(value);
        textFree(textBlock,penX,penY,fontSlot,fontSize,color,glow,outlined,out);
    }
    void labelFreeFloat(double value, int decimals, float penX, float penY, int fontSlot, int fontSize, Color color, float glow, boolean outlined, GlyphStream out) {
        textBlock.setFloat(value,decimals);
        textFree(textBlock,penX,penY,fontSlot,fontSize,color,glow,outlined,out);
    }
    void labelFree(CharSequence string, float penX, float penY, int fontSlot, int fontSize,
                   Color color, float glow, boolean outlined, GlyphStream out) {
        labelFree(toText(string),penX,penY,fontSlot,fontSize,color,glow,outlined,out);
    }
    void labelFree(Text text, float penX, float penY, int fontSlot, int fontSize,
                   Color color, float glow, boolean outlined, GlyphStream out) {
        if (text == null || text.isEmpty() || fontSize <= 0) return;
        Font font = fonts.boundFont(fontSlot);
        final int textLen = text.length();
        final float spaceAdvance = font.glyph(Text.SPACE).advance();
        final float scale = font.scale(fontSize);
        final float fColor = color.packedFormat();
        int fontData = fontData(fontSlot, fontSize, glow, outlined);
        float pX = penX;
        if (font.monospaced) {
            final float advanceScaled = spaceAdvance * scale;
            for (int i = 0; i < textLen; i++) {
                byte c = text.get(i);
                if (c != Text.LINE_FEED && c != Text.TAB && c != Text.SPACE) {
                    out.put(pX,penY,Float.intBitsToFloat(fontData | c),fColor);
                } pX += advanceScaled;
            }
        } else {
            byte prevChar = 0;
            for (int i = 0; i < textLen; i++) {
                byte c = text.get(i);
                if (c == Text.LINE_FEED || c == Text.TAB) c = Text.SPACE;
                if (c != Text.SPACE) {
                    out.put(pX,penY,Float.intBitsToFloat(fontData | c),fColor);
                } float advanceUnscaled = font.glyph(c).advance();
                if (i != 0) advanceUnscaled += font.kerning(prevChar,c);
                pX += advanceUnscaled * scale;
                prevChar = c;
            }
        }
    }

    void labelBoundBool(boolean value, Rectanglef bounds, int fontSlot,
                        Color color, float glow, boolean outlined, TextAlignment alignment, GlyphStream out) {
        labelBound(value ? textBlockTrue : textBlockFalse,bounds,fontSlot,color,glow,outlined,alignment,out);
    }

    void labelBoundInt(int value, Rectanglef bounds, int fontSlot,
                       Color color, float glow, boolean outlined, TextAlignment alignment, GlyphStream out) {
        textBlock.setInt(value);
        labelBound(textBlock,bounds,fontSlot,color,glow,outlined,alignment,out);
    }
    void labelBoundFloat(double value, int decimals, Rectanglef bounds, int fontSlot,
                         Color color, float glow, boolean outlined, TextAlignment alignment, GlyphStream out) {
        textBlock.setFloat(value,decimals);
        labelBound(textBlock,bounds,fontSlot,color,glow,outlined,alignment,out);
    }
    void labelBound(CharSequence string, Rectanglef bounds, int fontSlot,
                    Color color, float glow, boolean outlined, TextAlignment alignment, GlyphStream out) {
        labelBound(toText(string),bounds,fontSlot,color,glow,outlined,alignment,out);
    }
    void labelBound(Text text, Rectanglef bounds, int fontSlot,
                    Color color, float glow, boolean outlined, TextAlignment alignment, GlyphStream out) {
        if (text == null || text.isEmpty()) return;
        float boundsW = bounds.lengthX();
        float boundsH = bounds.lengthY();
        if (boundsW <= 0 || boundsH <= 0) return;
        Font font = fonts.boundFont(fontSlot);
        int fontSize = labelDesiredFontSize(font,boundsH);
        if (fontSize <= 0) return;

        final int textLen = text.length();
        final float scale = font.scale(fontSize);
        final float avgAdvance = font.avgAdvance * scale;
        final float fColor = color.packedFormat();
        final int fontData = fontData(fontSlot, fontSize, glow, outlined);
        final float labelWidth = labelUnscaledWidth(text,font) * scale;
        float penX = labelAlignX(bounds,alignment,labelWidth);
        float penY = labelAlignY(bounds,font,scale);

        if (font.monospaced) {
            final float spaceAdvanceUnscaled = font.glyph(Text.SPACE).advance();
            final float advanceScaled = spaceAdvanceUnscaled * scale;
            for (int i = 0; i < textLen; i++) {
                if ((penX + avgAdvance) > bounds.maxX) break;
                byte c = text.get(i);
                if (c != Text.LINE_FEED && c != Text.TAB && c != Text.SPACE) {
                    out.put(penX,penY,Float.intBitsToFloat(fontData | c),fColor);
                } penX += advanceScaled;
            }
        } else {
            byte prevChar = 0;
            for (int i = 0; i < textLen; i++) {
                if ((penX + avgAdvance) > bounds.maxX) break;
                byte c = text.get(i);
                if (c == Text.LINE_FEED || c == Text.TAB) c = Text.SPACE;
                if (c != Text.SPACE) {
                    out.put(penX,penY,Float.intBitsToFloat(fontData | c),fColor);
                } float advanceUnscaled = font.glyph(c).advance();
                if (i != 0) advanceUnscaled += font.kerning(prevChar,c);
                penX += advanceUnscaled * scale;
                prevChar = c;
            }
        }
    }

    void textFree(CharSequence string, float penX, float penY, int fontSlot, int fontSize,
        Color color, float glow, boolean outlined, GlyphStream out) {
        textFree(toText(string),penX,penY,fontSlot,fontSize,color,glow,outlined,out);
    } void textFree(Text text, float penX, float penY, int fontSlot, int fontSize,
        Color color, float glow, boolean outlined, GlyphStream out) {
        if (text == null || text.isEmpty() || fontSize <= 0) return;
        Font font = fonts.boundFont(fontSlot);
        final int textLen = text.length();
        final float scale = fontSize / font.size;
        final float spaceAdv = font.glyph(Text.SPACE).advance() * scale;
        final float tabAdv = font.indentAdvance() * scale;
        final float lineHeight = font.lineHeight() * scale;
        final float fColor = color.packedFormat();
        final int fontData = fontData(fontSlot, fontSize, glow, outlined);

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
                    col += tabIndents(col);
                } else {
                    float pX = penX + col * spaceAdv;
                    float pY = penY - row * lineHeight;
                    out.put(pX,pY,Float.intBitsToFloat(fontData | c),fColor);
                    col++;
                } index++;
            }
        } else {
            float pX = penX;
            float pY = penY;
            byte prevChar = 0;
            while (index < textLen) {
                byte c = text.get(index);
                if (c == Text.LINE_FEED) {
                    pX = penX;
                    pY -= lineHeight;
                    prevChar = 0;
                    col = 0;
                } else if (c == Text.SPACE) {
                    pX += spaceAdv;
                    prevChar = 0;
                    col++;
                } else if (c == Text.TAB) {
                    int indents = tabIndents(col);
                    pX += indents * tabAdv;
                    col += indents;
                    prevChar = 0;
                } else {
                    out.put(pX,pY,Float.intBitsToFloat(fontData | c),fColor);
                    float adv = font.glyph(c).advance();
                    if (prevChar != 0) adv += font.kerning(prevChar, c);
                    pX += adv * scale;
                    prevChar = c;
                    col++;
                } index++;
            }
        }
    }

    void textBound(CharSequence string, Rectanglef bounds, int fontSlot, int fontSize,
        Color color, float glow, boolean outlined, boolean wrap, GlyphStream out) {
        textBound(toText(string),bounds,fontSlot,fontSize,color,glow,outlined,wrap,out);
    } void textBound(Text text, Rectanglef bounds, int fontSlot, int fontSize,
        Color color, float glow, boolean outlined, boolean wrap, GlyphStream out) {
        if (text == null || text.isEmpty() || fontSize <= 0) return;
        Font font = fonts.boundFont(fontSlot);
        final float scale = fontSize / font.size;
        float penY = bounds.maxY - font.ascent * scale;
        float penX = bounds.minX;
        if (!wrap) textFree(text,penX,penY,fontSlot,fontSize,color,glow,outlined,out);

        final int textLen = text.length();
        final float spaceAdv = font.glyph(Text.SPACE).advance() * scale;
        final float tabAdv = font.indentAdvance() * scale;
        final float lineHeight = font.lineHeight() * scale;
        final float fColor = color.packedFormat();
        int fontData = fontData(fontSlot,fontSize,glow,outlined);
        int numColumns = (int) (bounds.lengthX() / spaceAdv);
        if (numColumns < 1) return;

        int col = 0;
        int row = 0;
        int index = 0;

        if (font.monospaced) {
            while (index < textLen) {
                byte c = text.get(index);
                if (c == Text.LINE_FEED) {
                    col = 0;
                    row++;
                    index++;
                } else if (c == Text.SPACE) {
                    col++;
                    index++;
                } else if (c == Text.TAB) {
                    col += tabIndents(col);
                    index++;
                } else {
                    int nextDelim = text.nextWordDelimiter(index);
                    int wordEnd = (nextDelim == -1) ? textLen : nextDelim;
                    int wordLen = wordEnd - index;
                    if (col > 0 && (col + wordLen) > numColumns) {
                        col = 0;
                        row++;
                    }
                    for (int i = 0; i < wordLen; i++) {
                        c = text.get(index);
                        float pX = penX + col * spaceAdv;
                        float pY = penY - row * lineHeight;
                        float fData = Float.intBitsToFloat(fontData | (c & 0xFF));
                        out.put(pX,pY,fData,fColor);
                        col++;
                        index++;
                    }
                }
            }
        } else {
            float pX = penX;
            float pY = penY;
            byte prevChar = 0;
            while (index < textLen) {
                byte c = text.get(index);
                if (c == Text.LINE_FEED) {
                    pX = penX;
                    pY -= lineHeight;
                    prevChar = 0;
                    col = 0;
                    index++;
                } else if (c == Text.SPACE) {
                    pX += spaceAdv;
                    prevChar = 0;
                    col++;
                    index++;
                } else if (c == Text.TAB) {
                    int indents = tabIndents(col);
                    pX += indents * tabAdv;
                    col += indents;
                    prevChar = 0;
                    index++;
                } else {
                    int nextDelim = text.nextWordDelimiter(index);
                    int wordEnd = (nextDelim == -1) ? textLen : nextDelim;
                    int wordLen = wordEnd - index;
                    float wordWidth = 0f;
                    byte wordPrevC = prevChar;
                    for (int i = index; i < wordEnd; i++) {
                        c = text.get(i);
                        float adv = font.glyph(c).advance();
                        if (wordPrevC != 0) {
                            adv += font.kerning(wordPrevC,c);
                        } wordWidth += adv;
                        wordPrevC = c;
                    }
                    wordWidth *= scale;
                    if (col > 0 && (pX + wordWidth > bounds.maxX)) {
                        pX = penX;
                        pY -= lineHeight;
                        col = 0;
                    }
                    for (int i = 0; i < wordLen; i++) {
                        c = text.get(index);
                        float fData = Float.intBitsToFloat(fontData | (c & 0xFF));
                        out.put(pX,pY,fData,fColor);
                        float adv = font.glyph(c).advance();
                        if (prevChar != 0) adv += font.kerning(prevChar, c);
                        pX += adv * scale;
                        prevChar = c;
                        col++;
                        index++;
                    }
                }
            }
        }
    }

    void textField(Text text, TextLayout layout, float xOff, float yOff,
        Color color, float glow, boolean outlined, TextAlignment align, GlyphStream out) {
        if (text == null || text.isEmpty() || layout.numLines() <= 0 || layout.fontSize() <= 0) return;
        Font font = fonts.boundFont(layout.fontSlot());
        final int textLen = text.length();
        final float scale = layout.fontSize() / font.size;
        final float lineHeight = scale * font.lineHeight();
        final float spaceAdv = scale * font.glyph(Text.SPACE).advance();
        final float tabAdv = scale * font.indentAdvance();
        final float ascent = scale * font.ascent;
        final float descent = scale * font.descent;
        final float boundsW = layout.container().lengthX();
        final float fColor = color.packedFormat();
        final int fontData = fontData(layout.fontSlot(), layout.fontSize(), glow, outlined);
        final float X = layout.container().minX + xOff;
        float pY = layout.container().maxY + yOff - ascent;

        for (int lineIndex = 0; lineIndex < layout.numLines(); lineIndex++) {
            TextLayout.Line line = layout.getLine(lineIndex);
            int col = 0;

            float lineTop = pY + ascent;
            float lineBot = pY - descent;
            if (lineTop < layout.container().minY || lineBot > layout.container().maxY) {
                pY -= lineHeight;
                continue;
            }
            float pX = X + line.xOffset(boundsW, align);
            if (font.monospaced) {
                for (int charIndex = line.fromIndex; charIndex < line.toIndex; charIndex++) {
                    if (charIndex >= textLen) break;
                    byte c = text.get(charIndex);
                    // Safety line: never occurs if the layout is the current layout of the text
                    if (c == Text.LINE_FEED) continue;
                    if (c == Text.SPACE) {
                        pX += spaceAdv;
                        col++;
                        continue;
                    }
                    if (c == Text.TAB) {
                        int indents = tabIndents(col);
                        pX += indents * spaceAdv;
                        col += indents;
                        continue;
                    }
                    out.put(pX, pY, Float.intBitsToFloat(fontData | c), fColor);
                    pX += spaceAdv;
                    col++;
                }
            } else {
                byte prevChar = 0;
                for (int charIndex = line.fromIndex; charIndex < line.toIndex; charIndex++) {
                    if (charIndex >= textLen) break;
                    byte c = text.get(charIndex);
                    // Safety line: never occurs if the layout is the current layout of the text
                    if (c == Text.LINE_FEED) continue;
                    if (c == Text.SPACE) {
                        pX += spaceAdv;
                        prevChar = 0;
                        col++;
                        continue;
                    }
                    if (c == Text.TAB) {
                        int indents = tabIndents(col);
                        pX += indents * tabAdv;
                        prevChar = 0;
                        col += indents;
                        continue;
                    }
                    out.put(pX, pY, Float.intBitsToFloat(fontData | c), fColor);
                    float adv = font.glyph(c).advance();
                    if (prevChar != 0) adv += font.kerning(prevChar, c);
                    pX += adv * scale;
                    prevChar = c;
                    col++;
                }
            }
            pY -= lineHeight;
        }
    }


    void textLayout(Text text, Rectanglef bounds, int fontSlot, int fontSize, boolean wrap, TextLayout dst) {
        dst.reset(bounds, fontSlot, fontSize);
        if (text == null || text.isEmpty() || fontSize <= 0) return;
        Font font = fonts.boundFont(fontSlot);
        final int textLen = text.length();
        final float scale = fontSize / font.size;
        final float boundsW = bounds.lengthX();
        final float spaceAdv = font.glyph(Text.SPACE).advance() * scale;
        final float tabAdv = font.indentAdvance() * scale;
        int numColumnsMono = (int) (boundsW / spaceAdv);
        if (numColumnsMono < 1) return;

        int lineStart = 0;
        int col = 0;
        int index = 0;

        if (font.monospaced) {
            if (wrap) {
                while (index < textLen) {
                    byte c = text.get(index);
                    if (c == Text.LINE_FEED) {
                        dst.newLine(lineStart, index, col * spaceAdv, true);
                        lineStart = index + 1;
                        col = 0;
                        index++;
                    } else if (c == Text.SPACE) {
                        col++;
                        index++;
                    } else if (c == Text.TAB) {
                        col += tabIndents(col);
                        index++;
                    } else {
                        int nextDelim = text.nextWordDelimiter(index);
                        int wordEnd = (nextDelim == -1) ? textLen : nextDelim;
                        int wordLen = wordEnd - index;
                        if (col > 0 && (col + wordLen) > numColumnsMono) {
                            dst.newLine(lineStart, index, col * spaceAdv, false);
                            lineStart = index;
                            col = 0;
                        }
                        col += wordLen;
                        index = wordEnd;
                    }
                }
            } else {
                while (index < textLen) {
                    byte c = text.get(index);
                    if (c == Text.LINE_FEED) {
                        dst.newLine(lineStart, index, col * spaceAdv, true);
                        lineStart = index + 1;
                        col = 0;
                    } else if (c == Text.SPACE) {
                        col++;
                    } else if (c == Text.TAB) {
                        col += tabIndents(col);
                    } else {
                        col++;
                    } index++;
                }
            }
            dst.newLine(lineStart, textLen, col * spaceAdv, false);

        } else {
            float lineWidth = 0.0f;
            byte prevChar = 0;
            if (wrap) {
                while (index < textLen) {
                    byte c = text.get(index);
                    if (c == Text.LINE_FEED) {
                        dst.newLine(lineStart, index, lineWidth, true);
                        lineStart = index + 1;
                        lineWidth = 0.0f;
                        col = 0;
                        prevChar = 0;
                        index++;
                    } else if (c == Text.SPACE) {
                        lineWidth += spaceAdv;
                        col++;
                        prevChar = 0;
                        index++;
                    } else if (c == Text.TAB) {
                        int indents = tabIndents(col);
                        lineWidth += indents * tabAdv;
                        col += indents;
                        prevChar = 0;
                        index++;
                    } else {
                        int nextDelim = text.nextWordDelimiter(index);
                        int wordEnd = (nextDelim == -1) ? textLen : nextDelim;
                        int wordLen = wordEnd - index;
                        float wordWidth = 0.0f;
                        byte wordPrevC = 0;
                        for (int i = index; i < wordEnd; i++) {
                            byte wc = text.get(i);
                            float adv = font.glyph(wc).advance();
                            if (wordPrevC != 0) adv += font.kerning(wordPrevC, wc);
                            wordWidth += adv * scale;
                            wordPrevC = wc;
                        }
                        if (col > 0 && (lineWidth + wordWidth > boundsW)) {
                            dst.newLine(lineStart, index, lineWidth, false);
                            lineStart = index;
                            lineWidth = 0.0f;
                            col = 0;
                            prevChar = 0;
                        }
                        for (int i = 0; i < wordLen; i++) {
                            byte wc = text.get(index);
                            float adv = font.glyph(wc).advance();
                            if (prevChar != 0) adv += font.kerning(prevChar, wc);
                            lineWidth += adv * scale;
                            prevChar = wc;
                            col++;
                            index++;
                        }
                    }
                }
            } else {
                while (index < textLen) {
                    byte c = text.get(index);
                    if (c == Text.LINE_FEED) {
                        dst.newLine(lineStart, index, lineWidth, true);
                        lineStart = index + 1;
                        lineWidth = 0.0f;
                        col = 0;
                        prevChar = 0;
                    } else if (c == Text.SPACE) {
                        lineWidth += spaceAdv;
                        col++;
                        prevChar = 0;
                    } else if (c == Text.TAB) {
                        int indents = tabIndents(col);
                        lineWidth += indents * tabAdv;
                        col += indents;
                        prevChar = 0;
                    } else {
                        float adv = font.glyph(c).advance();
                        if (prevChar != 0) adv += font.kerning(prevChar, c);
                        lineWidth += adv * scale;
                        prevChar = c;
                        col++;
                    } index++;
                }
            }
            dst.newLine(lineStart, textLen, lineWidth, false);
        }

        if (dst.numLines() > 0) {
            float h = (dst.numLines() * font.lineHeight() - font.lineGap);
            dst.setTextHeight(h * scale);
        }
    }

    /**
     * Calculate the font size needed to fill the label container height
     */
    int labelDesiredFontSize(int fontSlot, float boundsHeight) {
        return labelDesiredFontSize(fonts.boundFont(fontSlot),boundsHeight);
    }

    /**
     * Calculate the font size needed to fill the label container height
     */
    int labelDesiredFontSize(Font font, float boundsHeight) {
        return Math.round((boundsHeight / (font.ascent + font.descent)) * font.size);
    }

    /**
     * Adjust pen starting position based on allignment.
     * If the text is wider than the container, allignments defaults to Left.
     * @param bounds the label container
     * @param alignment allignment
     * @param textWidth scaled width of the text
     * @return starting pen x
     */
    float labelAlignX(Rectanglef bounds, TextAlignment alignment, float textWidth) {
        float boundsWidth = bounds.lengthX();
        if (boundsWidth <= textWidth) return bounds.minX;
        return switch (alignment) {
            case LEFT -> bounds.minX;
            case CENTER, RIGHT -> {
                if (alignment == TextAlignment.CENTER) {
                    yield bounds.minX + (boundsWidth - textWidth) * 0.5f;
                }  else yield bounds.minX + boundsWidth - textWidth;
            }
        };
    }

    /**
     * Adjust pen starting position so that the label appears vertically centered.
     * @param bounds the label container
     * @param fontSlot font slot (0 to 4)
     * @param fontSize font size (0 to 255)
     * @return starting pen y
     */
    float labelAlignY(Rectanglef bounds, int fontSlot, int fontSize) {
        Font font = fonts.boundFont(fontSlot);
        return labelAlignY(bounds,font,font.scale(fontSize));
    }

    /**
     * Adjust pen starting position so that the label appears vertically centered.
     * @param bounds the label container
     * @param font font
     * @param fontScale font size scale (fontSize / font.size)
     * @return starting pen y
     */
    float labelAlignY(Rectanglef bounds, Font font, float fontScale) {
        return bounds.minY + 0.5f * (bounds.lengthY() - (font.ascent - font.descent) * fontScale);
    }

    /**
     * Calculates the unscaled width of a "label".
     * A "label" is a single line string of text.
     * LINE_FEED's and TAB's are meassuered as a single SPACE character.
     * @param text text
     * @param fontSlot font slot (0 to 4)
     * @return unscaled width of the label string
     */
    float labelUnscaledWidth(Text text, int fontSlot) {
        return labelUnscaledWidth(text,fonts.boundFont(fontSlot));
    }

    /**
     * Calculates the unscaled width of a "label".
     * A "label" is a single line string of text.
     * LINE_FEED's and TAB's are meassuered as a single SPACE character.
     * @param text text
     * @param font font
     * @return unscaled width of the label string
     */
    float labelUnscaledWidth(Text text, Font font) {
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

    /**
     * Height of the Text (no-wrap) boundary box.
     */
    float textHeight(Text text, int fontSlot, int fontSize) {
        if (text.isEmpty()) return 0;
        Font font = fonts.boundFont(fontSlot);
        return (text.numLines() * font.lineHeight() - font.lineGap) * font.scale(fontSize);
    }


    static int tabIndents(int column) {
        return Text.TAB_LEN - (column % Text.TAB_LEN);
    }

    private Text toText(CharSequence string) {
        if (string instanceof Text text) return text;
        String str = string == null ? "" : string.toString();
        ManagedText text;
        if (str.length() > textBlock.capacity()) {
            text = textBuffer;
        } else text = textBlock;
        text.set(str);
        return text;
    }

    static int fontData(int fontSlot, int fontSize, float glow, boolean outlined) {
        int fontBits = ((fontSize & 0xFF) << 8);
        fontBits |= (fontSlot & 0x07) << 16;
        fontBits |= outlined ? (1 << 19) : 0;
        fontBits |= (glow > 0 ? ((int) (4095f * Math.min(glow, 1.0f))) << 20: 0); // 12 bit
        return fontBits;
    }

    public void free() {
        Disposable.free(textBlock,textBuffer,textBlockTrue,textBlockFalse);
    }
}
