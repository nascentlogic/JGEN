package io.github.nascentlogic.jgen.utils;

import java.util.*;

/**
 * Performant auto-growing circular queue for integer type primitives.
 * F.Dahl, 6/17/2026
 */
public class IntQueue {

    private static final int INITIAL_SIZE = 16;
    private static final int[] EMPTY_QUEUE = new int[0];
    private int[] queue;
    private int front;
    private int rear;
    private int size;

    /** Creates an empty queue with an initial capacity of 0 (allocates on first enqueue). */
    public IntQueue() { this(0); }
    /** Creates an empty queue with a specified initial capacity. */
    public IntQueue(int initialSize) {
        if (initialSize < 0) throw new IllegalArgumentException("initial size < 0");
        if (initialSize == 0) queue = EMPTY_QUEUE;
        else queue = new int[initialSize];
    }

    /** Enqueues a single integer. Automatically expands capacity if full. */
    public void enqueue(int value) {
        if (size == queue.length) {
            ensureCapacity(Math.max(size * 2, INITIAL_SIZE));
        } queue[rear] = value;
        rear = (rear + 1) % queue.length;
        size++;
    }

    /** Enqueues multiple integers or an entire array. */
    public void enqueue(int... values) {
        Objects.requireNonNull(values, "values array cannot be null");
        if (values.length == 0) return;
        ensureCapacity(size + values.length);
        for (int value : values) {
            enqueue(value);
        }
    }

    /** Dequeues and returns the front integer. Throws exception if empty. */
    public int dequeue() {
        if (size == 0) throw new NoSuchElementException("queue is empty");
        int value = queue[front];
        front = (front + 1) % queue.length;
        size--;
        return value;
    }

    /** Dequeues up to {@code count} times, returning the actual number of elements removed. */
    public int dequeue(int count) {
        int dequeued = 0;
        while (dequeued < count && size > 0) {
            dequeue();
            dequeued++;
        } return dequeued;
    }

    /** Guarantees the underlying array can hold at least {@code cap} elements. */
    public void ensureCapacity(int cap) {
        if (queue == EMPTY_QUEUE) {
            queue = new int[Math.max(cap, INITIAL_SIZE)];
        } else if (cap > queue.length) {
            int[] tmp = queue;
            queue = new int[cap];
            for (int i = 0; i < size; i++) {
                queue[i] = tmp[(front + i) % tmp.length];
            } front = 0;
            rear = size;
        }
    }

    /** Unsafe: exposes the actual underlying circular array. Use with caution. */
    public int[] array() { return queue; }
    /** Returns true if the queue contains no elements. */
    public boolean isEmpty() { return size == 0; }
    /** Returns the number of elements currently stored in the queue. */
    public int size() { return size; }
    /** Returns the current internal read index (front). */
    public int front() { return front; }
    /** Returns the current internal write index (rear). */
    public int rear() { return rear; }
    /** Returns the current maximum allocation limit of the internal buffer. */
    public int capacity() { return queue.length; }

}