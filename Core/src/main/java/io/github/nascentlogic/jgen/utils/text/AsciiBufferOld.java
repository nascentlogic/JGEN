package io.github.nascentlogic.jgen.utils.text;


import io.github.nascentlogic.jgen.utils.JgenMath;

import java.nio.charset.StandardCharsets;

public class AsciiBufferOld {

    /*
        Input Stream         -> Rule Applied                     -> Resulting Byte
        -------------------------------------------------------------------------
        'A' (65)             -> Rule 1 (ASCII)                   -> 'A' (65)
        '\t' (9)             -> Rule 2 (Allowed Control)         -> '\t' (9)
        '\0' (0)             -> Rule 2 (Dropped Control)         -> (Ignored)
        '\r\n' (13, 10)      -> Rule 3 (CRLF Pair)               -> '\n' (10)
        'X\rY' (88, 13, 89)  -> Rule 4 (Isolated CR)             -> 'X', '\n', 'Y'
        'æ' (230 / -26)      -> Rule 1 (Non-ASCII)               -> (Ignored)
     */

    public static final byte TAB = 9;
    public static final byte LINE_FEED = 10;
    public static final byte CARRIAGE_RETURN = 13;

    private byte[] buffer;
    private int gapStart;
    private int gapEnd;


    // =============================================================================
    // CONSTRUCTORS
    // =============================================================================

    /**
     * Creates an empty AsciiBuffer with a default power-of-two capacity of 16 bytes.
     */
    public AsciiBufferOld() {
        this(16);
    }

    /**
     * Creates an empty AsciiBuffer with a capacity rounded up to the nearest power of two (minimum 16).
     * @param initialCapacity Requested initial capacity in bytes.
     */
    public AsciiBufferOld(int initialCapacity) {
        int cap = JgenMath.nextPow2(Math.max(16, initialCapacity));
        this.buffer = new byte[cap];
        this.gapStart = 0;
        this.gapEnd = cap;
    }

    /**
     * Creates an AsciiBuffer initialized with a String.
     * Capacity is sized to the nearest power of two that fits the internal content (minimum 16).
     * @param text The initial string content.
     */
    public AsciiBufferOld(String text) {
        this(text, 0);
    }

    /**
     * Creates an AsciiBuffer initialized with a String and optional extra byte padding.
     * Capacity is sized to the nearest power of two >= (contentLength + padding) (minimum 16).
     * @param text    The initial string content.
     * @param padding Extra byte capacity to allocate inside the gap for future edits.
     */
    public AsciiBufferOld(String text, int padding) {
        int contentLen = calcInternalLength(text);
        int requiredCap = Math.max(16, contentLen + Math.max(0, padding));
        int cap = JgenMath.nextPow2(requiredCap);
        byte[] buffer = new byte[cap];
        if (contentLen > 0) toInternalFormat(text, buffer, 0);
        this.buffer = buffer;
        this.gapStart = contentLen;
        this.gapEnd = cap;
    }

    /**
     * Creates an AsciiBuffer initialized with a raw byte array slice.
     * Slices valid bytes into internal format without intermediate array copies.
     * @param src     Source byte array containing external or raw ASCII format data.
     * @param offset  Starting index in the source array.
     * @param length  Number of bytes to read from the source array.
     * @param padding Extra byte capacity to allocate inside the gap for future edits.
     */
    public AsciiBufferOld(byte[] src, int offset, int length, int padding) {
        int contentLen = calcInternalLength(src, offset, length);
        int requiredCap = Math.max(16, contentLen + Math.max(0, padding));
        int cap = JgenMath.nextPow2(requiredCap);
        byte[] buffer = new byte[cap];
        if (contentLen > 0) toInternalFormat(src, offset, length, buffer, 0);
        this.buffer = buffer;
        this.gapStart = contentLen;
        this.gapEnd = cap;
    }

    /**
     * Creates an AsciiBuffer initialized with a raw byte array.
     * @param src     Source byte array containing external or raw ASCII format data.
     * @param padding Extra byte capacity to allocate inside the gap for future edits.
     */
    public AsciiBufferOld(byte[] src, int padding) {
        this(src,0, src.length, padding);
    }


