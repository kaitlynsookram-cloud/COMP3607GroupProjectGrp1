package com.bluej.analytics.report;

import com.bluej.analytics.observer.TelemetryCollector;
import java.io.IOException;

public interface ReportStrategy {
    void generate(TelemetryCollector collector, String outputPath) throws IOException;
}