package com.bluej.analytics.report;

import com.bluej.analytics.observer.TelemetryCollector;
import java.io.IOException;
/**
 * Defines a strategy for generating reports based on telemetry data.
 */
public interface ReportStrategy {
    void generate(TelemetryCollector collector, String outputPath) throws IOException;
}