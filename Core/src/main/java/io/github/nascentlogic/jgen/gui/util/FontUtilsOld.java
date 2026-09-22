package io.github.nascentlogic.jgen.gui.util;

import io.github.nascentlogic.jgen.gui.Font;
import io.github.nascentlogic.jgen.gui.text.Text;
import org.joml.Vector2f;

/**
 * F.Dahl, 8/31/2026
 */
public class FontUtilsOld {




    /**
     * Measures the text buffer and populates the provided layout result with unscaled metrics and line indices.
     *
     * @param text           Normalized ASCII text buffer
     * @param font           Font metrics and glyph table
     * @param containerWidth Maximum width allowed per line before wrapping (in actual pixels)
     * @param fontSize       Target font size in pixels
     * @param wordWrap       Whether word wrapping is enabled
     * @param dstLayout      Output layout container to populate
     */
    public static void calculateLayout(Text text, Font font, float containerWidth, float fontSize, boolean wordWrap, TextLayout dstLayout) {
        dstLayout.reset();

        int textLength = text.length();
        if (textLength == 0 || containerWidth <= 0 || fontSize == 0) {
            return;
        }

        final float scale = fontSize / font.size;
        final float unscaledContainerWidth = containerWidth / scale;
        final float spaceAdvance = font.glyph(Text.SPACE).advance();
        final float tabAdvance = font.indentAdvance();
        final int tabSize = 4;

        float currentLineWidth = 0f;
        int currentColumn = 0;
        int lineStart = 0;
        int i = 0;

        if (font.monospaced) {
            if (wordWrap) {
                // Branch 1 - Monospaced + Wrap
                while (i < textLength) {
                    byte c = text.get(i);
                    if (c == Text.LINE_FEED) {
                        dstLayout.addLine(lineStart, i, currentLineWidth, true);
                        currentLineWidth = 0f;
                        currentColumn = 0;
                        lineStart = i + 1;
                        i++;
                    } else if (c == Text.SPACE) {
                        currentLineWidth += spaceAdvance;
                        currentColumn++;
                        i++;
                    } else if (c == Text.TAB) {
                        int indents = tabIndents(currentColumn, tabSize);
                        currentLineWidth += indents * tabAdvance;
                        currentColumn += indents;
                        i++;
                    } else {
                        // Contiguous word block (graphical characters)
                        int nextDelim = text.nextWordDelimiter(i);
                        int wordEnd = (nextDelim == -1) ? textLength : nextDelim;
                        int wordLength = wordEnd - i;
                        float wordWidth = wordLength * spaceAdvance;

                        // Wrap if the word overflows and we're not at column 0
                        if (currentColumn > 0 && (currentLineWidth + wordWidth > unscaledContainerWidth)) {
                            dstLayout.addLine(lineStart, i, currentLineWidth, false);
                            currentLineWidth = 0f;
                            currentColumn = 0;
                            lineStart = i;
                        }
                        currentLineWidth += wordWidth;
                        currentColumn += wordLength;
                        i = wordEnd; // Advance pointer directly to next delimiter
                    }
                }
            } else {
                // Branch 2 - Monospaced + No Wrap
                while (i < textLength) {
                    byte c = text.get(i);
                    if (c == Text.LINE_FEED) {
                        dstLayout.addLine(lineStart, i, currentLineWidth, true);
                        currentLineWidth = 0f;
                        currentColumn = 0;
                        lineStart = i + 1;
                    } else if (c == Text.SPACE) {
                        currentLineWidth += spaceAdvance;
                        currentColumn++;
                    } else if (c == Text.TAB) {
                        int indents = tabIndents(currentColumn, tabSize);
                        currentLineWidth += indents * tabAdvance;
                        currentColumn += indents;
                    } else {
                        currentLineWidth += spaceAdvance;
                        currentColumn++;
                    }
                    i++;
                }
            }
        } else {
            final int NULL_CHAR = 0;
            byte prevChar = NULL_CHAR;
            if (wordWrap) {
                // Branch 4 - Proportional + Wrap
                while (i < textLength) {
                    byte c = text.get(i);
                    if (c == Text.LINE_FEED) {
                        dstLayout.addLine(lineStart, i, currentLineWidth, true);
                        currentLineWidth = 0f;
                        currentColumn = 0;
                        prevChar = NULL_CHAR;
                        lineStart = i + 1;
                        i++;
                    } else if (c == Text.SPACE) {
                        currentLineWidth += spaceAdvance;
                        currentColumn++;
                        prevChar = NULL_CHAR;
                        i++;
                    } else if (c == Text.TAB) {
                        int indents = tabIndents(currentColumn, tabSize);
                        currentLineWidth += indents * tabAdvance;
                        currentColumn += indents;
                        prevChar = NULL_CHAR;
                        i++;
                    } else {
                        int nextDelim = text.nextWordDelimiter(i);
                        int wordEnd = (nextDelim == -1) ? textLength : nextDelim;

                        float wordWidth = 0f;
                        byte wPrev = prevChar; // NULL_CHAR when coming off whitespace, newlines, or a soft wrap
                        for (int w = i; w < wordEnd; w++) {
                            byte wc = text.get(w);
                            float adv = font.glyph(wc).advance();
                            if (wPrev != NULL_CHAR) {
                                adv += font.kerning(wPrev, wc);
                            }
                            wordWidth += adv;
                            wPrev = wc;
                        }

                        int wordLength = wordEnd - i;

                        // Wrap if word overflows current line
                        if (currentColumn > 0 && (currentLineWidth + wordWidth > unscaledContainerWidth)) {
                            dstLayout.addLine(lineStart, i, currentLineWidth, false);
                            currentLineWidth = 0f;
                            currentColumn = 0;
                            lineStart = i;
                            prevChar = NULL_CHAR; // Guardrail: Ensures line boundaries always break kerning pairs
                        }

                        currentLineWidth += wordWidth;
                        currentColumn += wordLength;
                        prevChar = wPrev; // Carry trailing char of word forward for next token on the same line
                        i = wordEnd;
                    }
                }
            } else {
                // Branch 3 - Proportional + No Wrap
                while (i < textLength) {
                    byte c = text.get(i);
                    if (c == Text.LINE_FEED) {
                        dstLayout.addLine(lineStart, i, currentLineWidth, true);
                        currentLineWidth = 0f;
                        currentColumn = 0;
                        prevChar = NULL_CHAR;
                        lineStart = i + 1;
                    } else if (c == Text.SPACE) {
                        currentLineWidth += spaceAdvance;
                        currentColumn++;
                        prevChar = NULL_CHAR;
                    } else if (c == Text.TAB) {
                        int indents = tabIndents(currentColumn, tabSize);
                        currentLineWidth += indents * tabAdvance;
                        currentColumn += indents;
                        prevChar = NULL_CHAR;
                    } else {
                        float adv = font.glyph(c).advance();
                        if (prevChar != NULL_CHAR) {
                            adv += font.kerning(prevChar, c);
                        }
                        currentLineWidth += adv;
                        currentColumn++;
                        prevChar = c;
                    }
                    i++;
                }
            }
        }

        // Push trailing un-emitted line
        if (lineStart <= textLength) {
            dstLayout.addLine(lineStart, textLength, currentLineWidth, false);
        }

        // Calculate unscaled tight height
        if (dstLayout.lineCount > 0) {
            dstLayout.height = (dstLayout.lineCount - 1) * font.lineHeight() + font.ascent + font.descent;
        }
    }

