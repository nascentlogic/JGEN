package io.github.nascentlogic.jgen.utils;

import org.joml.Matrix4f;

/**
 * F.Dahl, 5/8/2026
 */
public class JgenMath {

    private static final double INV_LN2 = 1.0 / Math.log(2.0);

    /**
     * Returns the smallest power of two >= value.
     * Returns 1 for inputs <= 1.
     * @throws IllegalArgumentException if value > 2^30 (overflow).
     */
    public static int nextPow2(int value) {
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

    /**
     * Returns the base-2 logarithm floor integer of a value.
     * Returns 0 for inputs <= 0.
     */
    public static int log2i(int value) {
        if (value <= 0) return 0;
        return 31 - Integer.numberOfLeadingZeros(value);
    }

    /**
     * Returns the precise base-2 logarithm floating-point representation of a value.
     */
    public static float log2f(float value) {
        return (float) (Math.log(value) * INV_LN2);
    }


    public static float clamp(float value) {
        return Math.clamp(value, 0f, 1f);
    }

    /**
     * Constructs a 2D orthographic screen-space matrix with (0,0) at the bottom-left.
     * @param width  The width of the screen/framebuffer in pixels.
     * @param height The height of the screen/framebuffer in pixels.
     * @param dest   The destination JOML Matrix4f to populate
     * @return The updated projection matrix.
     */
    public static Matrix4f screenSpaceMatrix(float width, float height, Matrix4f dest) {
        return dest.setOrtho2D(0.0f, width, 0.0f, height);
    }

    /** @see #screenSpaceMatrix(float, float, Matrix4f) */
    public static Matrix4f screenSpaceMatrix(float width, float height) {
        return screenSpaceMatrix(width, height, new Matrix4f());
    }

}
