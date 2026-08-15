package io.github.nascentlogic.jgen.utils.arena;

import java.util.Objects;

/**
 * F.Dahl, 8/15/2026
 */
public interface Text extends CharSequence {

    byte TAB = 0x09;             // '\t'
    byte LINE_FEED = 0x0A;       // '\n'
    byte CARRIAGE_RETURN = 0x0D; // '\r'

    static boolean isValidInternalFormat(int c) {
        return (c >= 32 && c < 127) || c == LINE_FEED || c == TAB;
    }

    static int normalizedLength(CharSequence src) { return normalizedLength(src,0,src.length()); }
    static int normalizedLength(CharSequence src, int srcFrom, int srcTo) {
        Objects.checkFromToIndex(srcFrom, srcTo, src.length());
        int count = 0;
        for (int i = srcFrom; i < srcTo; i++) {
            if (isValidInternalFormat(src.charAt(i))) count++;
        } return count;
    }

    static int normalize(CharSequence src, byte[] dst, int dstFrom) { return normalize(src, 0, src.length(), dst, dstFrom); }
    static int normalize(CharSequence src, int srcFrom, int srcTo, byte[] dst, int dstFrom) {
        Objects.checkFromToIndex(srcFrom, srcTo, src.length());
        if (srcFrom == srcTo) return 0;
        int count = 0;
        for (int i = srcFrom; i < srcTo; i++) {
            char c = src.charAt(i);
            if (isValidInternalFormat(c))
                dst[dstFrom + count++] = (byte) c;
        } return count;
    }

    static int normalize(byte[] src, byte[] dst, int dstFrom) { return normalize(src, 0, src.length, dst, dstFrom); }
    static int normalize(byte[] src, int srcFrom, int srcTo, byte[] dst, int dstFrom) {
        Objects.checkFromToIndex(srcFrom, srcTo, src.length);
        if (srcFrom == srcTo) return 0;
        int count = 0;
        for (int i = srcFrom; i < srcTo; i++) {
            byte b = src[i];
            if (isValidInternalFormat(b))
                dst[dstFrom + count++] = b;
        } return count;
    }
}
