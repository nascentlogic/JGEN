package io.github.nascentlogic.jgen.utils.text;

import io.github.nascentlogic.jgen.utils.JgenMath;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * F.Dahl, 8/9/2026
 */
public class AsciiBuffer {

    // dont calc length, no padding todo
    // export bytebuffer

    /*
        Important note:
        To keep things simple (Apart from the constructors),
        anything we insert has to be either another AsciiBuffer or single characters.
        Inserting Strings or raw byte[] we'll deal with later. And for those insertions
        we MUST make a roundtrip to -> AsciiBuffer before calling the insert(AsciiBuffer buffer).
        With insert, i mean any method that inserts / writes or replaces characters in / into this Buffer.
        AsciiBuffer deals exclusively in internal (normalized) bytes or other AsciiBuffers.
     */

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

    private byte[] buffer;
    private int gapStart;
    private int gapEnd;


    // =============================================================================
    // CONSTRUCTORS
    // =============================================================================

    /**
     * Constructs an empty AsciiBuffer with a default capacity of 16.
     */
    public AsciiBuffer() {
        this(16);
    }

    /**
     * Constructs an empty AsciiBuffer with the specified initial capacity.
     * The allocated capacity will be rounded up to the nearest power of 2 (minimum 16).
     * @param initialCapacity Minimum initial capacity.
     */
    public AsciiBuffer(int initialCapacity) {
        int cap = JgenMath.nextPow2(Math.max(16, initialCapacity));
        this.buffer = new byte[cap];
        this.gapStart = 0;
        this.gapEnd = cap;
    }

    /**
     * Constructs an AsciiBuffer from a segment of a byte array with optional trailing padding.
     * @param array    The source raw byte array.
     * @param srcFrom  Starting index in source array (inclusive).
     * @param srcTo    Ending index in source array (exclusive).
     * @param padding  Extra capacity to reserve in the gap after the set content.
     * @throws NullPointerException if array is null.
     * @throws IllegalArgumentException if padding is negative.
     * @throws IndexOutOfBoundsException if srcFrom or srcTo are out of bounds or srcFrom > srcTo.
     */
    public AsciiBuffer(byte[] array, int srcFrom, int srcTo, int padding) {
        if (array == null) throw new NullPointerException("array cannot be null");
        if (padding < 0) throw new IllegalArgumentException("padding cannot be negative: " + padding);
        Objects.checkFromToIndex(srcFrom, srcTo, array.length);
        int targetLen = normalizedLength(array, srcFrom, srcTo);
        int requiredCapacity = targetLen + padding;
        int cap = JgenMath.nextPow2(Math.max(16, requiredCapacity));
        this.buffer = new byte[cap];
        this.gapStart = targetLen;
        this.gapEnd = cap;
        if (targetLen > 0) normalize(array, srcFrom, srcTo, this.buffer, 0);
    }

    public AsciiBuffer(byte[] array, int srcFrom, int srcTo) {
        this(array, srcFrom, srcTo, 0);
    }

    public AsciiBuffer(byte[] array, int padding) {
        this(array, 0, array == null ? 0 : array.length, padding);
    }

    public AsciiBuffer(byte[] array) {
        this(array, 0, array == null ? 0 : array.length, 0);
    }

    /**
     * Constructs an AsciiBuffer from a String with optional trailing padding.
     * @param str      The source String.
     * @param padding  Extra capacity to reserve in the gap after the set content.
     * @throws NullPointerException if str is null.
     * @throws IllegalArgumentException if padding is negative.
     */
    public AsciiBuffer(String str, int padding) {
        if (str == null) throw new NullPointerException("str cannot be null");
        if (padding < 0) throw new IllegalArgumentException("padding cannot be negative: " + padding);
        int targetLen = normalizedLength(str);
        int requiredCapacity = targetLen + padding;
        int cap = JgenMath.nextPow2(Math.max(16, requiredCapacity));
        this.buffer = new byte[cap];
        this.gapStart = targetLen;
        this.gapEnd = cap;
        if (targetLen > 0) normalize(str, this.buffer, 0);
    }

    public AsciiBuffer(String str) {
        this(str, 0);
    }

    // =============================================================================
    // SET
    // =============================================================================

