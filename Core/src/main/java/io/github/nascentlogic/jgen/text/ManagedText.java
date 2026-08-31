package io.github.nascentlogic.jgen.text;

/**
 * F.Dahl, 8/15/2026
 */
public abstract class ManagedText extends ManagedBuffer implements Text {

    /**
     * Set character at index to a valid ascii "printable" character.<p>
     * valid == {@code (c >= 32 && c <= 126) || c == 10 || c == 9}.
     * @param c 7-bit ascii character
     * @param index index of character to replace
     * @return true if {@code c} is valid and therefore inserted
     * @throws IndexOutOfBoundsException
     */
    public abstract boolean set(byte c, int index);

    /**
     * Set the managed text to {@code str}.<p>
     * {@link TextBlock}'s will NOT grow to support: {@code str.length()} > this.{@link #capacity()}.<p>
     * {@link TextBuffer}'s WILL grow to support: {@code str.length()} > this.{@link #capacity()}.<p>
     * @param str charsequence
     * @return New length of {@code this}
     * @throws NullPointerException str == null
     * @throws java.nio.BufferOverflowException if {@code str.length()} > {@code this}{@link #capacity()}
     * AND {@code this} is not a {@link TextBuffer}.
     */
    public abstract int set(CharSequence str);

    /**
     * Clears the buffer, leaving {@link #length()} {@code == 0} post operation.<p>
     * Does not free the buffer ({@link #capacity()} remains the same).
     */
    public abstract void clear();

    /**
     * Capacity of backing array (arena block).
     * It's identical to {@link ManagedBuffer#blockSize()}
     * A disposed ManagedBuffer capacity == 0.
     * @return capacity
     */
    public final int capacity() {
        return blockSize();
    }
}
