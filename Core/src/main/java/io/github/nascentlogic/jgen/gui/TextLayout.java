package io.github.nascentlogic.jgen.gui;

import io.github.nascentlogic.jgen.gui.api.TextAlignment;
import org.joml.primitives.Rectanglef;

import java.util.Objects;

/**
 * F.Dahl, 10/3/2026
 */
public class TextLayout {

    private float width;
    private float height;
    /** number of visual content lines
     + (1 if text ends with '\n', else 0) */
    private int numLines;
    private int fontSlot;
    private int fontSize;
    private Line[] lines;
    private Rectanglef container;


    public TextLayout() {
        lines = new Line[256];
        for (int i = 0; i < lines.length; i++) {
            lines[i] = new Line();
        } container = new Rectanglef();
    }

    void reset(Rectanglef bounds, int fontSlot, int fontSize) {
        this.fontSlot = fontSlot;
        this.fontSize = fontSize;
        this.numLines = 0;
        this.width = 0;
        this.height = 0;
        this.container.set(bounds);
    }

    public Line getLine(int line) { return lines[Objects.checkIndex(line,numLines)]; }
    public int lineIndexOf(int charIndex) {
        if (numLines == 0) throw new IndexOutOfBoundsException("Empty TextLayout");
        for (int i = 0; i < numLines; i++) {
            if (lines[i].containsIndex(charIndex)) return i;
        } throw new IndexOutOfBoundsException("Character index: " + charIndex + ", is outside TextLayout bounds");
    }
    public void newLine(int fromIndex, int toIndex, float width, boolean hardBreak) {
        ensureCapacity(numLines + 1);
        lines[numLines].set(fromIndex, toIndex, width, hardBreak);
        this.width = Math.max(this.width,width);
        numLines++;
    }

    public void setTextHeight(float height) { this.height = height; }
    public float textHeight() { return height; }
    public float textWidth() { return width; }
    public int fontSlot() { return fontSlot; }
    public int fontSize() { return fontSize; }
    public int numLines() { return numLines; }
    public Rectanglef container() { return container; }

    private void ensureCapacity(int capacity) {
        if (lines.length < capacity) {
            int newCap = Math.max(lines.length * 2, capacity);
            Line[] newLines = new Line[newCap];
            System.arraycopy(lines, 0, newLines, 0, numLines);
            for (int i = numLines; i < newCap; i++) {
                newLines[i] = new Line();
            } lines = newLines;
        }
    }


    public static final class Line {
        /** Index in the text buffer where this visual line begins (inclusive). */
        public int fromIndex;
        /** Index in the text buffer where this visual line ends (exclusive).
         * For soft wraps, this matches the next line's startIndex.
         * For hard breaks ('\n'), this points to the newline character.*/
        public int toIndex;
        /** Scaled width of the line */
        public float width;
        /** True if this line was terminated by an explicit '\n'. */
        public boolean isHardBreak;
        public void set(int from, int to, float width, boolean hardBreak) {
            fromIndex = from; toIndex = to;
            this.width = width; isHardBreak = hardBreak;
        } /** Checks if a character index belongs to this line using standard downstream affinity. */
        public boolean containsIndex(int index) {
            if (fromIndex == toIndex) return index == fromIndex; // Empty line (\n\n)
            if (isHardBreak) return index >= fromIndex && index <= toIndex;
            return index >= fromIndex && index < toIndex;
        } public float xOffset(float boundsWidth, TextAlignment alignment) {
            return switch (alignment) {
                case LEFT   -> 0.0f;
                case CENTER -> (boundsWidth - width) * 0.5f;
                case RIGHT  -> (boundsWidth - width);
            };
        }
    }
}
