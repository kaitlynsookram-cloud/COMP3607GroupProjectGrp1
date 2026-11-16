package com.bluej.analytics.exporter;

import java.io.IOException;

import com.bluej.analytics.observer.TelemetryCollector;
/**
 * Abstract base for exporting telemetry data
 * Standard export method using the Template Method
 */

public abstract class DataExporter {
     /**
     * Validates the collector,formats the data,saves it to a file , does post-processing.
     * @param collector , telemetry collector holding the data
     * @param filename , name of file to export
     * @throws IOException , if an error happens while saving the file
     */
    // Template method
    public final void export(TelemetryCollector collector, String filename) throws IOException {
        validate(collector);
        String data = formatData(collector);
        saveToFile(data, filename);
        postProcess(filename);
    }
     /**
     * Validates that telemetry !=null
     * @param c ,telemetry collector to validate
     */


    protected void validate(TelemetryCollector c) {
        if (c == null) throw new IllegalArgumentException("Collector cannot be null");
    }
    /**
     * Formats telemetry data 
     * @param collector ,telemetry collector containing the data
     * @return formatted telemetry data in string form
     */
    protected abstract String formatData(TelemetryCollector collector);
    /**
     * Saves formatted telemetry data to the file
     * @param data ,formatted telemetry data
     * @param filename ,destination filename
     * @throws IOException ,if an error happens while saving the file
     */
    protected abstract void saveToFile(String data, String filename) throws IOException;
    /**
     * after saving the file , performs post processing
     * @param filename ,name of exported file
     */
    protected void postProcess(String filename) { /* hook */ }
    //hook method for subclasses to override if needed
}