package io.github.nascentlogic.jgen.text;

import io.github.nascentlogic.jgen.utils.Disposable;

import java.nio.ByteBuffer;

/**
 * F.Dahl, 8/12/2026
 */
public abstract class ManagedBuffer implements Disposable {

    int arenaOffset;    // arena start index
    int blockSize;      // allocated memory / buffer capacity  (0 = disposed)

    /**
     * Hook invoked after the arena assigns a new block to this buffer during reallocation or growth. <p>
     * The arena updates the buffer's offset and capacity prior to calling this method.
     * Subclasses are responsible for moving internal data or updating bookkeeping (such as gap pointers).
     * @param oldPos    absolute arena offset of the previous block
     * @param oldSize   capacity in bytes of the previous block
     * @param newPos    absolute arena offset of the newly assigned block
     * @param newSize   capacity in bytes of the newly assigned block
     */
    protected void onBlockChange(int oldPos, int oldSize, int newPos, int newSize) {
        moveGlobal(oldPos, newPos, Math.min(oldSize, newSize));
    }

    /**
     * Hook invoked immediately after this buffer's memory block is freed back to the arena. <p>
     * Subclasses may override this method to reset internal state, such as logical content sizes,
     * cursor positions, or gap pointers. Default implementation is a no-op.
     */
    protected void onFree() { /* no-op */ }

    /**
     * Creates a {@link ByteBuffer} slice representing this buffer's current memory block.<p>
     * The returned buffer's position is 0, and its limit and capacity equal the buffers ManagedBuffer's capacity.<p>
     * @return a {@link ByteBuffer} read only view of the buffer's memory
     * @throws IllegalStateException if this buffer is disposed
     */
    public final ByteBuffer memoryBlockView() {
        if (isDisposed()) throw new IllegalStateException("Buffer is disposed");
        return ByteBuffer.wrap(memory(), arenaOffset, blockSize)
                .slice().asReadOnlyBuffer();
    }

    /**
     * First-time allocation or re-init after free. Throws on failure.
     * @throws IllegalStateException if ManagedBuffer is disposed.
     */
    protected final void allocate(int bytes) {
        if (!isDisposed()) throw new IllegalStateException("Buffer is already allocated");
        ByteArena.get().allocate(this, bytes);
    }

    /**
     * Make sure we have at least this capacity. Returns false if impossible.
     * Will trigger {@link #onBlockChange(int, int, int, int)} if the Buffer grows, before this returns true.
     * @throws IllegalStateException if ManagedBuffer is disposed.
     */
    protected final boolean ensureCapacity(int bytes) {
        if (isDisposed()) throw new IllegalStateException("Buffer is disposed");
        if (bytes <= blockSize) return true;
        return ByteArena.get().ensureCapacity(this, bytes);
    }

    /**
     * Reads a single byte at a local buffer index without capacity bounds checks.
     * @param index local index relative to buffer start (0 to capacity - 1)
     * @return value at index
     * @throws IndexOutOfBoundsException if index is negative or exceeds arena bounds
     */
    protected final byte getByte(int index) {
        return memory()[arenaOffset + index];
    }

    /**
     * Writes a single byte at a local buffer index without capacity bounds checks.
     * @param index local index relative to buffer start (0 to capacity - 1)
     * @param value byte value to write
     * @throws IllegalStateException if ManagedBuffer is disposed
     * @throws IndexOutOfBoundsException if index is negative or exceeds arena bounds
     */
    protected final void setByte(int index, byte value) {
        if (isDisposed()) throw new IllegalStateException("Buffer is disposed");
        memory()[arenaOffset + index] = value;
    }

    /**
     * Internal memmove wrapper operating on raw arena offsets.
     * Fully supports overlapping transfers with zero GC allocations (uses native JVM intrinsics).
     * @param srcPos absolute arena offset of source
     * @param dstPos absolute arena offset of destination
     * @param len    number of bytes to copy
     * @throws IllegalStateException if ManagedBuffer is disposed
     * @throws IllegalArgumentException if length is negative
     * @throws IndexOutOfBoundsException if srcPos, dstPos, or len fall outside arena memory bounds
     */
    protected final void moveGlobal(int srcPos, int dstPos, int len) {
        if (isDisposed()) throw new IllegalStateException("Buffer is disposed");
        if (len < 0) throw new IllegalArgumentException("len < 0");
        if (len == 0 || srcPos == dstPos) return;
        byte[] arena = memory();
        System.arraycopy(arena, srcPos, arena, dstPos, len);
    }

