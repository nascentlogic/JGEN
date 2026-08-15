package io.github.nascentlogic.jgen.utils.arena;

import io.github.nascentlogic.jgen.utils.JgenUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * F.Dahl, 8/14/2026
 */
public class ArenaTest {

    private static final Random random = new Random();
    private static final List<TestBuffer> buffers = new ArrayList<>(1024);
    private static final int LOOPS = 50;
    private static final int OPERATIONS = 5_000;

    // Operation weights (Total: 100)
    private static final int OP_ALLOCATE = 20; // 40% chance
    private static final int OP_WRITE    = 25; // 40% chance
    private static final int OP_FREE     = 20; // 20% chance
    private static final int OP_SUM      = OP_ALLOCATE + OP_WRITE + OP_FREE;

    static void main() {
        System.out.println("Starting ByteArena Stress Test...\n");
        for (int i = 0; i < LOOPS; i++) {
            System.out.printf("--- Beginning Loop %d/%d ---%n", i + 1, LOOPS);
            for (int j = 0; j < OPERATIONS; j++) {
                int operation = random.nextInt(OP_SUM);
                try {
                    if (buffers.isEmpty() || operation < OP_ALLOCATE) {
                        doAllocate();
                    } else if (operation < (OP_ALLOCATE + OP_WRITE)) {
                        doWrite();
                    } else  {
                        doFree();
                    }
                    // Integrity check active buffers
                    verifyActiveBuffers();
                } catch (IllegalStateException e) {
                    System.out.printf("Arena exhausted at op %d (Active buffers: %d). Clearing batch.%n", j, buffers.size());
                    break;
                }
            }
            freeAndDump();
        }
        System.out.println("\nALL ARENA TESTS PASSED SUCCESSFULLY!");
    }

    // =============================================================================
    // OPERATIONS
    // =============================================================================

    private static void doAllocate() {
        int size = randomValidSize();
        TestBuffer buf = new TestBuffer(createArray(size));
        buffers.add(buf);
    }

    private static void doWrite() {
        // only called if !buffers.isEmty()
        TestBuffer buf = getRandomBuffer();
        int size = Math.max(randomValidSize() / 8,1); // writes are much smaller.
        byte[] array = createArray(size);
        buf.write(array);
    }

    private static void doFree() {
        // only called if !buffers.isEmty()
        int index = random.nextInt(buffers.size());
        TestBuffer buf = buffers.get(index);
        buf.clearZero();
        buf.free();
        removeBufferAt(index);
    }

    // =============================================================================
    // INTEGRITY CHECKS
    // =============================================================================

    private static void verifyActiveBuffers() {
        for (TestBuffer buf : buffers) {
            for (int i = 0; i < buf.getSize(); i++) {
                if (buf.get(i) != (byte) 1) {
                    throw new AssertionError("Data corruption detected at buffer index " + i);
                }
            }
        }
    }

    private static void verifyArenaCompletelyZeroed() {
        byte[] arena = ByteArena.get().array();
        for (int i = 0; i < arena.length; i++) {
            if (arena[i] != 0) {
                System.err.printf("MEMORY CORRUPTION AT ARENA INDEX %d: byte value = %d%n", i, arena[i]);
                ByteArena.get().dump();
                throw new AssertionError("Arena memory dirty after freeing all buffers!");
            }
        }
    }

    // =============================================================================
    // UTILITY
    // =============================================================================


    private static void removeBufferAt(int index) {
        int lastIndex = buffers.size() - 1;
        TestBuffer last = buffers.remove(lastIndex);
        if (index < lastIndex) {
            buffers.set(index, last);
        }
    }

    private static TestBuffer getRandomBuffer() {
        return buffers.get(random.nextInt(buffers.size()));
    }

    private static int randomValidSize() {
        int max = ByteArena.SEGMENT_SIZE / 4;
        int min = 1;
        return random.nextInt(min,max);
    }


    private static byte[] createArray(int size) {
        byte[] array = new byte[size];
        Arrays.fill(array,(byte) 1);
        return array;
    }

    private static void freeAndDump() {
        for (TestBuffer buffer : buffers) {
            buffer.clearZero();
            buffer.free();
        }
        buffers.clear();
        System.out.println("\n====== All buffers disposd ======\n");
        printArenaMemoryUsage();
        // Check 1: Verify total tracked memory is 0
        if (ByteArena.get().totalAllocatedBytes() != 0) {
            throw new AssertionError("Memory accounting leak! Remaining bytes: "
                    + ByteArena.get().totalAllocatedBytes());
        }
        // Check 2: Verify raw memory array contains no residual 1s
        verifyArenaCompletelyZeroed();
    }

    private static void printArenaMemoryUsage() {
        long used = ByteArena.get().totalAllocatedBytes();
        long total = ByteArena.SIZE;
        long remaining = total - used;
        String memoryUsed = JgenUtils.formatBytes(used);
        String memoryTotal = JgenUtils.formatBytes(total);
        String memoryRemaining = JgenUtils.formatBytes(remaining);
        System.out.println("\n====== Arena Memory ======");
        System.out.println("Used: \t" + memoryUsed);
        System.out.println("Total: \t" + memoryTotal);
        System.out.println("Left: \t" + memoryRemaining);
        System.out.println("==========================");
    }




}
