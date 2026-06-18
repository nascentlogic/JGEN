package io.github.nascentlogic.jgen.io;

import org.tinylog.Level;
import org.tinylog.core.LogEntry;
import org.tinylog.core.LogEntryValue;
import org.tinylog.writers.Writer;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

import static io.github.nascentlogic.jgen.utils.JgenMath.nextPowerOfTwo;

/**
 * F.Dahl, 5/6/2026
 */
public final class JgenlLogWriter implements Writer {

    private static final OptimizedEvictingQueue<Entry> queue = new OptimizedEvictingQueue<>(256);
    /**
     * Collect simplified (GUI-friendly) log entries.
     * Must be called from a single thread (E.g. game main-loop).
     * The logs accumulate when not collected and the queue will discard older entries.
     */
    public static void collect(Consumer<Entry> consumer) { queue.drainAll(consumer); }

    public JgenlLogWriter(Map<String, String> properties) { /* */ }
    @Override public Collection<LogEntryValue> getRequiredLogEntryValues() { return EnumSet.of(LogEntryValue.LEVEL, LogEntryValue.MESSAGE, LogEntryValue.EXCEPTION); }
    @Override public void write(LogEntry logEntry) { queue.add(Entry.from(logEntry)); }
    @Override public void flush() throws Exception { /* */ }
    @Override public void close() throws Exception { /* */ }

    public record Entry(Level level, String message) {
        private static final Entry NULL_ENTRY = new Entry(Level.INFO,"Internal Log Writer: NULL ENTRY");
        public static Entry from(LogEntry logEntry) {
            if (logEntry == null) return NULL_ENTRY;
            String msg = logEntry.getMessage() == null ? "" : logEntry.getMessage();
            Throwable ex = logEntry.getException();
            if (ex != null) {
                String exText = ex.getClass().getSimpleName() +
                        (ex.getMessage() == null ? "" : ": " + ex.getMessage());
                msg = msg.isBlank() ? exText : msg + " | " + exText;
            } return new Entry(logEntry.getLevel(), msg);
        }
    }

    /**
     * High-performance lock-free MPSC (Multi-Producer Single-Consumer) evicting queue.
     * Designed specifically for logging: many threads adding logs, one rendering thread draining.
     * F.Dahl, 5/7/2026
     */
    private static final class OptimizedEvictingQueue<E> {
        private final AtomicLong tail = new AtomicLong(0);
        private final AtomicLong head = new AtomicLong(0);
        private final AtomicReferenceArray<E> buffer;
        private final int capacity;
        private final int mask;

        OptimizedEvictingQueue(int capacity) {
            this.capacity = nextPowerOfTwo(Math.max(16, capacity));
            this.buffer = new AtomicReferenceArray<>(this.capacity);
            this.mask = this.capacity - 1;
        }

        void add(E element) {
            if (element == null) throw new NullPointerException("null elements not allowed");
            long t = tail.getAndIncrement();
            int index = (int) (t & mask);
            // Evict oldest element(s) if full (safe under contention)
            long h = head.get();
            if (t - h >= capacity) {
                while (t - h >= capacity) {
                    if (head.compareAndSet(h, h + 1)) {
                        buffer.lazySet((int) (h & mask), null); // help GC
                        break;
                    } h = head.get(); // CAS failed, retry with updated head
                }
            } buffer.set(index, element); // Release semantics
        }

        /**
         * Must be called from a SINGLE thread (the rendering thread).
         * Prioritizes smooth rendering over waiting for in-flight writes.
         */
        void drainAll(Consumer<E> consumer) {
            if (consumer == null) throw new NullPointerException();
            long h = head.get();
            long t = tail.get();
            while (h < t) {
                int index = (int) (h & mask);
                E item = buffer.getAndSet(index, null);
                if (item != null) consumer.accept(item);
                h++; // We advance anyway. This keeps the renderer smooth.
                // Extremely rare that a log is lost (only if queue wraps before write completes).
            } head.set(h); // Publish new head position
        }

        int size() {
            long s = tail.get() - head.get();
            return (int) Math.clamp(s, 0L, capacity);
        }
    }
}
