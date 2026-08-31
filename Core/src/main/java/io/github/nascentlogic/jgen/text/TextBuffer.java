package io.github.nascentlogic.jgen.text;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * F.Dahl, 8/16/2026
 */
public class TextBuffer extends ManagedText {

    private int gapStart; // cursor
    private int gapEnd;

    // =============================================================================
    // Constructors
    // =============================================================================

    public TextBuffer() { this(0); }

    public TextBuffer(int initialCapacity) {
        if (initialCapacity < 0) throw new IllegalArgumentException("capacity < 0");
        allocate(initialCapacity);
        gapStart = 0;
        gapEnd = capacity();
    }

    public TextBuffer(CharSequence str) {
        Objects.requireNonNull(str);
        allocate(str.length());
        gapStart = Text.normalize(str,memory(),arenaOffset());
        gapEnd = capacity();
    }

    public TextBuffer(byte[] array) {
        Objects.requireNonNull(array);
        allocate(array.length);
        gapStart = Text.normalize(array,memory(),arenaOffset());
        gapEnd = capacity();
    }

    /**
     * Package private initializer.
     * Used when converting {@link TextBlock} to {@code TextBuffer}.
     * @see TextBlock#toTextBuffer()
     * @param arenaOffset
     * @param blockSize
     * @param gapStart
     */
    TextBuffer(int arenaOffset, int blockSize, int gapStart) {
        this.arenaOffset = arenaOffset;
        this.blockSize = blockSize;
        this.gapStart = gapStart;
        this.gapEnd = blockSize;
    }

    // =============================================================================
    // TextBuffer Cursor
    // =============================================================================

    public int cursor() {
        return gapStart;
    }