    /**
     * Replaces the contents of this buffer with a segment of another AsciiBuffer and
     * reserves optional trailing padding.
     *
     * @param text     The source AsciiBuffer.
     * @param srcFrom  Starting index in source buffer (inclusive).
     * @param srcTo    Ending index in source buffer (exclusive).
     * @param padding  Extra capacity to reserve in the gap after the set content.
     * @return The new cursor position (gapStart).
     * @throws IllegalArgumentException if padding is negative or if text is this buffer.
     * @throws IndexOutOfBoundsException if srcFrom or srcTo are out of bounds or srcFrom > srcTo.
     */
    public int set(AsciiBuffer text, int srcFrom, int srcTo, int padding) {
        if (this == text) throw new IllegalArgumentException("Cannot set AsciiBuffer to a segment of itself.");
        if (padding < 0) throw new IllegalArgumentException("padding cannot be negative: " + padding);
        Objects.checkFromToIndex(srcFrom, srcTo, text.length());
        int targetLen = srcTo - srcFrom;
        int requiredCapacity = targetLen + padding;
        ensureCapacity(requiredCapacity);
        if (targetLen > 0) {
            int srcGapStart = text.gapStart;
            int srcGapLen = text.gapEnd - text.gapStart;
            if (srcTo <= srcGapStart) { // Segment lies entirely before source gap
                System.arraycopy(text.buffer, srcFrom, this.buffer, 0, targetLen);
            } else if (srcFrom >= srcGapStart) { // Segment lies entirely after source gap
                System.arraycopy(text.buffer, srcFrom + srcGapLen, this.buffer, 0, targetLen);
            } else { // Segment straddles source gap (2 contiguous block copies)
                int headLen = srcGapStart - srcFrom;
                int tailLen = targetLen - headLen;
                System.arraycopy(text.buffer, srcFrom, this.buffer, 0, headLen);
                System.arraycopy(text.buffer, srcGapStart + srcGapLen, this.buffer, headLen, tailLen);
            }
        }
        this.gapStart = targetLen;
        this.gapEnd = this.buffer.length;
        return this.gapStart;
    }

    public int set(AsciiBuffer text, int srcFrom, int srcTo) {
        return set(text, srcFrom, srcTo, 0);
    }

    public int set(AsciiBuffer text) {
        return set(text, 0, text.length(), 0);
    }

    /**
     * Normalizes and replaces the contents of this buffer with a segment of a byte array,
     * reserving optional trailing padding.
     * @param array    The source raw byte array.
     * @param srcFrom  Starting index in source array (inclusive).
     * @param srcTo    Ending index in source array (exclusive).
     * @param padding  Extra capacity to reserve in the gap after the set content.
     * @return The new cursor position (gapStart).
     * @throws IllegalArgumentException if padding is negative.
     * @throws IndexOutOfBoundsException if srcFrom or srcTo are out of bounds or srcFrom > srcTo.
     */
    public int set(byte[] array, int srcFrom, int srcTo, int padding) {
        if (padding < 0) throw new IllegalArgumentException("padding cannot be negative: " + padding);
        Objects.checkFromToIndex(srcFrom, srcTo, array.length);
        int targetLen = normalizedLength(array, srcFrom, srcTo);
        int requiredCapacity = targetLen + padding;
        ensureCapacity(requiredCapacity);
        if (targetLen > 0) normalize(array, srcFrom, srcTo, buffer, 0);
        gapStart = targetLen;
        gapEnd = buffer.length;
        return gapStart;
    }

    public int set(byte[] array, int srcFrom, int srcTo) {
        return set(array, srcFrom, srcTo, 0);
    }

    public int set(byte[] array, int padding) {
        return set(array, 0, array.length, padding);
    }

    public int set(byte[] array) {
        return set(array, 0, array.length, 0);
    }

    /**
     * Normalizes and replaces the contents of this buffer with a String,
     * reserving optional trailing padding.
     * @param str      The source String.
     * @param padding  Extra capacity to reserve in the gap after the set content.
     * @return The new cursor position (gapStart).
     * @throws IllegalArgumentException if padding is negative.
     * @throws NullPointerException if str is null.
     */
    public int set(String str, int padding) {
        if (padding < 0) throw new IllegalArgumentException("padding cannot be negative: " + padding);
        Objects.requireNonNull(str, "str cannot be null");
        int targetLen = normalizedLength(str);
        int requiredCapacity = targetLen + padding;
        ensureCapacity(requiredCapacity);
        if (targetLen > 0) normalize(str, buffer, 0);
        gapStart = targetLen;
        gapEnd = buffer.length;
        return gapStart;
    }

    public int set(String str) {
        return set(str, 0);
    }


    // =============================================================================
    // INSERT
    // =============================================================================

