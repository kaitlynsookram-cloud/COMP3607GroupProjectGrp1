
package com.bluej.analytics.report;

import java.util.List;
import java.util.Map;

import com.bluej.analytics.analysis.TelemetryAnalyzer;
import com.bluej.analytics.model.TelemetryEvent;
/**
 *Combines results from telemetry analysis to use in report generation. 
 */

public class ReportDataAggregator {

    private final TelemetryAnalyzer analyzer;
     /**
     * Makes a report data aggregator using the telemetry events
     * @param events the telemetry events to analyze
     */

    public ReportDataAggregator(List<TelemetryEvent> events) {
        this.analyzer = new TelemetryAnalyzer(events);
    }

    public long activeTimeMs() {
        return analyzer.totalActiveTimeMs();
    }

    public long idleTimeMs() {
        return analyzer.totalIdleTimeMs();
    }

    public double averageEditIntervalSeconds() {
        return analyzer.averageEditIntervalSeconds();
    }

    public double successRate() {
        return analyzer.compileSuccessRate();
    }

    public Map<String, Integer> errorFrequency() {
        return analyzer.errorFrequency();
    }

    public List<Long> compileIntervals() {
        return analyzer.timeBetweenCompiles();
    }

    public List<Integer> errorDiffSeries() {
        return analyzer.errorDifferenceSeries();
    }
}
