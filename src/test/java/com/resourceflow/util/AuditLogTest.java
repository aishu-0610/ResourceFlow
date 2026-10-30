package com.resourceflow.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        AuditLog.stop();
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Producer-consumer: a tiny buffer still passes every item through, in order")
    void bufferHandsOverEveryItem() throws InterruptedException {
        EventBuffer<Integer> buffer = new EventBuffer<>(2); // forces the producer to wait() often
        List<Integer> received = new ArrayList<>();

        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 1000; i++) buffer.put(i); // int autoboxed to Integer
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 1000; i++) received.add(buffer.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        assertEquals(1000, received.size());
        for (int i = 0; i < 1000; i++) {
            assertEquals(i, received.get(i)); // Integer auto-unboxed for comparison
        }
    }

    @Test
    void bufferRejectsBadCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new EventBuffer<String>(0));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Lines recorded from several threads all reach the file")
    void recordsFromManyThreads() throws Exception {
        Path logFile = tempDir.resolve("logs/audit.log");
        AuditLog.start(logFile);

        List<Thread> writers = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            int id = t;
            Thread writer = new Thread(() -> {
                for (int i = 0; i < 100; i++) AuditLog.record("thread %d line %d", id, i);
            }, "writer-" + t);
            writers.add(writer);
            writer.start();
        }
        for (Thread writer : writers) writer.join();
        AuditLog.stop(); // flushes the queue before returning

        List<String> lines = Files.readAllLines(logFile);
        assertEquals(400, lines.size());
        assertTrue(lines.getFirst().contains("[writer-"), "each line names the thread that recorded it");
    }

    @Test
    @DisplayName("Recording before start() is silently ignored")
    void recordWithoutStartDoesNothing() {
        assertDoesNotThrow(() -> AuditLog.record("ignored"));
        assertNull(AuditLog.currentFile());
    }
}