    /**
     * Calculates the height, line count, and tight bounding box of a Text buffer.
     * Performs calculations in unscaled font space to minimize multiplications.
     * @param text           Normalized ASCII text buffer
     * @param font           Font metrics and glyph table
     * @param containerWidth Maximum width allowed per line before wrapping
     * @param fontSize       Target font size (scales advances against font.size)
     * @param wordWrap       Whether word wrapping is enabled
     * @param dstBounds      Output vector: x = max line width (px), y = tight height (px)
     * @return total line count
     */
    public static int height(Text text, Font font, float containerWidth, float fontSize, boolean wordWrap, Vector2f dstBounds) {
        int textLength = text.length();
        if (textLength == 0 || containerWidth <= 0 || fontSize == 0) {
            dstBounds.zero();
            return 0;
        }

        final float scale = fontSize / font.size;
        final float unscaledContainerWidth = containerWidth / scale;
        final float spaceAdvance = font.glyph(Text.SPACE).advance();
        final float tabAdvance = font.indentAdvance();
        final int tabSize = 4;

        float currentLineWidth = 0f;
        float maxLineWidth = 0f;
        int currentColumn = 0;
        int lineCount = 1;
        int i = 0;

        if (font.monospaced) {
            if (wordWrap) {
                // Branch 1 - Monospaced + Wrap
                while (i < textLength) {
                    byte c = text.get(i);
                    if (c == Text.LINE_FEED) {
                        maxLineWidth = Math.max(maxLineWidth, currentLineWidth);
                        currentLineWidth = 0f;
                        currentColumn = 0;
                        lineCount++;
                        i++;
                    } else if (c == Text.SPACE) {
                        currentLineWidth += spaceAdvance;
                        currentColumn++;
                        i++;
                    } else if (c == Text.TAB) {
                        int indents = tabIndents(currentColumn, tabSize);
                        currentLineWidth += indents * tabAdvance;
                        currentColumn += indents;
                        i++;
                    } else {
                        // Contiguous word block (graphical characters)
                        int nextDelim = text.nextWordDelimiter(i);
                        int wordEnd = (nextDelim == -1) ? textLength : nextDelim;
                        int wordLength = wordEnd - i;
                        float wordWidth = wordLength * spaceAdvance;
                        // Wrap if the word overflows and we're not at column 0
                        if (currentColumn > 0 && (currentLineWidth + wordWidth > unscaledContainerWidth)) {
                            maxLineWidth = Math.max(maxLineWidth, currentLineWidth);
                            currentLineWidth = 0f;
                            currentColumn = 0;
                            lineCount++;
                        }
                        currentLineWidth += wordWidth;
                        currentColumn += wordLength;
                        i = wordEnd; // Advance pointer directly to next delimiter
                    }
                }
            } else {
                // Branch 2 - Monospaced + No Wrap
                while (i < textLength) {
                    byte c = text.get(i);
                    if (c == Text.LINE_FEED) {
                        maxLineWidth = Math.max(maxLineWidth, currentLineWidth);
                        currentLineWidth = 0f;
                        currentColumn = 0;
                        lineCount++;
                    } else if (c == Text.SPACE) {
                        currentLineWidth += spaceAdvance;
                        currentColumn++;
                    } else if (c == Text.TAB) {
                        int indents = tabIndents(currentColumn, tabSize);
                        currentLineWidth += indents * tabAdvance;
                        currentColumn += indents;
                    } else {
                        currentLineWidth += spaceAdvance;
                        currentColumn++;
                    }
                    i++;
                }
            }
        } else {
            // final float TAB_INTERVAL = tabSize * tabAdvance; // later
            final int NULL_CHAR = 0;
            byte prevChar = NULL_CHAR;
            if (wordWrap) {
                // Branch 4 - Proportional + Wrap
                while (i < textLength) {
                    byte c = text.get(i);
                    if (c == Text.LINE_FEED) {
                        maxLineWidth = Math.max(maxLineWidth, currentLineWidth);
                        currentLineWidth = 0f;
                        currentColumn = 0;
                        prevChar = NULL_CHAR;
                        lineCount++;
                        i++;
                    } else if (c == Text.SPACE) {
                        currentLineWidth += spaceAdvance;
                        currentColumn++;
                        prevChar = NULL_CHAR;
                        i++;
                    } else if (c == Text.TAB) {

                        // later
                        // currentLineWidth += tabAdvance(currentLineWidth, TAB_INTERVAL);
                        // currentColumn++;
                        // prevChar = NULL_CHAR;
                        // i++;

                        int indents = tabIndents(currentColumn, tabSize);
                        currentLineWidth += indents * tabAdvance;
                        currentColumn += indents;
                        prevChar = NULL_CHAR;
                        i++;

                    } else {
                        // Scan and measure proportional word block with kerning
                        int nextDelim = text.nextWordDelimiter(i);
                        int wordEnd = (nextDelim == -1) ? textLength : nextDelim;
                        float wordWidth = 0f;
                        byte wordPrevChar = prevChar;
                        for (int w = i; w < wordEnd; w++) {
                            byte wc = text.get(w);
                            float adv = font.glyph(wc).advance();
                            if (wordPrevChar != NULL_CHAR) {
                                adv += font.kerning(wordPrevChar, wc);
                            }
                            wordWidth += adv;
                            wordPrevChar = wc;
                        }

                        int wordLength = wordEnd - i;

                        // Wrap if the word overflows and we're not at column 0
                        if (currentColumn > 0 && (currentLineWidth + wordWidth > unscaledContainerWidth)) {
                            maxLineWidth = Math.max(maxLineWidth, currentLineWidth);
                            currentLineWidth = 0f;
                            currentColumn = 0;
                            lineCount++;
                        }
                        currentLineWidth += wordWidth;
                        currentColumn += wordLength;
                        prevChar = wordPrevChar; // Carry last char of word forward
                        i = wordEnd; // Advance pointer directly to next delimiter
                    }
                }
            } else {
                // Branch 3 - Proportional + No Wrap
                while (i < textLength) {
                    byte c = text.get(i);
                    if (c == Text.LINE_FEED) {
                        maxLineWidth = Math.max(maxLineWidth, currentLineWidth);
                        currentLineWidth = 0f;
                        currentColumn = 0;
                        prevChar = NULL_CHAR;
                        lineCount++;
                    } else if (c == Text.SPACE) {
                        currentLineWidth += spaceAdvance;
                        currentColumn++;
                        prevChar = NULL_CHAR;
                    } else if (c == Text.TAB) {
                        // later
                        // currentLineWidth += tabAdvance(currentLineWidth, TAB_INTERVAL);
                        // currentColumn++;
                        // prevChar = NULL_CHAR;
                        int indents = tabIndents(currentColumn, tabSize);
                        currentLineWidth += indents * tabAdvance;
                        currentColumn += indents;
                        prevChar = NULL_CHAR;
                    } else {
                        float adv = font.glyph(c).advance();
                        if (prevChar != NULL_CHAR) {
                            adv += font.kerning(prevChar, c);
                        } currentLineWidth += adv;
                        currentColumn++;
                        prevChar = c;
                    }
                    i++;
                }
            }
        }
        maxLineWidth = Math.max(maxLineWidth,currentLineWidth);
        // Tight vertical bounds in unscaled space:
        // (lineCount - 1) * lineHeight + ascent + descent
        float unscaledTightHeight = (lineCount - 1) * font.lineHeight() + font.ascent + font.descent;
        float finalWidth = maxLineWidth * scale;
        float finalHeight = unscaledTightHeight * scale;
        dstBounds.set(finalWidth, finalHeight);
        return lineCount;
    }


