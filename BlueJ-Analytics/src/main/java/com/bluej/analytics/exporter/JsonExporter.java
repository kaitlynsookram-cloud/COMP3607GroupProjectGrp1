package com.bluej.analytics.exporter;

import com.bluej.analytics.observer.TelemetryCollector;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.*;

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

    
    private static class ExportPayload {
        final Object metadata;
        final Object events;
        ExportPayload(Object m, Object e) { this.metadata = m; this.events = e; }
    }
}