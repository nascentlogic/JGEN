package io.github.nascentlogic.jgen.gui.text;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * F.Dahl, 8/15/2026
 */
public final class TextView implements UnmanagedText {

    final byte[] array;
    final int pos;
    final int len;

    // =============================================================================
    // Constructors
    // =============================================================================

    /**
     * Package private initializer.
     * @param array Normalized ascii array.
     */
    TextView(byte[] array) {
        this(array, 0, array.length);
    }

    /**
     * Package private initializer.
     * @param array Normalized ascii array.
     * @param pos positional offset.
     * @param len content length.
     */
    TextView(byte[] array, int pos, int len) {
        Objects.checkFromIndexSize(pos, len, array.length);
        this.array = array;
        this.pos = pos;
        this.len = len;
    }

    // =============================================================================
    // Text
    // =============================================================================

    @Override
    public byte get(int index) {
        return array[pos + index];
    }

    @Override
    public ByteBuffer readBuffer() {
        return ByteBuffer.wrap(array).slice(pos, len).asReadOnlyBuffer();
    }

    // =============================================================================
    // CharSequence
    // =============================================================================

    @Override
    public int length() {
        return len;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, len);
        if (start == 0 && end == len) return this;
        return new TextView(array, pos + start, end - start);
    }

    @Override
    public String toString() {
        return new String(array, pos, len, StandardCharsets.US_ASCII);
    }

}