    /**
     * Calculates the tab indents required to align to the next tab stop.
     * @param currentColumn current column index (0-based)
     * @param tabSize number of columns per tab stop (e.g., 4)
     * @return indents to add
     */
    public static int tabIndents(int currentColumn, int tabSize) {
        return tabSize - (currentColumn % tabSize);
    }


    /**
     * Stores unscaled structural metrics and visual line boundaries resulting from a layout pass.
     * <p>
     * Decoupled from alignment so that text can be aligned dynamically at render time
     * without re-running measurement loops.
     * </p>
     */
    public static final class TextLayout { // todo iterable
        public float width;
        public float height;
        public int lineCount;
        private Line[] lines = new Line[16];
        public TextLayout() {
            for (int i = 0; i < lines.length; i++)
                lines[i] = new Line();
        }
        public void reset() {
            width = 0.0f;
            height = 0.0f;
            lineCount = 0;
        }
        public void addLine(int startIndex, int endIndex, float lineWidth, boolean isHardBreak) {
            ensureCapacity(lineCount + 1);
            lines[lineCount].set(startIndex, endIndex, lineWidth, isHardBreak);
            width = Math.max(width,lineWidth);
            lineCount++;
        }

        /** Retrieves the line metrics at the given visual index. */
        public Line getLine(int lineIndex) {
            return lines[lineIndex];
        }