    /**
     * Replaces a target logical index range in this buffer with a range from a source AsciiBuffer.
     * Operates safely across source and destination gap boundaries in a single pass.
     * @param src     Source AsciiBuffer.
     * @param srcFrom Starting index in source buffer.
     * @param srcTo   Ending index in source buffer.
     * @param dstFrom Starting index in destination range.
     * @param dstTo   Ending index in destination range.
     * @return New cursor position (gapStart).
     * @throws IllegalArgumentException if src is this buffer.
     */
    public int insertReplace(AsciiBuffer src, int srcFrom, int srcTo, int dstFrom, int dstTo) {
        if (this == src) throw new IllegalArgumentException("Cannot replace AsciiBuffer with a segment of itself.");
        int srcLen = src.length();
        int srcMin = Math.clamp(Math.min(srcFrom, srcTo), 0, srcLen);
        int srcMax = Math.clamp(Math.max(srcFrom, srcTo), 0, srcLen);
        int srcCount = srcMax - srcMin;
        int dstLen = this.length();
        int dstMin = Math.clamp(Math.min(dstFrom, dstTo), 0, dstLen);
        int dstMax = Math.clamp(Math.max(dstFrom, dstTo), 0, dstLen);
        int dstCount = dstMax - dstMin;
        setCursorIndex(dstMin); // Move cursor/gap to replacement index FIRST
        // Ensure capacity AFTER cursor is in position, but BEFORE modifying gapEnd
        int netChange = srcCount - dstCount;
        ensureCapacity(dstLen + Math.max(0, netChange));
        // Expand gap to erase the destination range
        if (dstCount > 0) this.gapEnd += dstCount;
        // Copy source range into gap
        if (srcCount > 0) {
            int srcGapStart = src.gapStart;
            int srcGapLength = src.gapEnd - src.gapStart;
            if (srcMax <= srcGapStart) { // Entire source segment lies BEFORE source gap
                System.arraycopy(src.buffer, srcMin, this.buffer, this.gapStart, srcCount);
            } else if (srcMin >= srcGapStart) { // Entire source segment lies AFTER source gap
                System.arraycopy(src.buffer, srcMin + srcGapLength, this.buffer, this.gapStart, srcCount);
            } else { // Source segment STRADDLES source gap (2 contiguous block copies)
                int headLen = srcGapStart - srcMin;
                int tailLen = srcCount - headLen;
                System.arraycopy(src.buffer, srcMin, this.buffer, this.gapStart, headLen);
                System.arraycopy(src.buffer, srcGapStart + srcGapLength, this.buffer, this.gapStart + headLen, tailLen);
            } this.gapStart += srcCount;
        } return this.gapStart;
    }

    public int insertReplace(AsciiBuffer src, int dstFrom, int dstTo) {
        return insertReplace(src, 0, src.length(), dstFrom, dstTo);
    }

    public int insert(byte c) {
        return insert(c, gapStart);
    }

    public int insert(byte c, int index) {
        if (isValidInternalFormat(c)) {
            setCursorIndex(index);
            ensureCapacity(length() + 1);
            buffer[gapStart++] = c;
        } return gapStart;
    }

    public int insert(AsciiBuffer text) {
        return insert(text, gapStart);
    }

    public int insert(AsciiBuffer text, int index) {
        return insertReplace(text, 0, text.length(), index, index);
    }

    public int insert(AsciiBuffer text, int srcFrom, int srcTo, int index) {
        return insertReplace(text, srcFrom, srcTo, index, index);
    }

    // =============================================================================
    // DELETE
    // =============================================================================

    public int delete(int fromIndex, int toIndex) {
        int dstLen = length();
        int dstMin = Math.clamp(Math.min(fromIndex, toIndex), 0, dstLen);
        int dstMax = Math.clamp(Math.max(fromIndex, toIndex), 0, dstLen);
        int dstCount = dstMax - dstMin;
        if (dstCount > 0) {
            setCursorIndex(dstMin);
            gapEnd += dstCount;
        } return gapStart;
    }

    public int delete(int count) {
        if (count <= 0) return gapStart;
        return delete(gapStart, gapStart + count);
    }

    public int delete() {
        if (gapStart > 0) gapStart--;
        return gapStart;
    }

    public int deleteForward() {
        if (gapEnd < buffer.length) gapEnd++;
        return gapStart;
    }

    /**
     * Resets the buffer to an empty state in O(1) time.
     * Keeps the existing backing array allocation, moving the gap to span the full capacity.
     * @return New cursor index (0)
     */
    public int clear() {
        gapStart = 0;
        gapEnd = buffer.length;
        return gapStart;
    }