    /**
     * Returns the current cursor position (logical character index).
     */
    public int getCursor() {
        return gapStart;
    }

    /**
     * Returns the total number of active characters in the buffer.
     * Represents the logical length of the text.
     */
    public int length() {
        return buffer.length - (gapEnd - gapStart);
    }

    /**
     * Returns total allocated byte capacity of the backing buffer.
     * (User should only need to acount for length())
     */
    public int capacity() {
        return buffer.length;
    }

    /**
     * Ensures the backing array can accommodate at least minCapacity bytes.
     * If expansion is needed, resizes to the smallest power of two >= minCapacity.
     * @param minCapacity Minimum required total capacity in bytes.
     */
    public void ensureCapacity(int minCapacity) {
        if (buffer.length >= minCapacity) return;
        int newCap = JgenMath.nextPow2(Math.max(buffer.length * 2, minCapacity));
        byte[] newBuffer = new byte[newCap];
        // Copy left segment (0 to gapStart)
        if (gapStart > 0) System.arraycopy(buffer, 0, newBuffer, 0, gapStart);
        // Copy right segment (gapEnd to buffer.length) to the end of newBuffer
        int rightSegmentLen = buffer.length - gapEnd;
        int newGapEnd = newCap - rightSegmentLen;
        if (rightSegmentLen > 0) System.arraycopy(buffer, gapEnd, newBuffer, newGapEnd, rightSegmentLen);
        this.buffer = newBuffer;
        this.gapEnd = newGapEnd;
    }



    // =============================================================================
    // INSERT
    // =============================================================================

    /**
     * Inserts a single byte at the current cursor position.
     * Automatically expands buffer capacity if the gap is full.
     * @param c ASCII byte to insert.
     * @return true if inserted, false if the byte is not valid internal ASCII format.
     */
    public boolean insert(byte c) {
        if (!isValidInternalFormat(c)) return false;
        ensureCapacity(length() + 1);
        buffer[gapStart++] = c;
        return true;
    }

    /**
     * Inserts a String at the current cursor position, normalizing line breaks and filtering invalid characters.
     * @param text String to insert.
     * @return Number of valid bytes inserted into the buffer.
     */
    public int insert(String text) {
        if (text == null || text.isEmpty()) return 0;
        int contentLen = calcInternalLength(text);
        if (contentLen == 0) return 0;
        ensureCapacity(length() + contentLen);
        toInternalFormat(text, buffer, gapStart);
        gapStart += contentLen;
        return contentLen;
    }

    /**
     * Inserts a slice of a raw byte array at the current cursor position.
     * @param src    Source byte array containing ASCII/external format data.
     * @param offset Starting index in source array.
     * @param length Number of bytes to read from source array.
     * @return Number of valid bytes inserted into the buffer.
     */
    public int insert(byte[] src, int offset, int length) {
        if (src == null || length <= 0) return 0;
        int contentLen = calcInternalLength(src, offset, length);
        if (contentLen == 0) return 0;
        ensureCapacity(length() + contentLen);
        toInternalFormat(src, offset, length, buffer, gapStart);
        gapStart += contentLen;
        return contentLen;
    }

    /**
     * Inserts a raw byte array at the current cursor position.
     * @param src Source byte array.
     * @return Number of valid bytes inserted into the buffer.
     */
    public int insert(byte[] src) {
        return insert(src, 0, src == null ? 0 : src.length);
    }

    /**
     * Inserts the content of another AsciiBuffer at the current cursor position.
     * @param src Source AsciiBuffer to insert.
     * @return Number of bytes inserted.
     */
    public int insert(AsciiBufferOld src) {
        if (src == null) return 0;
        int srcLen = src.length();
        if (srcLen == 0) return 0;
        ensureCapacity(length() + srcLen);
        // Copy left segment of src (0 to src.gapStart)
        if (src.gapStart > 0) System.arraycopy(src.buffer, 0, this.buffer, this.gapStart, src.gapStart);
        // Copy right segment of src (src.gapEnd to src.buffer.length)
        int srcRightLen = src.buffer.length - src.gapEnd;
        if (srcRightLen > 0) System.arraycopy(src.buffer, src.gapEnd, this.buffer, this.gapStart + src.gapStart, srcRightLen);
        this.gapStart += srcLen;
        return srcLen;
    }



