package com.resourceflow.io;

import com.resourceflow.exception.ScenarioFileException;
import com.resourceflow.model.Scenario;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Saves and loads complete What-If scenarios (resources, requests, strategy, constraints)
 * as binary .rflow files using Java Object Serialization.
 *
 * File layout: a UTF header string, a format version int, then the serialized Scenario object.
 * Stream chain (byte streams): Files.newOutputStream -> BufferedOutputStream -> ObjectOutputStream.
 */
public final class ScenarioFileStore {

    public static final String FILE_EXTENSION = ".rflow";
    private static final String HEADER = "RESOURCEFLOW-SCENARIO";
    private static final int FORMAT_VERSION = 1;

    private ScenarioFileStore() {
        // static utility class: no instances
    }

    public static void save(Scenario scenario, Path target) throws ScenarioFileException {
        if (scenario == null) {
            throw new IllegalArgumentException("Scenario must not be null");
        }
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(target)))) {
            out.writeUTF(HEADER);
            out.writeInt(FORMAT_VERSION);
            out.writeObject(scenario);
        } catch (IOException e) {
            throw new ScenarioFileException("Could not save scenario to " + target.getFileName(), e);
        }
    }

    /**
     * Reads a scenario back. Shows nested try blocks and a multi-catch clause; every
     * low-level failure is wrapped (chained) into a ScenarioFileException.
     */
    public static Scenario load(Path source) throws ScenarioFileException {
        try {
            try (ObjectInputStream in = new ObjectInputStream(
                    new BufferedInputStream(Files.newInputStream(source)))) {

                String header = in.readUTF();
                if (!HEADER.equals(header)) {
                    throw new ScenarioFileException("Not a ResourceFlow scenario file: " + source.getFileName());
                }
                int version = in.readInt();
                if (version != FORMAT_VERSION) {
                    throw new ScenarioFileException("Unsupported scenario file version " + version);
                }
                return (Scenario) in.readObject();
            }
        } catch (NoSuchFileException e) {
            throw new ScenarioFileException("File not found: " + source, e);
        } catch (ClassNotFoundException | ClassCastException | InvalidClassException e) {
            throw new ScenarioFileException("File was written by an incompatible version of ResourceFlow", e);
        } catch (EOFException | StreamCorruptedException e) {
            throw new ScenarioFileException("Scenario file is damaged or incomplete", e);
        } catch (IOException e) {
            throw new ScenarioFileException("Could not read scenario file: " + e.getMessage(), e);
        }
    }
}
