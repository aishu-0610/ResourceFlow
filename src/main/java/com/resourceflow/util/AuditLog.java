package com.resourceflow.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Application audit trail written to a text file by a background thread.
 *
 * Any thread (UI, workers) calls AuditLog.record(...), which only drops the line into an
 * EventBuffer and returns immediately. A single low-priority consumer thread takes lines
 * out of the buffer and writes them to disk, so slow file I/O never blocks the UI.
 *
 * Until start() is called, record() does nothing; tests can therefore use services
 * without creating log files.
 */
public final class AuditLog {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String STOP_SIGNAL = "\u0000STOP"; // "poison pill" that tells the consumer to finish

    private static volatile AuditLog instance;

    private final Path file;
    private final EventBuffer<String> buffer = new EventBuffer<>(256);
    private final Thread writerThread;

    private AuditLog(Path file) {
        this.file = file;
        this.writerThread = new Thread(this::writeLoop, "AuditLog-Writer");
        this.writerThread.setDaemon(true);                 // never keeps the JVM alive on exit
        this.writerThread.setPriority(Thread.MIN_PRIORITY); // background disk work
        this.writerThread.start();
    }

    /** Starts the background writer. Calling it again replaces the previous log. */
    public static synchronized void start(Path file) {
        stop();
        instance = new AuditLog(file);
    }

    /** Flushes everything queued so far, then stops the writer thread. */
    public static synchronized void stop() {
        AuditLog current = instance;
        if (current == null) {
            return;
        }
        instance = null;
        try {
            current.buffer.put(STOP_SIGNAL);
            current.writerThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void record(String message) {
        AuditLog current = instance;
        if (current == null) {
            return;
        }
        String line = String.format("[%s] [%s] %s", LocalDateTime.now().format(TIME),
                Thread.currentThread().getName(), message);
        try {
            current.buffer.put(line);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Overload with printf-style formatting, e.g. record("Ran %s in %d ms", name, ms). */
    public static void record(String format, Object... args) {
        record(String.format(Locale.US, format, args));
    }

    public static Path currentFile() {
        AuditLog current = instance;
        return (current != null) ? current.file : null;
    }

    /**
     * Returns the last {@code maxLines} lines of the log file, read with NIO (Files.readAllLines).
     */
    public static List<String> tail(int maxLines) throws IOException {
        Path path = currentFile();
        if (path == null || !Files.exists(path)) {
            return new ArrayList<>();
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        return new ArrayList<>(lines.subList(Math.max(0, lines.size() - maxLines), lines.size()));
    }

    /** Consumer loop run by writerThread: take a line, append it to the file, repeat. */
    private void writeLoop() {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());

            // try-with-resources closes the writer automatically, even if an exception occurs
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile(), StandardCharsets.UTF_8, true))) {
                String line;
                while (!STOP_SIGNAL.equals(line = buffer.take())) {
                    writer.write(line);
                    writer.newLine();
                    writer.flush();
                }
            }
        } catch (IOException e) {
            System.err.println("Audit log disabled: " + e.getMessage());
            discardUntilStop(); // keep emptying the buffer so producers never block on a dead writer
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void discardUntilStop() {
        try {
            while (!STOP_SIGNAL.equals(buffer.take())) {
                // drop the line
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