    // =============================================================================
    // DELETE
    // =============================================================================


    /**
     * Deletes the byte immediately to the left of the cursor (Backspace).
     * @return true if a byte was deleted, false if the cursor was at index 0.
     */
    public boolean deleteBackspace() {
        if (gapStart > 0) {
            gapStart--;
            return true;
        } return false;
    }

    /**
     * Deletes the byte immediately to the right of the cursor (Delete key).
     * @return true if a byte was deleted, false if the cursor was at the end of the text.
     */
    public boolean deleteForward() {
        if (gapEnd < buffer.length) {
            gapEnd++;
            return true;
        } return false;
    }

    /**
     * Deletes a range of characters starting at a logical index.
     * Moves the cursor to the start of the deleted range.
     * @param start Starting logical index.
     * @param count Number of characters to delete.
     * @return true if deletion occurred, false if count <= 0.
     * @throws IndexOutOfBoundsException if start < 0 or (start + count) > length().
     */
    public boolean deleteRange(int start, int count) {
        if (count <= 0) return false;
        int totalLen = length();
        if (start < 0 || start + count > totalLen) {
            throw new IndexOutOfBoundsException("Invalid range [" + start + ", " + (start + count) + ") for length " + totalLen);
        } // Align gap with deletion start
        setCursor(start);
        // Expand gap to consume 'count' characters from the right
        gapEnd += count;
        return true;
    }

    /**
     * Resets the buffer to an empty state in O(1) time.
     * Keeps the existing backing array allocation, moving the gap to span the full capacity.
     */
    public void clear() {
        this.gapStart = 0;
        this.gapEnd = buffer.length;
    }


    // =============================================================================
    // CURSOR
    // =============================================================================

    /**
     * Relocates the internal gap to align with a specific logical character index.
     * @param position Target logical index (must be between 0 and length()).
     * @throws IndexOutOfBoundsException if position < 0 or position > length().
     */
    public void setCursor(int position) {
        if (position < 0 || position > length()) {
            throw new IndexOutOfBoundsException("Cursor position " + position + " out of bounds for length " + length());
        } if (position < gapStart) {
            // Moving cursor left: shift bytes from left of gap to right of gap
            int moveCount = gapStart - position;
            System.arraycopy(buffer, position, buffer, gapEnd - moveCount, moveCount);
            gapStart -= moveCount;
            gapEnd -= moveCount;
        } else if (position > gapStart) {
            // Moving cursor right: shift bytes from right of gap to left of gap
            int moveCount = position - gapStart;
            System.arraycopy(buffer, gapEnd, buffer, gapStart, moveCount);
            gapStart += moveCount;
            gapEnd += moveCount;
        }
    }

    /**
     * Attempts to move the cursor one logical position to the left.
     * @return true if the cursor was moved, false if the cursor was already at index 0.
     */
    public boolean moveCursorLeft() {
        if (gapStart > 0) {
            setCursor(gapStart - 1);
            return true;
        } return false;
    }

    /**
     * Attempts to move the cursor one logical position to the right.
     * @return true if the cursor was moved, false if the cursor was already at the end of the text.
     */
    public boolean moveCursorRight() {
        if (gapStart < length()) {
            setCursor(gapStart + 1);
            return true;
        } return false;
    }


    // =============================================================================
    // SEARCH & INSPECTION
    // =============================================================================

    /**
     * Reads the ASCII byte at the current cursor position without moving the cursor or gap.
     * @return The ASCII byte value (0..127) at the cursor, or -1 if the cursor is at end of buffer.
     */
    public int getByte() {
        if (gapStart >= length()) return -1;
        return buffer[gapEnd] & 0x7F;
    }

