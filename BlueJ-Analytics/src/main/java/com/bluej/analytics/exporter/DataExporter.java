package com.bluej.analytics.exporter;

import com.bluej.analytics.observer.TelemetryCollector;
import java.io.IOException;

public abstract class DataExporter {
    // Template method
    public final void export(TelemetryCollector collector, String filename) throws IOException {
        validate(collector);
        String data = formatData(collector);
        saveToFile(data, filename);
        postProcess(filename);
    }

    protected void validate(TelemetryCollector c) {
        if (c == null) throw new IllegalArgumentException("Collector cannot be null");
    }
    protected abstract String formatData(TelemetryCollector collector);
    protected abstract void saveToFile(String data, String filename) throws IOException;
    protected void postProcess(String filename) { /* hook */ }
}