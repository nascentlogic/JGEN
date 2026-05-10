package io.github.nascentlogic.jgen.utils;

/**
 * F.Dahl, 5/8/2026
 */
public class MathUtils {


    /**
     * Returns the smallest power of two >= value.
     * Returns 1 for inputs <= 1.
     * @throws IllegalArgumentException if value > 2^30 (overflow).
     */
    public static int nextPowerOfTwo(int value) {
        if (value <= 1) return 1;
        if (value > (1 << 30)) {
            throw new IllegalArgumentException("Value exceeds maximum positive power of two.");
        } value--;
        value |= value >>> 1;
        value |= value >>> 2;
        value |= value >>> 4;
        value |= value >>> 8;
        value |= value >>> 16;
        return value + 1;
    }

}
