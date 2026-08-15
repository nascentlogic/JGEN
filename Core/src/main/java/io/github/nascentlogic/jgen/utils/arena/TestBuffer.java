package io.github.nascentlogic.jgen.utils.arena;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

/**
 * F.Dahl, 8/14/2026
 */
public class TestBuffer extends ManagedBuffer {

    int size;

    public TestBuffer(byte[] array) {
        Objects.requireNonNull(array);
        this(array.length);
        write(array);
    }

    public TestBuffer(int requestedSize) {
        if (requestedSize <= 0) throw new IllegalArgumentException("requested size <= 0");
        allocate(requestedSize);
    }

    @Override
    protected void onBlockChange(int oldPos, int oldSize, int newPos, int newSize) {
        // moveGlobal(oldPos, newPos, size); // if size == 0, moveGlobal reurns.
        if (oldPos == newPos || size == 0) return;
        // 1. Always copy live payload first
        moveGlobal(oldPos, newPos, size);
        // 2. Safely clear ONLY the old memory region that is NOT part of the new block
        byte[] arena = memory();
        if (newPos < oldPos) {
            // Moved LEFT: Old region ends at oldPos + size.
            // The portion from max(oldPos, newPos + size) to (oldPos + size) is no longer used.
            int clearStart = Math.max(oldPos, newPos + size);
            int clearEnd = oldPos + size;
            if (clearEnd > clearStart) {
                Arrays.fill(arena, clearStart, clearEnd, (byte) 0);
            }
        } else {
            // Moved RIGHT: Old region starts at oldPos.
            // The portion from oldPos to min(newPos, oldPos + size) is no longer used.
            int clearEnd = Math.min(newPos, oldPos + size);
            if (clearEnd > oldPos) {
                Arrays.fill(arena, oldPos, clearEnd, (byte) 0);
            }
        }
    }

    @Override
    protected void onFree() {
        size = 0;
    }

    @Override
    protected ByteBuffer asByteBuffer() {
        if (isDisposed()) throw new IllegalStateException("Buffer is disposed");
        return ByteBuffer.wrap(memory(), arenaOffset(), size)
                .slice().asReadOnlyBuffer();
    }

    public void write(byte value) {
        if (!ensureCapacity(size + 1))
            throw new IllegalStateException("Arena capacity exhausted");
        setByte(size,value);
        size++;
    }

    public void write(byte[] array) {
        Objects.requireNonNull(array);
        int len = array.length;
        if (len == 0) return;
        if (!ensureCapacity(size + len))
            throw new IllegalStateException("Arena capacity exhausted");
        uncheckedWrite(array, 0, size, len);
        size += len;
    }

    public void clearZero() {
        if (size > 0) {
            byte[] arena = memory();
            Arrays.fill(arena,arenaOffset(),arenaOffset() + size,(byte) 0);
        }
    }

    public byte get(int index) {
        boundsCheck(index);
        return getByte(index);
    }

    public int capacity() {
        return blockSize();
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int getSize() {
        return size;
    }

    private void boundsCheck(int index) {
        if (index < 0 || index >= size)
            throw new IndexOutOfBoundsException("index: " + index + "out of bounds: [0 -> " + size + " - 1]");
    }
}