        /**
         * Computes the scaled horizontal pixel offset for rendering or hit-testing a line.
         * @param lineIndex      target line index
         * @param containerWidth actual container width in pixels
         * @param scale          font scale factor (fontSize / font.size)
         * @param alignment      target text alignment
         * @return scaled X offset in pixels
         */
        public float getLineXOffset(int lineIndex, float containerWidth, float scale, TextAlignment alignment) {
            float scaledLineWidth = lines[lineIndex].width * scale;
            return switch (alignment) {
                case LEFT   -> 0.0f;
                case CENTER -> (containerWidth - scaledLineWidth) * 0.5f;
                case RIGHT  -> containerWidth - scaledLineWidth;
            };
        }

        /** Maps a character buffer index to its corresponding visual line index. */
        public int getLineForIndex(int charIndex) {
            if (lineCount == 0) throw new IndexOutOfBoundsException("Layout contains no lines (lineCount == 0)");
            for (int i = 0; i < lineCount; i++) {
                if (lines[i].containsIndex(charIndex)) return i;
            } // Tail edge case: caret positioned at the exact end of text
            Line lastLine = lines[lineCount - 1];
            if (charIndex == lastLine.endIndex) return lineCount - 1;
            throw new IndexOutOfBoundsException("Character index " + charIndex + " is outside layout bounds");
        }