    // =============================================================================
    // CURSOR
    // =============================================================================

    /**
     * @return Current cursor position (logical index)
     */
    public int cursorIndex() {
        return gapStart;
    }

    public int setCursorIndex(int index) {
        if (index == gapStart) return gapStart;
        int length = length();
        index = Math.clamp(index,0,length);
        if (index < gapStart) {
            // Moving cursor left: shift bytes from left of gap to right of gap
            int moveCount = gapStart - index;
            System.arraycopy(buffer, index, buffer, gapEnd - moveCount, moveCount);
            gapStart -= moveCount;
            gapEnd -= moveCount;
        } else if (index > gapStart) {
            // Moving cursor right: shift bytes from right of gap to left of gap
            int moveCount = index - gapStart;
            System.arraycopy(buffer, gapEnd, buffer, gapStart, moveCount);
            gapStart += moveCount;
            gapEnd += moveCount;
        } return gapStart;
    }

    /**
     * Attempts to move the cursor one logical position to the left.
     * @return new cursor index
     */
    public int moveCursorLeft() {
        setCursorIndex(gapStart - 1);
        return gapStart;
    }

    /**
     * Attempts to move the cursor one logical position to the right.
     * @return new cursor index
     */
    public int moveCursorRight() {
        setCursorIndex(gapStart + 1);
        return gapStart;
    }

    // =============================================================================
    // SEARCH & INSPECTION
    // =============================================================================

    /**
     * Reads the ASCII byte at the current cursor position without moving the cursor or gap.
     * @return The ASCII byte value (0..127) at the cursor, or -1 if the cursor is at end of buffer.
     */
    public byte get() {
        if (gapStart >= length()) return -1;
        return buffer[gapEnd];
    }