    /**
     * Reads the ASCII byte at a specific logical character index without moving the cursor or gap.
     * @param index Logical character index (0 to length() - 1).
     * @return The ASCII byte value (0..127) at index, or -1 if index is out of bounds.
     */
    public int getByte(int index) {
        if (index < 0 || index >= length()) return -1;
        return index < gapStart ? (buffer[index] & 0x7F) : (buffer[gapEnd + (index - gapStart)] & 0x7F);
    }

    /**
     * Searches forward for a target byte starting at a specific logical character index.
     * @param target    Byte to find.
     * @param fromIndex Starting logical character index.
     * @return Logical index of first occurrence, or -1 if not found.
     */
    public int indexOf(byte target, int fromIndex) {
        int len = length();
        if (fromIndex < 0) fromIndex = 0;
        if (fromIndex >= len) return -1;
        for (int i = fromIndex; i < len; i++) {
            if (getByte(i) == target) return i;
        } return -1;
    }

    /**
     * Searches forward for a target byte starting implicitly from the current cursor position.
     * Safe on empty buffers (returns -1).
     * @param target Byte to find.
     * @return Logical index of first occurrence, or -1 if not found.
     */
    public int indexOf(byte target) {
        return indexOf(target, gapStart);
    }

    /**
     * Searches forward for a multi-byte sequence starting at a specific logical character index.
     * Normalizes the pattern if needed to match internal buffer formatting.
     *
     * @param pattern   Byte array pattern to find.
     * @param offset    Starting index in pattern.
     * @param length    Number of bytes to match from pattern.
     * @param fromIndex Starting logical character index in the buffer.
     * @return Starting logical index of first occurrence, or -1 if not found.
     */
    public int indexOf(byte[] pattern, int offset, int length, int fromIndex) {
        if (pattern == null || length <= 0 || offset < 0 || offset + length > pattern.length) return -1;
        int bufLen = length();
        if (fromIndex < 0) fromIndex = 0;
        if (fromIndex + length > bufLen) return -1;
        // Check if pattern contains \r (needs line-ending normalization)
        boolean needsNormalization = false;
        for (int i = offset; i < offset + length; i++) {
            if (pattern[i] == CARRIAGE_RETURN) {
                needsNormalization = true;
                break;
            }
        }
        byte[] searchPat;
        int searchLen;
        if (needsNormalization) {
            searchLen = calcInternalLength(pattern, offset, length);
            searchPat = new byte[searchLen];
            toInternalFormat(pattern, offset, length, searchPat, 0);
        } else {
            searchPat = pattern;
            searchLen = length;
        }
        byte firstByte = searchPat[needsNormalization ? 0 : offset];
        int patOffset = needsNormalization ? 0 : offset;
        int maxIndex = bufLen - searchLen;
        for (int i = fromIndex; i <= maxIndex; i++) {
            if (getByte(i) == firstByte) {
                int j = 1;
                while (j < searchLen && getByte(i + j) == searchPat[patOffset + j]) j++;
                if (j == searchLen) return i;
            }
        }
        return -1;
    }

    /**
     * Searches forward for a byte array pattern starting at a specific logical character index.
     */
    public int indexOf(byte[] pattern, int fromIndex) {
        return indexOf(pattern, 0, pattern == null ? 0 : pattern.length, fromIndex);
    }

    /**
     * Searches forward for a byte array pattern starting implicitly at the current cursor position.
     */
    public int indexOf(byte[] pattern) {
        return indexOf(pattern, gapStart);
    }

