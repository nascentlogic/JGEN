package io.github.nascentlogic.jgen.gfx;

import org.lwjgl.system.MemoryUtil;
import org.tinylog.Logger;

import java.nio.*;

/**
 * F.Dahl, 6/19/2026
 */
public class Buffers {


    /**
     * Performs a memory block transfer (blit) from a source
     * Buffer to a destination Buffer.
     * <p>This method works seamlessly across any combination of direct (native)
     * and heap-allocated buffers. Position and limit tracking markers of both buffers remain
     * completely unchanged after the transfer.
     * <p>The transferred range spans inclusively from {@code src.position()} to
     * {@code src.position() + src.remaining() - 1}, writing into the destination
     * buffer starting exactly at {@code dst.position()}.
     * @param src The source buffer providing the data.
     * @param dst The destination buffer receiving the data.
     */
    public static void blit(ByteBuffer src, ByteBuffer dst) {
        if (!isValidBlitTransfer(src, dst)) return;
        int count = src.remaining();
        if (src.isDirect() && dst.isDirect()) {
            MemoryUtil.memCopy(MemoryUtil.memAddress(src) + src.position(), MemoryUtil.memAddress(dst) + dst.position(), count);
        } else if (!src.isDirect() && !dst.isDirect() && src.hasArray() && dst.hasArray()) {
            System.arraycopy(src.array(), src.arrayOffset() + src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else if (!src.isDirect() && dst.isDirect() && src.hasArray()) {
            dst.put(dst.position(), src.array(), src.arrayOffset() + src.position(), count);
        } else if (src.isDirect() && !dst.isDirect() && dst.hasArray()) {
            src.get(src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else { int srcPos = src.position();
            int dstPos = dst.position();
            for (int i = 0; i < count; i++) dst.put(dstPos + i, src.get(srcPos + i));
        }
    }
    /** @see #blit(ByteBuffer, ByteBuffer) */
    public static void blit(ShortBuffer src, ShortBuffer dst) {
        if (!isValidBlitTransfer(src, dst)) return;
        int count = src.remaining();
        if (src.isDirect() && dst.isDirect()) {
            MemoryUtil.memCopy(
                    MemoryUtil.memAddress(src) + ((long) src.position() * Short.BYTES),
                    MemoryUtil.memAddress(dst) + ((long) dst.position() * Short.BYTES),
                    (long) count * Short.BYTES);
        } else if (!src.isDirect() && !dst.isDirect() && src.hasArray() && dst.hasArray()) {
            System.arraycopy(src.array(), src.arrayOffset() + src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else if (!src.isDirect() && dst.isDirect() && src.hasArray()) {
            dst.put(dst.position(), src.array(), src.arrayOffset() + src.position(), count);
        } else if (src.isDirect() && !dst.isDirect() && dst.hasArray()) {
            src.get(src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else {
            int srcPos = src.position();
            int dstPos = dst.position();
            for (int i = 0; i < count; i++) dst.put(dstPos + i, src.get(srcPos + i));
        }
    }
    /** @see #blit(ByteBuffer, ByteBuffer) */
    public static void blit(IntBuffer src, IntBuffer dst) {
        if (!isValidBlitTransfer(src, dst)) return;
        int count = src.remaining();
        if (src.isDirect() && dst.isDirect()) {
            MemoryUtil.memCopy(
                    MemoryUtil.memAddress(src) + ((long) src.position() * Integer.BYTES),
                    MemoryUtil.memAddress(dst) + ((long) dst.position() * Integer.BYTES),
                    (long) count * Integer.BYTES);
        } else if (!src.isDirect() && !dst.isDirect() && src.hasArray() && dst.hasArray()) {
            System.arraycopy(src.array(), src.arrayOffset() + src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else if (!src.isDirect() && dst.isDirect() && src.hasArray()) {
            dst.put(dst.position(), src.array(), src.arrayOffset() + src.position(), count);
        } else if (src.isDirect() && !dst.isDirect() && dst.hasArray()) {
            src.get(src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else {
            int srcPos = src.position();
            int dstPos = dst.position();
            for (int i = 0; i < count; i++) dst.put(dstPos + i, src.get(srcPos + i));
        }
    }
    /** @see #blit(ByteBuffer, ByteBuffer) */
    public static void blit(FloatBuffer src, FloatBuffer dst) {
        if (!isValidBlitTransfer(src, dst)) return;
        int count = src.remaining();
        if (src.isDirect() && dst.isDirect()) {
            MemoryUtil.memCopy(
                    MemoryUtil.memAddress(src) + ((long) src.position() * Float.BYTES),
                    MemoryUtil.memAddress(dst) + ((long) dst.position() * Float.BYTES),
                    (long) count * Float.BYTES);
        } else if (!src.isDirect() && !dst.isDirect() && src.hasArray() && dst.hasArray()) {
            System.arraycopy(src.array(), src.arrayOffset() + src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else if (!src.isDirect() && dst.isDirect() && src.hasArray()) {
            dst.put(dst.position(), src.array(), src.arrayOffset() + src.position(), count);
        } else if (src.isDirect() && !dst.isDirect() && dst.hasArray()) {
            src.get(src.position(), dst.array(), dst.arrayOffset() + dst.position(), count);
        } else {
            int srcPos = src.position();
            int dstPos = dst.position();
            for (int i = 0; i < count; i++) dst.put(dstPos + i, src.get(srcPos + i));
        }
    }

    private static boolean isValidBlitTransfer(Buffer src, Buffer dst) {
        if (dst.isReadOnly()) {
            Logger.warn("Buffer blit: Unable to write to read-only buffer");
            return false;
        } int count = src.remaining();
        if (count == 0) {
            Logger.warn("Buffer blit: Nothing to write. Src buffer remaining = 0");
            return false;
        } if (dst.remaining() < count) {
            Logger.warn("Buffer blit: Nothing written. Dst buffer required: {}, available: {}", count, dst.remaining());
            return false;
        } return true;
    }




}
