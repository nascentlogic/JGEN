package io.github.nascentlogic.jgen.gui.text;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * F.Dahl, 8/15/2026
 */
public interface Text extends CharSequence, Comparable<CharSequence> {

    /** Singleton 0 length, UmmanagedText. */
    Text EMPTY_TEXT = EmptyText.INSTANCE;
    /** 0 length, Read-Only ByteBuffer. */
    ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0).asReadOnlyBuffer();

    // =============================================================================
    // CharSequence
    // =============================================================================

    @Override
    default char charAt(int index) {
        // no masking (& 0x7F). array values normalized by contract.
        return (char) get(index);
    }

    // =============================================================================
    // Get
    // =============================================================================

    /**
     * Get raw byte without char casting OR bounds check.
     * @param index index of character.
     * @return valid printable ascii character.
     */
    byte get(int index);


    /**
     * Creates a {@link ByteBuffer} slice representing of this buffer's current content.<p>
     * Views are short lived objects, as modifying the Text may alter the View. Beneficial for saving text to file.<p>
     * The returned buffer's position is 0, and its limit and capacity equal the buffers length. <p>
     * @return a read only view of the buffer content.
     * @throws IllegalStateException if this buffer is a disposed {@link ManagedBuffer}.
     */
    ByteBuffer readBuffer();


    // =============================================================================
    // INDEX OF (Forward Search)
    // =============================================================================

    /**
     * Returns the index within this text of the first occurrence of the specified ASCII character.
     * @param c the ASCII character (byte value) to search for
     * @return the index of the first occurrence, or -1 if character does not occur
     */
    default int indexOf(byte c) {
        return indexOf(c, 0);
    }

    /**
     * Returns the index within this text of the first occurrence of the specified ASCII character,
     * starting the search at the specified index.
     * @param c         the ASCII character (byte value) to search for
     * @param fromIndex the index to start the search from
     * @return the index of the first occurrence at or after {@code fromIndex},
     *         or -1 if character does not occur
     */
    default int indexOf(byte c, int fromIndex) {
        int len = length();
        if (fromIndex < 0) fromIndex = 0;
        for (int i = fromIndex; i < len; i++) {
            if (get(i) == c) return i;
        } return -1;
    }

    /**
     * Returns the index within this text of the first occurrence of the specified substring.
     * @param str the sequence to search for
     * @return the index of the first occurrence, or -1 if the sequence is not found
     */
    default int indexOf(CharSequence str) {
        return indexOf(str, 0);
    }

    /**
     * Returns the index within this text of the first occurrence of the specified substring,
     * starting at the specified index.
     * @param str       the sequence to search for
     * @param fromIndex the index to start the search from
     * @return the index of the first occurrence at or after {@code fromIndex},
     *         or -1 if the sequence is not found
     */
    @SuppressWarnings("all")
    default int indexOf(CharSequence str, int fromIndex) {
        Objects.requireNonNull(str, "str cannot be null");
        int strLen = str.length();
        int len = length();
        if (fromIndex < 0) fromIndex = 0;
        // String.indexOf behavior for empty target
        if (strLen == 0) return Math.min(fromIndex, len);
        if (fromIndex >= len || strLen > len - fromIndex) return -1;
        char firstChar = str.charAt(0);
        int max = len - strLen;
        for (int i = fromIndex; i <= max; i++) {
            // Look for first character
            if (charAt(i) != firstChar) {
                while (++i <= max && charAt(i) != firstChar);
            } // Found first character, now check the rest
            if (i <= max) {
                int j = i + 1;
                int end = j + strLen - 1;
                for (int k = 1; j < end && charAt(j) == str.charAt(k); j++, k++);
                if (j == end) return i; // Full match found
            }
        }
        return -1;
    }

    // =============================================================================
    // LAST INDEX OF (Backward Search)
    // =============================================================================

    /**
     * Returns the index within this text of the last occurrence of the specified ASCII character.
     * @param c the ASCII character (byte value) to search for
     * @return the index of the last occurrence, or -1 if character does not occur
     */
    default int lastIndexOf(byte c) {
        return lastIndexOf(c, length() - 1);
    }

    /**
     * Returns the index within this text of the last occurrence of the specified ASCII character,
     * searching backward starting at the specified index.
     * @param c         the ASCII character (byte value) to search for
     * @param fromIndex the index to start the search backward from
     * @return the index of the last occurrence at or before {@code fromIndex},
     *         or -1 if character does not occur
     */
    default int lastIndexOf(byte c, int fromIndex) {
        int len = length();
        if (fromIndex >= len) fromIndex = len - 1;
        for (int i = fromIndex; i >= 0; i--) {
            if (get(i) == c) return i;
        } return -1;
    }

    /**
     * Returns the index within this text of the last occurrence of the specified substring.
     * @param str the sequence to search for
     * @return the index of the last occurrence, or -1 if the sequence is not found
     */
    default int lastIndexOf(CharSequence str) {
        return lastIndexOf(str, length());
    }

    /**
     * Returns the index within this text of the last occurrence of the specified substring,
     * searching backward starting at the specified index.
     * @param str       the sequence to search for
     * @param fromIndex the index to start the search backward from
     * @return the index of the last occurrence at or before {@code fromIndex},
     *         or -1 if the sequence is not found
     */
    @SuppressWarnings("all")
    default int lastIndexOf(CharSequence str, int fromIndex) {
        Objects.requireNonNull(str, "str cannot be null");
        int strLen = str.length();
        int len = length();
        if (fromIndex > len - strLen) fromIndex = len - strLen;
        if (fromIndex < 0) return -1;
        // String.lastIndexOf behavior for empty target
        if (strLen == 0) return fromIndex;
        char lastChar = str.charAt(strLen - 1);
        int min = strLen - 1;
        int i = fromIndex + strLen - 1;
        for (; i >= min; i--) {
            // Look for last character
            if (charAt(i) != lastChar) {
                while (--i >= min && charAt(i) != lastChar);
            } // Found last character, check the rest backward
            if (i >= min) {
                int start = i - strLen + 1;
                int j = i - 1;
                int k = strLen - 2;
                while (j >= start && charAt(j) == str.charAt(k)) {
                    j--;
                    k--;
                } if (j < start) return start; // Full match found
            }
        }
        return -1;
    }

    // =============================================================================
    // CHARACTER TYPE SEARCH
    // =============================================================================

    /**
     * Finds the index of the first printable character at or after {@code fromIndex}.
     * @return index of matching character, or -1 if none found
     */
    default int nextPrintable(int fromIndex) {
        int len = length();
        if (fromIndex < 0) fromIndex = 0;
        for (int i = fromIndex; i < len; i++) {
            if (isPrintable(get(i))) return i;
        } return -1;
    }

    /**
     * Finds the index of the first graphical (ink) character at or after {@code fromIndex}.
     * @return index of matching character, or -1 if none found
     */
    default int nextGraphical(int fromIndex) {
        int len = length();
        if (fromIndex < 0) fromIndex = 0;
        for (int i = fromIndex; i < len; i++) {
            if (isGraphical(get(i))) return i;
        } return -1;
    }

    /**
     * Finds the index of the first whitespace character (Space, Tab, Line Feed) at or after {@code fromIndex}.
     * @return index of matching character, or -1 if none found
     */
    default int nextWhitespace(int fromIndex) {
        int len = length();
        if (fromIndex < 0) fromIndex = 0;
        for (int i = fromIndex; i < len; i++) {
            if (isWhitespace(get(i))) return i;
        } return -1;
    }

    /**
     * Finds the index of the first horizontal whitespace character (Space, Tab) at or after {@code fromIndex}.
     * @return index of matching character, or -1 if none found
     */
    default int nextHorizontalWhitespace(int fromIndex) {
        int len = length();
        if (fromIndex < 0) fromIndex = 0;
        for (int i = fromIndex; i < len; i++) {
            if (isHorizontalWhitespace(get(i))) return i;
        } return -1;
    }

    /**
     * Finds the index of the first control character (Line Feed, Tab) at or after {@code fromIndex}.
     * @return index of matching character, or -1 if none found
     */
    default int nextControl(int fromIndex) {
        int len = length();
        if (fromIndex < 0) fromIndex = 0;
        for (int i = fromIndex; i < len; i++) {
            if (isControl(get(i))) return i;
        } return -1;
    }

    /**
     * Finds the index of the first word delimiter (Space, Tab, Line Feed) at or after {@code fromIndex}.
     * @return index of matching character, or -1 if none found
     */
    default int nextWordDelimiter(int fromIndex) {
        int len = length();
        if (fromIndex < 0) fromIndex = 0;
        for (int i = fromIndex; i < len; i++) {
            if (isWordDelimiter(get(i))) return i;
        } return -1;
    }

    // =============================================================================
    // PREFIX / SUFFIX CHECKS
    // =============================================================================

    /**
     * Tests if this text starts with the specified ASCII character.
     * @param c the ASCII character (byte value) to check
     * @return {@code true} if the character sequence represented by the argument is a prefix
     */
    default boolean startsWith(byte c) {
        return length() > 0 && get(0) == c;
    }

    /**
     * Tests if this text starts with the specified prefix.
     * @param prefix the prefix to check
     * @return {@code true} if the sequence represented by the argument is a prefix
     */
    default boolean startsWith(CharSequence prefix) {
        return startsWith(prefix, 0);
    }

    /**
     * Tests if the substring of this text beginning at the specified index
     * starts with the specified prefix.
     * @param prefix the prefix to check
     * @param offset where to begin searching in this text
     * @return {@code true} if the sequence represented by the argument is a prefix at offset
     */
    default boolean startsWith(CharSequence prefix, int offset) {
        Objects.requireNonNull(prefix, "prefix cannot be null");
        int prefixLen = prefix.length();
        int len = length();
        // Bounds check matching String.startsWith
        if (offset < 0 || offset > len - prefixLen) return false;
        for (int i = 0; i < prefixLen; i++) {
            if (charAt(offset + i) != prefix.charAt(i)) return false;
        } return true;
    }

    /**
     * Tests if this text ends with the specified ASCII character.
     * @param c the ASCII character (byte value) to check
     * @return {@code true} if the character sequence represented by the argument is a suffix
     */
    default boolean endsWith(byte c) {
        int len = length();
        return len > 0 && get(len - 1) == c;
    }

    /**
     * Tests if this text ends with the specified suffix.
     * @param suffix the suffix to check
     * @return {@code true} if the sequence represented by the argument is a suffix
     */
    default boolean endsWith(CharSequence suffix) {
        return startsWith(suffix, length() - suffix.length());
    }

    // =============================================================================
    // EQUALITY and COMPARISON
    // =============================================================================

    /**
     * @see String#contentEquals(CharSequence)
     */
    default boolean contentEquals(CharSequence str) {
        if (str == this) return true;
        if (str == null) return false;
        int strLen = str.length();
        int len = length();
        if (strLen != len) return false;
        if (str instanceof Text text) {
            for (int i = 0; i < len; i++)
                if (text.get(i) != get(i)) return false;
        } else for (int i = 0; i < len; i++) {
            if (str.charAt(i) != charAt(i)) return false;
        } return true;
    }

    /**
     * Compares this {@code Text} instance with another {@code CharSequence} lexicographically.
     * @param other the sequence to be compared
     * @return a negative integer, zero, or a positive integer as this text
     *         is lexicographically less than, equal to, or greater than the specified sequence.
     */
    @Override
    default int compareTo(CharSequence other) {
        Objects.requireNonNull(other, "other sequence cannot be null");
        if (other == this) return 0;
        int len1 = length();
        int len2 = other.length();
        int lim = Math.min(len1, len2);
        // Fast path for comparing two Text instances (direct byte arithmetic)
        if (other instanceof Text text) {
            for (int i = 0; i < lim; i++) {
                byte b1 = get(i);
                byte b2 = text.get(i);
                // Safe because b1, b2 are guaranteed > 0 by contract
                if (b1 != b2) return b1 - b2;
            }
        } else for (int i = 0; i < lim; i++) {
            char c1 = charAt(i);
            char c2 = other.charAt(i);
            if (c1 != c2) return c1 - c2;
        } return len1 - len2;
    }


    // =============================================================================
    // Static Utility
    // =============================================================================

    // Printable ASCII & Normal Control Constants
    byte TAB             = 0x09; // '\t' (9)
    byte LINE_FEED       = 0x0A; // '\n' (10)
    byte CARRIAGE_RETURN = 0x0D; // '\r' (13)
    byte SPACE           = 0x20; // ' '  (32)
    byte TILDE           = 0x7E; // '~'  (126)

    /**
     * Checks whether a character code point represents a valid normalized
     * character in our internal ASCII text format.
     *
     * Valid characters include:
     * - Tab ('\t', 0x09)
     * - Line Feed ('\n', 0x0A)
     * - Printable ASCII range (' ' through '~', 32 to 126)
     *
     * @param c character code point to check
     * @return true if valid internal format; false otherwise
     */
    static boolean isValidInternalFormat(int c) {
        return (c >= SPACE && c <= TILDE) || c == LINE_FEED || c == TAB;
    }

    /**
     * Printable ASCII range: ' ' (32) through '~' (126).
     */
    static boolean isPrintable(byte c) {
        return c >= SPACE && c <= TILDE;
    }

    /**
     * Graphic/Ink character range: '!' (33) through '~' (126).
     * Excludes Space, Tab, and Line Feed.
     */
    static boolean isGraphical(byte c) {
        return c > SPACE && c <= TILDE;
    }

    /**
     * Whitespace characters present in normalized text: Space, Tab, Line Feed.
     */
    static boolean isWhitespace(byte c) {
        return c == SPACE || c == TAB || c == LINE_FEED;
    }

    /**
     * Non-breaking horizontal whitespace: Space and Tab.
     */
    static boolean isHorizontalWhitespace(byte c) {
        return c == SPACE || c == TAB;
    }

    /**
     * Control characters present in normalized text: Line Feed and Tab.
     */
    static boolean isControl(byte c) {
        return c == LINE_FEED || c == TAB;
    }

    /**
     * Checks if character breaks a word (Line Feed, Space, or Tab).
     */
    static boolean isWordDelimiter(byte c) {
        return c == SPACE || c == TAB || c == LINE_FEED;
    }




    static int normalizedLength(CharSequence src) { return normalizedLength(src,0,src.length()); }
    static int normalizedLength(CharSequence src, int srcFrom, int srcTo) {
        int srcLen = src.length();
        Objects.checkFromToIndex(srcFrom, srcTo, srcLen);
        if (srcFrom == srcTo) return 0;
        if (src instanceof Text) return srcTo - srcFrom;
        int count = 0;
        for (int i = srcFrom; i < srcTo; i++) {
            if (isValidInternalFormat(src.charAt(i))) count++;
        } return count;
    }

    static int normalize(CharSequence src, byte[] dst, int dstFrom) { return normalize(src, 0, src.length(), dst, dstFrom); }
    static int normalize(CharSequence src, int srcFrom, int srcTo, byte[] dst, int dstFrom) {
        int srcLen = src.length();
        Objects.checkFromToIndex(srcFrom, srcTo, srcLen);
        if (srcFrom == srcTo) return 0;
        int count = 0;
        if (src instanceof Text text) {
            for (int i = srcFrom; i < srcTo; i++)
                dst[dstFrom + count++] = text.get(i);
        } else for (int i = srcFrom; i < srcTo; i++) {
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

    /**
     * Checks if two half-open 1D ranges [start1, end1) and [start2, end2) overlap.
     * @param start1 inclusive start of first range
     * @param end1   exclusive end of first range
     * @param start2 inclusive start of second range
     * @param end2   exclusive end of second range
     * @return true if the ranges intersect; false otherwise
     */
    static boolean rangeOverlap(int start1, int end1, int start2, int end2) {
        return start1 < end2 && start2 < end1;
    }

    /**
     * Checks if two 1D regions defined by (pos, len) overlap.
     * @param pos1 starting position of first region
     * @param len1 length of first region
     * @param pos2 starting position of second region
     * @param len2 length of second region
     * @return true if the regions intersect; false otherwise
     */
    static boolean regionOverlap(int pos1, int len1, int pos2, int len2) {
        return rangeOverlap(pos1, pos1 + len1, pos2, pos2 + len2);
    }


    int MAX_INT_DIGITS = 11;
    int MAX_FLOAT_DIGITS = 16;

    /**
     * Converts an int value into 7-bit ASCII bytes inside a destination array.
     * Optimized for high-performance tight rendering loops.
     *
     * @param value    The int value to format.
     * @param dst      Destination byte array.
     * @param dstFrom  Starting index position in the destination array.
     * @return         The total number of characters written.
     *
     * Documentation:
     * - Max characters inserted: 11
     * - Min value: -2147483648
     * - Max value:  2147483647
     * - Caller must ensure dstFrom + 11 <= dst.length
     */
    static int insertIntDigits(int value, byte[] dst, int dstFrom) {
        if (value == 0) {
            dst[dstFrom] = '0';
            return 1;
        }
        long v = value;
        int pos = dstFrom;
        if (v < 0){
            v = -v;
            dst[pos++] = '-';
        }
        int start = pos;
        while (v > 0) {
            dst[pos++] = (byte) ('0' + (int)(v % 10));
            v /= 10;
        }
        for (int i = start, j = pos - 1; i < j; i++, j--) {
            byte tmp = dst[i];
            dst[i] = dst[j];
            dst[j] = tmp;
        } return pos - dstFrom;
    }

    int[] POW_10 = {1, 10, 100, 1000, 10000, 100000, 1000000};

    /**
     * Converts a floating point value into 7-bit ASCII bytes inside a destination array.
     * Optimized for high-performance tight rendering loops.
     *
     * @param value     The floating point value to format.
     * @param decimals  Number of decimal places (clamped between 0 and 6).
     * @param dst      Destination byte array.
     * @param dstFrom    Starting index position in the destination array.
     * @return          The total number of characters written.
     *
     * Documentation:
     * - Max characters inserted: 16
     * - Min value: -99_999_999.0 (or -Infinity)
     * - Max value: 99_999_999.0 (or Infinity)
     */
    static int insertFloatDigits(double value, int decimals, byte[] dst, int dstFrom) {
        if (Double.isNaN(value)) {
            dst[dstFrom]     = 'N';
            dst[dstFrom + 1] = 'a';
            dst[dstFrom + 2] = 'N';
            return 3;
        }
        if (Double.isInfinite(value)) {
            int pos = dstFrom;
            if (value < 0f) dst[pos++] = '-';
            dst[pos++] = 'I'; dst[pos++] = 'n'; dst[pos++] = 'f';
            dst[pos++] = 'i'; dst[pos++] = 'n'; dst[pos++] = 'i';
            dst[pos++] = 't'; dst[pos++] = 'y';
            return pos - dstFrom;
        }

        boolean neg = value < 0f;
        double v = neg ? -value : value;

        if (v >= 99_999_999.0) {
            int pos = dstFrom;
            if (neg) dst[pos++] = '-';
            dst[pos++] = '9'; dst[pos++] = '9'; dst[pos++] = '9'; dst[pos++] = '9';
            dst[pos++] = '9'; dst[pos++] = '9'; dst[pos++] = '9'; dst[pos++] = '9';
            return pos - dstFrom;
        }

        if (decimals < 0) decimals = 0;
        if (decimals > 6) decimals = 6;
        // =========================================================================
        // CHANGE APPLIED: Scale First, Round Once
        // Previously, we did: int intPart = (int) v; double frac = v - intPart;
        // That subtraction introduced floating-point drift.
        // Now, we scale the entire number first, round globally, and split cleanly
        // using integer division and modulo.
        // =========================================================================
        int scale = POW_10[decimals];
        long totalScaled = Math.round(v * scale);
        int intPart = (int) (totalScaled / scale);
        int fracInt = (int) (totalScaled % scale);
        // =========================================================================
        int pos = dstFrom;
        if (neg) dst[pos++] = '-';

        // Integer digits
        if (intPart == 0) {
            dst[pos++] = '0';
        } else {
            int start = pos;
            int tempInt = intPart;
            while (tempInt > 0) {
                dst[pos++] = (byte) ('0' + (tempInt % 10));
                tempInt /= 10;
            }
            for (int i = start, j = pos - 1; i < j; i++, j--) {
                byte tmp = dst[i];
                dst[i] = dst[j];
                dst[j] = tmp;
            }
        }
        // Fractional digits
        if (decimals > 0) {
            dst[pos++] = '.';
            for (int i = 0; i < decimals; i++) {
                dst[pos + decimals - 1 - i] = (byte) ('0' + (fracInt % 10));
                fracInt /= 10;
            } pos += decimals;
        }
        return pos - dstFrom;
    }


    // =============================================================================
    // Classes
    // =============================================================================

    final class EmptyText implements UnmanagedText {
        private static final EmptyText INSTANCE = new EmptyText();
        private EmptyText() { /* Singleton */ }
        public int length() { return 0; }
        public byte get(int index) { throw new IndexOutOfBoundsException("Index: " + index + ", Length: 0"); }
        public ByteBuffer readBuffer() { return EMPTY_BUFFER;}
        public CharSequence subSequence(int start, int end) {
            Objects.checkFromToIndex(start, end, 0); return this;
        } public String toString() { return ""; }
    }

}
