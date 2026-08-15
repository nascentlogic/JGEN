package io.github.nascentlogic.jgen.utils.arena;


import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * F.Dahl, 8/15/2026
 */
public class ImmutableText implements UnmanagedText {

    final byte[] array;

    public ImmutableText(CharSequence str) {
        this.array = new byte[Text.normalizedLength(str)];
        Text.normalize(str, array, 0);
    }

    /**
     * Package private initializer.
     * @param array Normalized ascii array.
     */
    ImmutableText(byte[] array) {
        this.array = Objects.requireNonNull(array);
    }

    @Override
    public int length() {
        return array.length;
    }

    @Override
    public char charAt(int index) {
        return (char) array[index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, array.length);
        if (start == 0 && end == array.length) return this;
        return new TextView(array, start, end - start);
    }

    @Override
    public String toString() {
        return new String(array, StandardCharsets.US_ASCII);
    }
}
