package com.resourceflow.util;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * A fixed-size, thread-safe queue for the producer-consumer problem.
 *
 * Producers call put(), the consumer calls take(). Instead of busy-waiting, threads use
 * inter-thread communication:
 *  - put() calls wait() while the buffer is full, take() calls wait() while it is empty;
 *  - after changing the buffer, each calls notifyAll() to wake the waiting side.
 * wait()/notifyAll() must be called while holding the object's lock, which is why both
 * methods are synchronized.
 *
 * @param <T> type of the items stored (a generic class)
 */
public class EventBuffer<T> {

    private final Queue<T> items = new ArrayDeque<>();
    private final int capacity;

    public EventBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive: " + capacity);
        }
        this.capacity = capacity;
    }

    public synchronized void put(T item) throws InterruptedException {
        while (items.size() == capacity) {
            wait(); // buffer full: release the lock and sleep until a consumer takes something
        }
        items.add(item);
        notifyAll(); // wake the consumer waiting in take()
    }

    public synchronized T take() throws InterruptedException {
        while (items.isEmpty()) {
            wait(); // buffer empty: sleep until a producer puts something
        }
        T item = items.poll();
        notifyAll(); // wake any producer waiting in put()
        return item;
    }

    public synchronized int size() {
        return items.size();
    }
}