    public void setCursor(int index) {
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length());
        } if (index == gapStart) return;
        if (index < gapStart) {
            // Moving cursor left: shift bytes from left of gap to right of gap
            int count = gapStart - index;
            moveLocal(index, gapEnd - count, count);
            gapStart -= count;
            gapEnd -= count;
        } else { // Moving cursor right: shift bytes from right of gap to left of gap
            int count = index - gapStart;
            moveLocal(gapEnd, gapStart, count);
            gapStart += count;
            gapEnd += count;
        }
    }

    public void moveCursor(int delta) {
        if (delta == 0) return;
        setCursor(gapStart + delta);
    }

    public void moveCursorForward() {
        setCursor(gapStart + 1);
    }

    public void moveCursorBackward() {
        setCursor(gapStart - 1);
    }

    public void moveCursorStart() {
        setCursor(0);
    }

    public void moveCursorEnd() {
        setCursor(length());
    }

    // =============================================================================
    // TextBuffer Insert
    // =============================================================================

    public int insert(byte c) {
        return insert(c,gapStart);
    }

    public int insert(byte c, int index) {
        int len = length();
        Objects.checkIndex(index, len + 1);
        if (!Text.isValidInternalFormat(c)) return 0;
        ensureCapacity(len + 1);
        setCursor(index);
        setByte(gapStart, c);
        gapStart++;
        return 1;
    }

    public int insert(CharSequence str) {
        return insert(str,gapStart);
    }

    public int insert(CharSequence str, int index) {
        return insert(str,0,str.length(),index);
    }

    public int insert(CharSequence str, int srcFrom, int srcTo, int index) {
        return insert(str,srcFrom,srcTo,index,index);
    }

    // insert can be optimized to avoid staging (in some cases) later
    public int insert(CharSequence str, int srcFrom, int srcTo, int dstFrom, int dstTo) {
        final int currentLen = length();
        Objects.requireNonNull(str);
        Objects.checkFromToIndex(srcFrom, srcTo, str.length());
        Objects.checkFromToIndex(dstFrom, dstTo, currentLen);
        final int insertLen = Text.normalizedLength(str, srcFrom, srcTo);
        final int replaceLen = dstTo - dstFrom;
        if (insertLen == 0 && replaceLen == 0) {
            setCursor(dstFrom);
            return 0;
        } byte[] stagingBuffer = new byte[insertLen];
        if (Text.normalize(str, srcFrom, srcTo, stagingBuffer, 0) != insertLen) {
            // this should never occur. Just in case I decide to refactor in the future.
            throw new IllegalStateException("normalize does not match normalizedLength");
        } ensureCapacity(currentLen - replaceLen + insertLen);
        setCursor(dstFrom);
        gapEnd += replaceLen;
        if (insertLen > 0) {
            uncheckedWrite(stagingBuffer, 0, gapStart, insertLen);
            gapStart += insertLen;
        } return insertLen - replaceLen;
    }

    // =============================================================================
    // TextBuffer Delete
    // =============================================================================

    /**
     * Deletes the character sequence in range [from, to).
     * @return number of characters deleted.
     */
    public int delete(int from, int to) {
        Objects.checkFromToIndex(from, to, length());
        int deleteLen = to - from;
        if (deleteLen == 0) return 0;
        setCursor(from);
        gapEnd += deleteLen;
        return deleteLen;
    }

    /**
     * Deletes the single character at the specified index.
     * @return 1 if deleted.
     */
    public int delete(int index) {
        return delete(index, index + 1);
    }

    /**
     * Deletes the character immediately after the current cursor position.
     * @return 1 if deleted.
     * @throws IndexOutOfBoundsException if cursor is at the end of the buffer.
     */
    public int deleteForward() {
        if (gapStart >= length()) {
            throw new IndexOutOfBoundsException("Cannot delete forward at end of buffer (length: " + length() + ")");
        } gapEnd++; // Swallows the character at physical index gapEnd into the gap
        return 1;
    }

    /**
     * Deletes the character immediately before the current cursor position.
     * @return 1 if deleted.
     * @throws IndexOutOfBoundsException if cursor is at the start of the buffer.
     */
    public int deleteBackward() {
        if (gapStart == 0) {
            throw new IndexOutOfBoundsException("Cannot delete backward at start of buffer");
        } gapStart--;
        return 1;
    }

    // =============================================================================
    // ManagedText
    // =============================================================================

    @Override
    public boolean set(byte c, int index) {
        final int gapSize = gapEnd - gapStart;
        final int len = capacity() - gapSize;
        Objects.checkIndex(index, len);
        if (Text.isValidInternalFormat(c)) {
            int idx = (index < gapStart) ? index : index + gapSize;
            setByte(idx, c);
            return true;
        } return false;
    }

    @Override
    public int set(CharSequence str) { clear();
        return insert(str);
    }

    @Override
    public void clear() {
        gapStart = 0;
        gapEnd = capacity();
    }

    // =============================================================================
    // ManagedBuffer
    // =============================================================================

    @Override
    protected void onBlockChange(int oldPos, int oldSize, int newPos, int newSize) {
        if (oldPos == newPos && oldSize == newSize) return; // should never occur
        int leftLen   = gapStart;
        int rightLen  = oldSize - gapEnd;
        int newGapEnd = newSize - rightLen;
        // Right Payload: Always moves to the new tail (newPos + newGapEnd)
        if (rightLen > 0) moveGlobal(oldPos + gapEnd, newPos + newGapEnd, rightLen);
        // Left Payload: Only needs to move if the block's base offset shifted
        if (leftLen > 0 && oldPos != newPos) moveGlobal(oldPos, newPos, leftLen);
        gapEnd = newGapEnd;
    }

    @Override
    protected void onFree() {
        gapStart = 0;
        gapEnd = gapStart;
    }

    // =============================================================================
    // Text
    // =============================================================================

    @Override
    public byte get(int index) {
        int idx = (index < gapStart) ? index : index + (gapEnd - gapStart);
        return getByte(idx);
    }

    @Override
    public ByteBuffer readBuffer() {
        if (isDisposed()) throw new IllegalStateException("TextBuffer is disposed");
        final int len = length();
        if (len == 0) return Text.EMPTY_TEXT.readBuffer();
        final int gapSize = gapEnd - gapStart;
        // Fast Path 1: Gap is at the end (all content left of gap)
        if (gapStart == len) return ByteBuffer.wrap(memory()).slice(arenaOffset(), len).asReadOnlyBuffer();
        // Fast Path 2: Gap is at the start (all content right of gap)
        if (gapStart == 0) return ByteBuffer.wrap(memory()).slice(arenaOffset() + gapSize, len).asReadOnlyBuffer();
        // Fallback Path: Gap splits the content. Materialize into a contiguous buffer.
        byte[] dst = new byte[len];
        uncheckedRead(0, dst, 0, gapStart);
        uncheckedRead(gapEnd, dst, gapStart, len - gapStart);
        return ByteBuffer.wrap(dst).asReadOnlyBuffer();
    }

    // =============================================================================
    // CharSeuence
    // =============================================================================

    @Override
    public int length() {
        return capacity() - (gapEnd - gapStart);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (isDisposed()) throw new IllegalStateException("TextBuffer is disposed");
        final int len = length();
        Objects.checkFromToIndex(start, end, len);
        if (start == 0 && end == len) return this;
        final int subLen = end - start;
        if (subLen == 0) return Text.EMPTY_TEXT;
        // Case 1: Entirely to the left of the gap
        if (end <= gapStart) {
            return new TextView(memory(), arenaOffset() + start, subLen);
        } // Case 2: Entirely to the right of the gap
        if (start >= gapStart) {
            int gapSize = gapEnd - gapStart;
            return new TextView(memory(), arenaOffset() + start + gapSize, subLen);
        } // Case 3: Spans across the gap -> materialise into ImmutableText
        int leftLen = gapStart - start;
        int rightLen = end - gapStart;
        byte[] dst = new byte[subLen];
        uncheckedRead(start, dst, 0, leftLen);
        uncheckedRead(gapEnd, dst, leftLen, rightLen);
        return new ImmutableText(dst);
    }

    @Override
    public String toString() {
        int len = length();
        if (len == 0) return "";
        byte[] tmp = new byte[len];
        int leftLen = gapStart;
        int rightLen = len - leftLen;
        uncheckedRead(0, tmp, 0, leftLen);
        uncheckedRead(gapEnd, tmp, leftLen, rightLen);
        return new String(tmp, 0, len, StandardCharsets.US_ASCII);
    }
}
