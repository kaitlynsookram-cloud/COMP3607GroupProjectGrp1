package com.bluej.analytics.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.bluej.analytics.observer.TelemetryCollector;
import com.google.gson.GsonBuilder;
/**
 * Exports telemetry data into formatted JSON file
 * Uses Gson to serialize collector metadata 
 * 
 * Made by: Anthony Lakicharan
 */

public class JsonExporter extends DataExporter {

    @Override
    protected String formatData(TelemetryCollector collector) {
        return new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create()
                .toJson(new ExportPayload(collector.getMetadata(), collector.getEvents()));
    }

    @Override
    protected void saveToFile(String data, String filename) throws IOException {
        Path p = Paths.get(filename);
        Files.writeString(p, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
     /**
     * Telemetry metadata and events grouped for JSON serialization
     */
    
    private static class ExportPayload {
        final Object metadata;
        final Object events;
        ExportPayload(Object m, Object e) { this.metadata = m; this.events = e; }
    }
}