package com.bluej.analytics.report;

import java.io.IOException;

import com.bluej.analytics.observer.TelemetryCollector;

public class SummaryReport implements ReportStrategy {
    @Override

    public void generate(TelemetryCollector collector, String outputPath) throws IOException { 
        ReportDataAggregator agg = new ReportDataAggregator(collector.getEvents());
        System.out.println("=== Summary Report ===");
        System.out.printf("Compilations: %d%n", agg.compileIntervals().size() + 1);
        System.out.printf("Success rate: %.2f%%%n", agg.successRate() * 100);
        System.out.printf("Active / Idle: %d / %d ms%n",
                agg.activeTimeMs(), agg.idleTimeMs());
        System.out.println("Errors by code: " + agg.errorFrequency());
    }
}