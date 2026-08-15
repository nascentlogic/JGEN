package io.github.nascentlogic.jgen.utils.arena;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * F.Dahl, 8/15/2026
 */
public class TextView implements UnmanagedText {

    final byte[] array;
    final int pos;
    final int len;

    /**
     * Package private initializer.
     * @param array Normalized ascii array.
     */
    TextView(byte[] array) {
        this(array, 0, array.length);
    }

    TextView(byte[] array, int pos, int len) {
        Objects.checkFromIndexSize(pos, len, array.length);
        this.array = array;
        this.pos = pos;
        this.len = len;
    }

    @Override
    public int length() {
        return len;
    }

    @Override
    public char charAt(int index) {
        Objects.checkIndex(index, len);
        // no masking (& 0x7F). array values normalized by contract.
        return (char) array[pos + index];
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