    /**
     * Searches forward for a String pattern starting at a specific logical character index.
     * Operates zero-allocation for clean strings (no \r).
     *
     * @param pattern   String pattern to search for.
     * @param fromIndex Starting logical character index in the buffer.
     * @return Starting logical index of first occurrence, or -1 if not found.
     */
    public int indexOf(String pattern, int fromIndex) {
        if (pattern == null || pattern.isEmpty()) return -1;
        int patLen = pattern.length();
        int bufLen = length();
        if (fromIndex < 0) fromIndex = 0;
        if (fromIndex + patLen > bufLen) return -1;
        // Check if string pattern contains \r
        boolean hasCarriageReturn = false;
        for (int i = 0; i < patLen; i++) {
            if (pattern.charAt(i) == CARRIAGE_RETURN) {
                hasCarriageReturn = true;
                break;
            }
        }
        // If pattern contains \r, normalize to internal bytes first
        if (hasCarriageReturn) {
            int normLen = calcInternalLength(pattern);
            byte[] normBytes = new byte[normLen];
            toInternalFormat(pattern, normBytes, 0);
            return indexOf(normBytes, 0, normLen, fromIndex);
        }
        // Zero-allocation direct character search path
        char firstChar = pattern.charAt(0);
        int maxIndex = bufLen - patLen;
        for (int i = fromIndex; i <= maxIndex; i++) {
            if (getByte(i) == (byte) firstChar) {
                int j = 1;
                while (j < patLen && getByte(i + j) == (byte) pattern.charAt(j)) j++;
                if (j == patLen) return i;
            }
        }

        return -1;
    }

    /**
     * Searches forward for a String pattern starting implicitly from the current cursor position.
     * @param pattern String pattern to search for.
     * @return Starting logical index of first occurrence, or -1 if not found.
     */
    public int indexOf(String pattern) {
        return indexOf(pattern, gapStart);
    }

    /**
     * Searches backward for a target byte starting from a specific logical character index.
     * @param target    Byte to find.
     * @param fromIndex Starting logical character index (searches backward towards index 0).
     * @return Logical index of last occurrence, or -1 if not found.
     */
    public int lastIndexOf(byte target, int fromIndex) {
        int len = length();
        if (len == 0) return -1;
        if (fromIndex >= len) fromIndex = len - 1;
        if (fromIndex < 0) return -1;
        for (int i = fromIndex; i >= 0; i--) {
            if (getByte(i) == target) return i;
        } return -1;
    }

    /**
     * Searches backward for a target byte starting implicitly from the character before the current cursor position.
     * @param target Byte to find.
     * @return Logical index of last occurrence, or -1 if not found.
     */
    public int lastIndexOf(byte target) {
        return lastIndexOf(target, gapStart - 1);
    }


    /**
     * Helper for inspecting raw array bytes, gap bounds, and string conversions during testing.
     */
    public void printRawBufferDetails() {
        System.out.println("--- Buffer Details ---");
        System.out.println("Cursor: " + getCursor() + ", Length: " + length() + ", Capacity: " + capacity());
        System.out.println("Gap Start: " + gapStart + ", Gap End: " + gapEnd + " (Gap Size: " + (gapEnd - gapStart) + ")");
        System.out.print("Raw Bytes: [");
        for (int i = 0; i < buffer.length; i++) {
            if (i == gapStart) System.out.print(" GAP_START-> ");
            if (i == gapEnd) System.out.print(" <-GAP_END ");

            byte b = buffer[i];
            if (i >= gapStart && i < gapEnd) {
                System.out.print("..");
            } else if (b == LINE_FEED) {
                System.out.print("\\n");
            } else if (b == TAB) {
                System.out.print("\\t");
            } else {
                System.out.print((char) b);
            }
            if (i < buffer.length - 1) System.out.print(" ");
        }
        System.out.println("]");
        System.out.println("Internal Format String: \"" + toInternalString().replace("\n", "\\n").replace("\t", "\\t") + "\"");
        System.out.println("----------------------\n");
    }


    @Override
    public String toString() {
        return "AsciiBuffer[length=" + length() + ", capacity=" + buffer.length + "]";
    }

    /** Converts active characters to a String matching internal engine representation ('\n' line breaks). */
    public String toInternalString() {
        int textLength = length();
        if (textLength == 0) return "";
        byte[] result = new byte[textLength];
        if (gapStart > 0) {
            System.arraycopy(buffer, 0, result, 0, gapStart);
        } int rightLen = buffer.length - gapEnd;
        if (rightLen > 0) {
            System.arraycopy(buffer, gapEnd, result, gapStart, rightLen);
        } return new String(result, StandardCharsets.US_ASCII);
    }

