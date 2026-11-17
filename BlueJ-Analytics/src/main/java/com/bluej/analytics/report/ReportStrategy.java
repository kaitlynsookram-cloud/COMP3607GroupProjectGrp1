package com.bluej.analytics.report;

import java.io.IOException;

import com.bluej.analytics.observer.TelemetryCollector;
/**
 * Defines a strategy for generating reports based on telemetry data.
 * 
 * Made by: Kaydance Hamilton
 */
public interface ReportStrategy {
    void generate(TelemetryCollector collector, String outputPath) throws IOException;
}