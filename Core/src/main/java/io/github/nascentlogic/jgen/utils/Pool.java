package io.github.nascentlogic.jgen.utils;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;

public final class Pool<I> {

    private static final int MIN_CAPACITY = 16;
    public interface Poolable {
        default void poolableOnFree() { /* reset */ }
        default void poolableOnDiscard() { /* free */ }
    }

    private Object[] items;
    private int size;
    private int peak;
    private final int max;
    private final Supplier<I> supplier;

    private Pool(int initialCap, int maxCap, Supplier<I> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.items = new Object[Math.max(initialCap, MIN_CAPACITY)];
        this.max = Math.max(items.length, maxCap);
    }

    /** {@code Pool<Bullet> bulletPool = Pool.of(64, 1024, Bullet::new);} */
    public static <T> Pool<T> of(int initialCap, int maxCap, Supplier<T> supplier) {
        return new Pool<>(initialCap, maxCap, supplier);
    } public static <T> Pool<T> of(int initialCap, Supplier<T> supplier) {
        return new Pool<>(initialCap, Integer.MAX_VALUE, supplier);
    } public static <T> Pool<T> of(Supplier<T> supplier) {
        return new Pool<>(MIN_CAPACITY, Integer.MAX_VALUE, supplier);
    }

    public int size() { return size; }
    public int peak() { return peak; }
    public int maxCapacity() { return max; }
    public int capacity() { return items.length; }


    @SuppressWarnings("unchecked")
    public I obtain() {
        if (size == 0) return supplier.get();
        int index = --size;
        I object = (I) items[index];
        items[index] = null;
        return object;
    }

    public void free(I item) {
        if (item == null) return;
        if (size < max) {
            if (item instanceof Poolable poolable)
                poolable.poolableOnFree();
            ensureCapacity(size + 1);
            items[size++] = item;
            if (size > peak) peak = size;
        } else if (item instanceof Poolable poolable) {
            poolable.poolableOnDiscard();
        }
    }

    public void preFill(int count) {
        if (count <= 0) return;
        int targetSize = Math.min(max, size + count);
        ensureCapacity(targetSize);
        while (size < targetSize) {
            items[size++] = supplier.get();
        } if (size > peak) peak = size;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            if (items[i] instanceof Poolable poolable) {
                poolable.poolableOnDiscard();
            } items[i] = null;
        } size = 0;
    }

    private void ensureCapacity(int newCap) {
        if (newCap > items.length) {
            int grownCap = Math.max(items.length * 2, newCap);
            int targetCap = Math.min(max, grownCap);
            items = Arrays.copyOf(items, targetCap);
        }
    }
}