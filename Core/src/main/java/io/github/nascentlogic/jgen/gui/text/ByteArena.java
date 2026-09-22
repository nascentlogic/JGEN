package io.github.nascentlogic.jgen.gui.text;

import io.github.nascentlogic.jgen.utils.JgenMath;

/**
 * F.Dahl, 8/12/2026
 */
public class ByteArena {

    public static final int MIN_ORDER = 5;
    public static final int MIN_BLOCK_SIZE = 1 << MIN_ORDER; // 32 B
    public static final int MAX_ORDER = 20;
    public static final int SEGMENT_SIZE = 1 << MAX_ORDER; // 1M B
    public static final int NUM_SEGMENTS = 16;
    public static final int SIZE = SEGMENT_SIZE * NUM_SEGMENTS; // 16 MB

    private final byte[] array = new byte[SIZE];
    private final Segment[] segments = new Segment[NUM_SEGMENTS];
    private final Size tmpSize0 = new Size();
    private final Size tmpSize1 = new Size();
    private final Block tmpBlock0 = new Block();
    private final Block tmpBlock1 = new Block();
    private long totalAllocatedBytes = 0; // Tracking field
    public long totalAllocatedBytes() { return totalAllocatedBytes; }

    private static ByteArena instance;
    private ByteArena() {
        for (int i = 0; i < segments.length; i++)
            segments[i] = new Segment();
    } protected static ByteArena get() {
        if (instance == null)
            instance = new ByteArena();
        return instance;
    }


    byte[] array() { return array; }

