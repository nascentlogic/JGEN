package io.github.nascentlogic.jgen.gui.text;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * F.Dahl, 8/16/2026
 */
public class TextBlock extends ManagedText {

    private int len;

    // =============================================================================
    // Constructors
    // =============================================================================

    /** allocates 32 byte */
    public TextBlock() {
        allocate(ByteArena.MIN_BLOCK_SIZE);
    }

    public TextBlock(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("capacity < 0");
        allocate(capacity);
    }

    public TextBlock(CharSequence str) {
        Objects.requireNonNull(str);
        allocate(str.length());
        len = Text.normalize(str,memory(),arenaOffset());
    }

    public TextBlock(byte[] array) {
        Objects.requireNonNull(array);
        allocate(array.length);
        len = Text.normalize(array,memory(),arenaOffset());
    }

    // =============================================================================
    // TextBlock Exclusive
    // =============================================================================

    /**
     * Converts {@code this} into a {@link TextBuffer}, essentially transfering
     * the arena memory block from {@code this} onto the TextBuffer.
     * Leaving {@code this} effectively disposed.<p>
     * {@code gapStart} is set to {@code lenght()} and
     * {@code gapEnd} is set to {@code capacity()}.<p>
     * @return {@code this} as a {@code TextBuffer}.
     * @throws IllegalStateException If already disposed / freed
     */
    public TextBuffer toTextBuffer() {
        if (isDisposed()) throw new IllegalStateException("TextBlock is disposed");
        TextBuffer buffer = new TextBuffer(arenaOffset,blockSize,len);
        blockSize = 0; // signals to the outside that this is disposed
        arenaOffset = 0; // does not matter
        len = 0;
        return buffer;
    }

    public int setInt(int value) {
        len = Text.insertIntDigits(value,memory(),arenaOffset());
        return len;
    }

    public int setFloat(double value, int decimals) {
        len = Text.insertFloatDigits(value,decimals,memory(),arenaOffset());
        return len;
    }


    // =============================================================================
    // ManagedText
    // =============================================================================

    @Override
    public boolean set(byte c, int index) {
        Objects.checkIndex(index, len);
        if (Text.isValidInternalFormat(c)) {
            setByte(index, c);
            return true;
        } return false;
    }

    @Override
    public int set(CharSequence str) {
        if (isDisposed()) throw new IllegalStateException("TextBlock is disposed");
        Objects.requireNonNull(str);
        if (str == this) return len;
        int strLen = str.length();
        if (strLen == 0) { clear();
            return 0;
        } if (strLen > capacity()) {
            throw new BufferOverflowException();
        } // Allocate temporary buffer ONLY if view overlaps the exact bytes we will overwrite
        if (str instanceof TextView view && isOverlappingWriteTarget(view, strLen)) {
            byte[] tmp = new byte[strLen];
            int tmpLen = Text.normalize(str, tmp, 0);
            uncheckedWrite(tmp, 0, 0, tmpLen);
            this.len = tmpLen;
        } else { // Direct zero-allocation pass
            this.len = Text.normalize(str, memory(), arenaOffset());
        } return len;
    }

    @Override
    public void clear() { len = 0; }

    /**
     * Checks whether a TextView overlaps with the specific memory range
     * that will be overwritten during set(CharSequence).
     * Assumes view != null and writeLength > 0 (guaranteed by set()).
     */
    private boolean isOverlappingWriteTarget(TextView view, int writeLength) {
        assert view != null && writeLength > 0;
        if (view.array != memory()) return false;
        int dstStart = arenaOffset();
        int dstEnd   = dstStart + writeLength;
        int srcStart = view.pos;
        int srcEnd   = srcStart + view.len;
        return Text.rangeOverlap(dstStart, dstEnd, srcStart, srcEnd);
    }

    // =============================================================================
    // ManagedBuffer
    // =============================================================================

    @Override
    protected void onBlockChange(int oldPos, int oldSize, int newPos, int newSize) {
        // Sanity check. Should never occur.
        throw new UnsupportedOperationException("Text blocks are fixed size and cannot be resized or moved");
    }

    @Override
    protected void onFree() { len = 0; } // crucial to reflect that the buffer is empty

    // =============================================================================
    // Text
    // =============================================================================

    @Override
    public byte get(int index) {
        return getByte(index);
    }

    @Override
    public ByteBuffer readBuffer() {
        if (isDisposed()) throw new IllegalStateException("TextBlock is disposed");
        return ByteBuffer.wrap(memory()).slice(arenaOffset(), len).asReadOnlyBuffer();
    }

    // =============================================================================
    // CharSeuence
    // =============================================================================

    @Override
    public int length() { return len; }

    @Override
    public CharSequence subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, len);
        if (start == 0 && end == len) return this;
        return new TextView(memory(), arenaOffset() + start, end - start);
    }

    @Override
    public String toString() {
        return new String(memory(), arenaOffset(), len, StandardCharsets.US_ASCII);
    }
}