        private void ensureCapacity(int capacity) {
            if (lines.length < capacity) {
                int newCap = Math.max(lines.length * 2, capacity);
                Line[] newLines = new Line[newCap];
                System.arraycopy(lines, 0, newLines, 0, lineCount);
                for (int i = lineCount; i < newCap; i++) {
                    newLines[i] = new Line();
                } lines = newLines;
            }
        }
    }



    /** Single visual line entry within the layout result. */
    public static final class Line {
        /** Index in the text buffer where this visual line begins (inclusive). */
        public int startIndex;
        /** Index in the text buffer where this visual line ends (exclusive).
         * For soft wraps, this matches the next line's startIndex.
         * For hard breaks ('\n'), this points to the newline character.*/
        public int endIndex;
        /** Unscaled horizontal width of this line. */
        public float width;
        /** True if this line was terminated by an explicit '\n'. */
        public boolean isHardBreak;
        public void set(int startIndex, int endIndex, float width, boolean isHardBreak) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.width = width;
            this.isHardBreak = isHardBreak;
        } /** Checks if a character index belongs to this line using standard downstream affinity. */
        public boolean containsIndex(int charIndex) {
            if (startIndex == endIndex) return charIndex == startIndex; // Empty line (\n\n)
            if (isHardBreak) return charIndex >= startIndex && charIndex <= endIndex;
            // Soft wrap half-open interval [startIndex, endIndex)
            return charIndex >= startIndex && charIndex < endIndex;
        }
    }

}
