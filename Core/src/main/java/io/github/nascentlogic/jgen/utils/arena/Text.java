package io.github.nascentlogic.jgen.utils.arena;

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
