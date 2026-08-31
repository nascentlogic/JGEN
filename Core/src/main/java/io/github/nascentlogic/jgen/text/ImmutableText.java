package io.github.nascentlogic.jgen.text;


import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * F.Dahl, 8/15/2026
 */
public class ImmutableText implements UnmanagedText {

    final byte[] array;

    // =============================================================================
    // Constructors
    // =============================================================================

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

    // =============================================================================
    // Text
    // =============================================================================

    @Override
    public byte get(int index) {
        return array[index];
    }

    @Override
    public ByteBuffer readBuffer() {
        return ByteBuffer.wrap(array).asReadOnlyBuffer();
    }

    // =============================================================================
    // CharSequence
    // =============================================================================

    @Override
    public int length() {
        return array.length;
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

    // =============================================================================
    // Object
    // =============================================================================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ImmutableText other)) return false;
        return java.util.Arrays.equals(this.array, other.array);
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(array);
    }
}