    /** Exports active characters to a String formatted with the host OS's native line breaks (e.g. \r\n on Windows). */
    public String toExternalString() {
        String lineSep = System.lineSeparator();
        // If system line separator is just \n (Linux / macOS), internal matches external
        if (lineSep.equals("\n")) return toInternalString();
        // On platforms with \r\n (Windows), expand \n to \r\n
        String internalText = toInternalString();
        if (internalText.isEmpty()) return "";
        StringBuilder builder = new StringBuilder(internalText.length() + 16);
        for (int i = 0; i < internalText.length(); i++) {
            char c = internalText.charAt(i);
            if (c == '\n') {
                builder.append('\r');
            } builder.append(c);
        } return builder.toString();
    }


    /** Checks if a character byte matches valid internal ASCII format. */
    private static boolean isValidInternalFormat(int c) {
        if (c >= 0 && c < 127) {
            if (c < 32) return c == LINE_FEED || c == TAB;
            return true;
        } return false;
    }

    /** Calculates the exact internal byte length of a String without writing or allocating arrays. */
    private static int calcInternalLength(String str) {
        if (str == null || str.isEmpty()) return 0;
        int len = str.length();
        int count = 0;
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c < 127) {
                if (c < 32) {
                    if (c == CARRIAGE_RETURN) {
                        if (i + 1 < len && str.charAt(i + 1) == '\n') {
                            continue; // Skip \r in \r\n pair
                        } // Isolated \r will be converted to \n, so it counts as 1 byte
                    } else if (c != '\n' && c != '\t') {
                        continue; // Drop unprintable control chars
                    }
                }
                count++;
            }
        }
        return count;
    }

    /** Calculates the exact internal byte length of a raw byte array without writing or allocating arrays. */
    private static int calcInternalLength(byte[] src, int srcOffset, int srcLength) {
        if (src == null || srcLength <= 0) return 0;
        int count = 0;
        for (int i = 0; i < srcLength; i++) {
            byte b = src[srcOffset + i];
            if (b >= 0 && b < 127) {
                if (b < 32) {
                    if (b == CARRIAGE_RETURN) {
                        if (i + 1 < srcLength && src[srcOffset + i + 1] == LINE_FEED) {
                            continue; // Skip \r in \r\n pair
                        } // Isolated \r will be converted to \n, so it counts as 1 byte
                    } else if (b != LINE_FEED && b != TAB) {
                        continue; // Drop unprintable control bytes
                    }
                }
                count++;
            }
        }
        return count;
    }

    /** Writes normalized internal bytes directly from a String into a dst array.
     * Returns the exact number of valid bytes written. */
    private static int toInternalFormat(String str, byte[] dst, int dstOffset) {
        if (str == null || str.isEmpty() || dst == null) return 0;
        int len = str.length();
        int count = 0;
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c < 127) {
                if (c < 32) {
                    if (c == CARRIAGE_RETURN) {
                        if (i + 1 < len && str.charAt(i + 1) == '\n') {
                            continue; // Skip \r in \r\n pair
                        } c = '\n'; // Convert isolated \r to \n
                    } else if (c != '\n' && c != '\t') {
                        continue; // Drop unprintable control chars
                    }
                }
                dst[dstOffset + count++] = (byte) c;
            }
        }
        return count;
    }

    /** Writes normalized internal bytes directly from a src byte array into a dst array.
     * Returns the exact number of valid bytes written. */
    private static int toInternalFormat(byte[] src, int srcOffset, int srcLength, byte[] dst, int dstOffset) {
        if (src == null || srcLength <= 0 || dst == null) return 0;
        int count = 0;
        for (int i = 0; i < srcLength; i++) {
            byte b = src[srcOffset + i];
            if (b >= 0 && b < 127) {
                if (b < 32) {
                    if (b == CARRIAGE_RETURN) {
                        if (i + 1 < srcLength && src[srcOffset + i + 1] == LINE_FEED) {
                            continue; // Skip \r in \r\n pair
                        } b = LINE_FEED; // Convert isolated \r to \n
                    } else if (b != LINE_FEED && b != TAB) {
                        continue; // Drop unprintable control bytes
                    }
                }
                dst[dstOffset + count++] = b;
            }
        }
        return count;
    }




}