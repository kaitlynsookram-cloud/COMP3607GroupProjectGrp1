package com.bluej.analytics;

import com.bluej.analytics.exporter.JsonExporter;
import com.bluej.analytics.observer.TelemetryCollector;
import com.bluej.analytics.model.CompileEvent;
import com.bluej.analytics.model.SessionMetadata;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JsonExporterTest {

    @TempDir
    Path tempDir;

    private final TelemetryCollector collector =
            TelemetryCollector.INSTANCE;

    @AfterEach
    void cleanup() {
        collector.resetSession();
    }

    @Test
    void jsonFileIsCreated() throws Exception {
        collector.resetSession();

        Path file = tempDir.resolve("session.json");

        new JsonExporter().export(
                collector, file.toString());

        assertTrue(Files.exists(file));
        assertTrue(Files.size(file) > 0);
    }

    @Test
    void jsonContainsStudentMetadata() throws Exception {
        collector.resetSession();

        SessionMetadata metadata =
                new SessionMetadata("STUDENT001", 1000);

        metadata.setSessionEndMs(5000);
        collector.setMetadata(metadata);

        Path file = tempDir.resolve("metadata.json");
        new JsonExporter().export(
                collector, file.toString());

        JsonObject json = JsonParser.parseString(
                Files.readString(file)).getAsJsonObject();

        JsonObject savedMetadata =
                json.getAsJsonObject("metadata");

        assertEquals("STUDENT001",
                savedMetadata.get("studentId").getAsString());

        assertEquals(4000,
                savedMetadata.get("sessionEndMs").getAsLong()
                        - savedMetadata.get("sessionStartMs").getAsLong());
    }

    @Test
    void compilationEventIsExported() throws Exception {
        collector.resetSession();

        collector.onEvent(new CompileEvent(
                1000, 2, false, List.of(), 50));

        Path file = tempDir.resolve("events.json");
        new JsonExporter().export(
                collector, file.toString());

        JsonObject json = JsonParser.parseString(
                Files.readString(file)).getAsJsonObject();

        assertEquals(1,
                json.getAsJsonArray("events").size());

        JsonObject event = json.getAsJsonArray("events")
                .get(0).getAsJsonObject();

        assertEquals(2,
                event.get("errorCount").getAsInt());

        assertFalse(event.get("successful").getAsBoolean());
    }

    @Test
    void emptySessionExportsSuccessfully() throws Exception {
        collector.resetSession();

        Path file = tempDir.resolve("empty.json");
        new JsonExporter().export(
                collector, file.toString());

        JsonObject json = JsonParser.parseString(
                Files.readString(file)).getAsJsonObject();

        assertEquals(0,
                json.getAsJsonArray("events").size());
    }

    @Test
    void nullCollectorIsRejected() {
        Path file = tempDir.resolve("invalid.json");

        assertThrows(IllegalArgumentException.class,
                () -> new JsonExporter().export(
                        null, file.toString()));
    }
}