    /**
     * Reads the ASCII byte at a specific logical character index without moving the cursor or gap.
     * @param index Logical character index (0 to length() - 1).
     * @return The ASCII byte value (0..127) at index, or -1 if index is out of bounds.
     */
    public byte get(int index) {
        if (index < 0 || index >= length()) return -1;
        return index < gapStart ? (buffer[index]) : (buffer[gapEnd + (index - gapStart)]);
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
        if (!isValidInternalFormat(target)) return -1;
        for (int i = fromIndex; i < len; i++) {
            if (get(i) == target) return i;
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
     * Searches backward for a target byte starting from a specific logical character index.
     * @param target    Byte to find.
     * @param fromIndex Starting logical character index (Inclusive. Searches backward towards index 0).
     * @return Logical index of last occurrence, or -1 if not found.
     */
    public int lastIndexOf(byte target, int fromIndex) {
        int len = length();
        if (len == 0) return -1;
        if (fromIndex >= len) fromIndex = len - 1;
        if (fromIndex < 0) return -1;
        if (!isValidInternalFormat(target)) return -1;
        for (int i = fromIndex; i >= 0; i--) {
            if (get(i) == target) return i;
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

    // todo: So ATP we have search functionality for singular characters. Which is fine, and could be enough for
    //  The foundational Buffer structure.


    // =============================================================================
    // TO STRING
    // =============================================================================

    /**
     * Converts active characters to a String matching internal engine representation ('\n' line breaks).
     */
    public String asNormalizedString() {
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

    /**
     * Exports active characters to a String formatted with the host OS's native line breaks (e.g. \r\n on Windows).
     */
    public String asExportableString() {
        String normalizedString = asNormalizedString();
        String lineSep = System.lineSeparator();
        // If system line separator is just \n (Linux / macOS), internal matches external
        if (lineSep.equals("\n")) return normalizedString;
        if (normalizedString.isEmpty()) return "";
        // On platforms with \r\n (Windows), expand \n to \r\n
        StringBuilder builder = new StringBuilder(normalizedString.length() + 16);
        for (int i = 0; i < normalizedString.length(); i++) {
            char c = normalizedString.charAt(i);
            if (c == '\n') {
                builder.append('\r');
            } builder.append(c);
        } return builder.toString();
    }

    @Override
    public String toString() {
        return "AsciiBuffer[length=" + length() + ", capacity=" + buffer.length + "]";
    }

    // =============================================================================
    // SPLIT / SUBTEXT
    // =============================================================================


    public AsciiBuffer[] split(byte c) {
        if (!isValidInternalFormat(c)) {
            return new AsciiBuffer[] { copy() };
        }
        // todo
        //  if character is "new line", how do we split on multiple consectutive newlines?
        //  How does java do this traditionally
        return null;
    }

    public AsciiBuffer subText(int fromIndex, int toIndex) {
        // todo: discuss
        return null;
    }

    // =============================================================================
    // COPY
    // =============================================================================

    public AsciiBuffer copy() {
        // todo: discuss. (view? no, probably not. Has to be a new backing array)
        return null;
    }

    // =============================================================================
    // OTHER
    // =============================================================================


    /**
     * Logical length of the text. Equivalent of String length().
     * @return Number of active characters in the buffer
     */
    public int length() {
        return buffer.length - (gapEnd - gapStart);
    }

    /**
     * @return Total allocated byte capacity of the backing buffer
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
    // INTERNAL UTILITY METHODS
    // =============================================================================

    public static final byte TAB = 0x09;             // '\t'
    public static final byte LINE_FEED = 0x0A;       // '\n'
    public static final byte CARRIAGE_RETURN = 0x0D; // '\r'

    /**
     * Checks if a character byte or character code point matches valid internal ASCII format.
     */
    private static boolean isValidInternalFormat(int c) {
        if (c >= 0 && c < 127) {
            if (c < 32) return c == LINE_FEED || c == TAB;
            return true;
        } return false;
    }

    /**
     * Calculates the exact byte length required to store a String in valid internal ASCII format.
     */
    private static int normalizedLength(String str) {
        int len = str.length();
        if (len == 0) return 0;
        int count = 0;
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if (c == CARRIAGE_RETURN) {
                count++;
                if (i + 1 < len && str.charAt(i + 1) == LINE_FEED) i++;
            } else if (isValidInternalFormat(c)) count++;
        } return count;
    }

    /**
     * Calculates the exact byte length required to store a byte array range in valid internal ASCII format.
     * @throws IndexOutOfBoundsException if srcFrom or srcTo are out of bounds or srcFrom > srcTo.
     */
    private static int normalizedLength(byte[] src, int srcFrom, int srcTo) {
        Objects.checkFromToIndex(srcFrom, srcTo, src.length);
        if (srcFrom == srcTo) return 0;
        int count = 0;
        for (int i = srcFrom; i < srcTo; i++) {
            byte b = src[i];
            if (b == CARRIAGE_RETURN) { count++;
                if (i + 1 < srcTo && src[i + 1] == LINE_FEED) i++;
            } else if (isValidInternalFormat(b)) count++;
        } return count;
    }

    /**
     * Calculates the exact byte length required to store an entire byte array in valid internal ASCII format.
     */
    private static int normalizedLength(byte[] src) {
        return normalizedLength(src, 0, src.length);
    }

    /**
     * Normalizes a String and writes the resulting ASCII bytes directly into a destination byte array.
     */
    private static int normalize(String src, byte[] dst, int dstFrom) {
        int len = src.length();
        if (len == 0) return 0;
        int count = 0;
        for (int i = 0; i < len; i++) {
            char c = src.charAt(i);
            if (c == CARRIAGE_RETURN) {
                dst[dstFrom + count++] = LINE_FEED;
                if (i + 1 < len && src.charAt(i + 1) == LINE_FEED) i++;
            } else if (isValidInternalFormat(c)) {
                dst[dstFrom + count++] = (byte) c;
            }
        }
        return count;
    }

    /**
     * Normalizes a segment of a source byte array and writes the resulting ASCII bytes directly into a destination byte array.
     * @throws IndexOutOfBoundsException if src range is invalid.
     */
    private static int normalize(byte[] src, int srcFrom, int srcTo, byte[] dst, int dstFrom) {
        Objects.checkFromToIndex(srcFrom, srcTo, src.length);
        if (srcFrom == srcTo) return 0;
        int count = 0;
        for (int i = srcFrom; i < srcTo; i++) {
            byte b = src[i];
            if (b == CARRIAGE_RETURN) {
                dst[dstFrom + count++] = LINE_FEED;
                if (i + 1 < srcTo && src[i + 1] == LINE_FEED) i++;
            } else if (isValidInternalFormat(b)) {
                dst[dstFrom + count++] = b;
            }
        }
        return count;
    }

    /**
     * Normalizes an entire source byte array and writes the resulting ASCII bytes directly into a destination byte array.
     */
    private static int normalize(byte[] src, byte[] dst, int dstFrom) {
        return normalize(src, 0, src.length, dst, dstFrom);
    }

}