    /**
     * Internal memmove wrapper operating on local buffer indices (0 to capacity - 1).
     * Fully supports overlapping transfers with zero GC allocations (uses native JVM intrinsics).
     * @param srcPos local source index relative to buffer start
     * @param dstPos local destination index relative to buffer start
     * @param len    number of bytes to copy
     * @throws IllegalStateException if ManagedBuffer is disposed
     * @throws IllegalArgumentException if length is negative
     * @throws IndexOutOfBoundsException if indices fall outside buffer boundaries
     */
    protected final void moveLocal(int srcPos, int dstPos, int len) {
        moveGlobal(arenaOffset + srcPos, arenaOffset + dstPos, len);
    }

    /**
     * Copies bytes from an external array into this buffer at a local buffer index. <p>
     * This operation is unchecked and will <b>not</b> call {@link #ensureCapacity(int)}.
     * Callers must ensure the buffer has sufficient capacity prior to writing.
     * @param src    source array
     * @param srcPos starting index in source array
     * @param dstPos local destination index in this buffer
     * @param len    number of bytes to copy
     * @throws IllegalStateException if ManagedBuffer is disposed
     * @throws IndexOutOfBoundsException if write exceeds buffer capacity or array bounds
     */
    protected final void uncheckedWrite(byte[] src, int srcPos, int dstPos, int len) {
        if (isDisposed()) throw new IllegalStateException("Buffer is disposed");
        System.arraycopy(src, srcPos, memory(), arenaOffset + dstPos, len);
    }

    /**
     * Copies bytes from this buffer at a local index into an external destination array.
     * @param srcPos local source index in this buffer
     * @param dst    destination array
     * @param dstPos starting index in destination array
     * @param len    number of bytes to copy
     * @throws IllegalStateException if ManagedBuffer is disposed
     * @throws IndexOutOfBoundsException if read exceeds buffer capacity or array bounds
     */
    protected final void uncheckedRead(int srcPos, byte[] dst, int dstPos, int len) {
        if (isDisposed()) throw new IllegalStateException("Buffer is disposed");
        System.arraycopy(memory(), arenaOffset + srcPos, dst, dstPos, len);
    }

    /**
     * Updates the internal block position and allocated size of this buffer. <p>
     * Called directly by the arena right before notifying {@link #onBlockChange(int, int, int, int)}.
     * @param newPos absolute arena offset of the allocated block
     * @param newSize   allocated size in bytes (block capacity)
     */
    final void setBlock(int newPos, int newSize) {
        arenaOffset = newPos;
        blockSize = newSize;
    }

    /**
     * Returns the size in bytes of the allocated block currently owned by this buffer.
     * Returns 0 if this buffer is disposed.
     * @return allocated block size in bytes
     */
    protected final int blockSize() { return blockSize; }

    /**
     * Returns the absolute start index of this buffer's block within the shared memory arena.
     * @return absolute arena offset
     */
    protected final int arenaOffset() { return arenaOffset; }

    /**
     * Returns the index of the arena segment currently containing this buffer's block.
     * @return segment index (0 to NUM_SEGMENTS - 1)
     */
    protected final int segmentIndex() { return arenaOffset >>> ByteArena.MAX_ORDER; }

    /**
     * Returns the relative offset of this buffer's block within its current arena segment.
     * @return offset within the segment (0 to SEGMENT_SIZE - 1)
     */
    protected final int segmentOffset() { return arenaOffset & (ByteArena.SEGMENT_SIZE - 1); }

    /**
     * Returns the underlying shared byte array backing the entire arena.
     * @return the raw arena memory array
     */
    protected final byte[] memory() { return ByteArena.get().array(); }

    /**
     * Checks whether this buffer has been disposed or freed back to the arena.
     * @return true if disposed or unallocated; false if actively allocated
     */
    public final boolean isDisposed() { return blockSize == 0; }

    /**
     * Frees this buffer's memory block back to the arena and marks it as disposed.
     * Does nothing if the buffer is already disposed.
     */
    public final void free() {
        if (!isDisposed()) {
            ByteArena.get().free(this);
            blockSize = 0;
            onFree();
        }
    }

}