    final void allocate(ManagedBuffer buf, int bytes) {
        Size request = tmpSize0.setBytes(bytes);
        if (request.bytes > SEGMENT_SIZE)
            throw new IllegalArgumentException("Requested size exceeds segment capacity");
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].allocate(request, tmpBlock0)) {
                tmpBlock0.segment = i;
                totalAllocatedBytes += tmpBlock0.size.bytes;
                buf.setBlock(tmpBlock0.arenaOffset(), tmpBlock0.size.bytes);
                return;
            }
        } throw new IllegalStateException("Arena exhausted");
    }

    final boolean ensureCapacity(ManagedBuffer buf, int bytes) {
        Size request = tmpSize0.setBytes(bytes);
        if (request.bytes > SEGMENT_SIZE) return false;

        final int oldOffset = buf.arenaOffset();
        final int oldSize   = buf.blockSize();
        final int segIndex  = buf.segmentIndex();
        final int segOffset = buf.segmentOffset();
        tmpBlock0.set(segIndex, segOffset, JgenMath.log2iFloor(oldSize));

        // 1. Classic in-place buddy growth (offset may move left)
        if (segments[segIndex].tryGrowInPlace(tmpBlock0, request, tmpBlock1)) {
            tmpBlock1.segment = segIndex;
            final int newOffset = tmpBlock1.arenaOffset();
            final int newSize   = tmpBlock1.size.bytes;
            totalAllocatedBytes += (newSize - oldSize);
            buf.setBlock(newOffset, newSize);
            buf.onBlockChange(oldOffset, oldSize, newOffset, newSize);
            return true;
        }

        // 2. Allocate a new block (old block still live)
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].allocate(request, tmpBlock1)) {
                tmpBlock1.segment = i;
                final int newOffset = tmpBlock1.arenaOffset();
                final int newSize   = tmpBlock1.size.bytes;
                totalAllocatedBytes += (newSize - oldSize);
                buf.setBlock(newOffset, newSize);
                buf.onBlockChange(oldOffset, oldSize, newOffset, newSize);
                // 3. Free old only after the implementation has copied
                segments[segIndex].free(tmpBlock0);
                return true;
            }
        }

        // Original block untouched
        return false;
    }

    /** Dump the whole arena. */
    void dump() { dump(System.out); }
    void dump(java.io.PrintStream out) {
        out.println("=== ByteArena dump ===");
        out.println("SEGMENT_SIZE = " + SEGMENT_SIZE + "  NUM_SEGMENTS = " + NUM_SEGMENTS);
        out.println();
        for (int i = 0; i < segments.length; i++) {
            out.printf("--- Segment %d %s ---%n", i, segments[i].isCompletelyFree() ? "(completely free)" : "");
            segments[i].dump(out);
            out.println();
        }
    }

    final void free(ManagedBuffer buf) {
        int order = JgenMath.log2iFloor(buf.blockSize());
        tmpBlock0.set(buf.segmentIndex(), buf.segmentOffset(), order);
        totalAllocatedBytes -= tmpBlock0.size.bytes;
        segments[tmpBlock0.segment].free(tmpBlock0);
    }


    static final class Size {
        int order;
        int bytes;
        Size() { setOrder(MIN_ORDER); }
        Size(int bytes) { setBytes(bytes); }
        Size setBytes(int bytes) {
            this.bytes = JgenMath.nextPow2(Math.max(bytes, MIN_BLOCK_SIZE));
            this.order = JgenMath.log2iFloor(this.bytes);
            return this;
        } Size setOrder(int order) {
            this.order = Math.max(order, MIN_ORDER);
            this.bytes = 1 << order;
            return this;
        }
    }

    static final class Block {
        final Size size = new Size();
        int segment;
        int offset;
        int arenaOffset() { return segment * SEGMENT_SIZE + offset; }
        Block set(int segment, int offset, int order) {
            this.segment = segment;
            this.offset = offset;
            this.size.setOrder(order);
            return this;
        }
    }

    static final class Segment {
        private final long[][] freeBitmaps;
        private final int[] freeCount;
        private final Size tmpSize = new Size();

        Segment() {
            this.freeCount = new int[MAX_ORDER + 1];
            this.freeBitmaps = new long[MAX_ORDER + 1][];
            for (int order = MIN_ORDER; order <= MAX_ORDER; order++) {
                int blocksAtOrder = SEGMENT_SIZE >>> order;
                int words = (blocksAtOrder + 63) >>> 6;
                freeBitmaps[order] = new long[words];
            } markFree(MAX_ORDER, 0);
        }

        boolean isCompletelyFree() {
            return freeCount[MAX_ORDER] == 1;
        }

        boolean allocate(Size request, Block dst) {
            int currentOrder = request.order;
            while (currentOrder <= MAX_ORDER && freeCount[currentOrder] == 0) currentOrder++;
            if (currentOrder > MAX_ORDER) return false;
            int offset = popFirstFreeOffset(currentOrder);
            while (currentOrder > request.order) { currentOrder--;
                markFree(currentOrder, offset + (1 << currentOrder));
            } dst.set(0, offset, request.order); // segment index filled by caller
            return true;
        }

        /**
         * Try to grow the allocated block to the target size by absorbing
         * free buddies (classic buddy merge). The start offset may move
         * downward. The original block is never marked free.
         * On success dst receives the final (offset, order).
         */
        boolean tryGrowInPlace(Block block, Size target, Block dst) {
            if (target.order <= block.size.order) {
                dst.set(block.segment, block.offset, block.size.order);
                return true;
            }
            int order  = block.size.order;
            int offset = block.offset;
            // Probe – only succeed if every needed buddy is free
            int probeOrder  = order;
            int probeOffset = offset;
            while (probeOrder < target.order) {
                int buddy = probeOffset ^ (1 << probeOrder);
                if (!isFree(probeOrder, buddy)) return false;
                probeOffset = Math.min(probeOffset, buddy);
                probeOrder++;
            }
            // Commit – absorb free buddies only
            while (order < target.order) {
                int buddy = offset ^ (1 << order);
                unmarkFree(order, buddy);
                offset = Math.min(offset, buddy);
                order++;
            }
            dst.set(block.segment, offset, target.order);
            return true;
        }

        void dump(java.io.PrintStream out) {
            for (int order = MIN_ORDER; order <= MAX_ORDER; order++) {
                int count = freeCount[order];
                if (count == 0) continue; // keep output short
                out.printf("  order %2d (%4d B): %d free block(s)%n",  order, 1 << order, count);
                // Optional: list the actual offsets (useful with tiny test sizes)
                long[] bitmap = freeBitmaps[order];
                for (int w = 0; w < bitmap.length; w++) {
                    long word = bitmap[w];
                    while (word != 0) {
                        int bit = Long.numberOfTrailingZeros(word);
                        int blockIdx = (w << 6) + bit;
                        int offset = blockIdx << order;
                        out.printf("      offset %d%n", offset);
                        word &= word - 1;// clear lowest set bit
                    }
                }
            }
        }

        void free(Block block) {
            int order = block.size.order;
            int offset = block.offset;
            while (order < MAX_ORDER) {
                int buddy = offset ^ (1 << order);
                if (!isFree(order, buddy)) break;
                unmarkFree(order, buddy);
                offset = Math.min(offset, buddy);
                order++;
            } markFree(order, offset);
        }

        private int popFirstFreeOffset(int order) {
            long[] bitmap = freeBitmaps[order];
            for (int w = 0; w < bitmap.length; w++) {
                long word = bitmap[w];
                if (word != 0L) {
                    int bitIdx = Long.numberOfTrailingZeros(word);
                    int blockIdx = (w << 6) + bitIdx;
                    int offset = blockIdx << order;
                    unmarkFree(order, offset);
                    return offset;
                }
            }
            throw new IllegalStateException();
        }

        private boolean isFree(int order, int offset) {
            int blockIdx = offset >>> order;
            return (freeBitmaps[order][blockIdx >>> 6] & (1L << (blockIdx & 63))) != 0;
        }

        private void markFree(int order, int offset) {
            int blockIdx = offset >>> order;
            freeBitmaps[order][blockIdx >>> 6] |= (1L << (blockIdx & 63));
            freeCount[order]++;
        }

        private void unmarkFree(int order, int offset) {
            int blockIdx = offset >>> order;
            freeBitmaps[order][blockIdx >>> 6] &= ~(1L << (blockIdx & 63));
            freeCount[order]--;
        }
    }
}
