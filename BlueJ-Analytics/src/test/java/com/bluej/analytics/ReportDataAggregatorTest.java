package com.bluej.analytics;

import com.bluej.analytics.model.*;
import com.bluej.analytics.report.ReportDataAggregator;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReportDataAggregatorTest {

    @Test
    void successRateCalculatedCorrectly() {
        List<TelemetryEvent> events = List.of(
                new CompileEvent(1000, 1, false, List.of(), 5),
                new CompileEvent(2000, 0, true, List.of(), 5),
                new CompileEvent(3000, 0, true, List.of(), 5));
        ReportDataAggregator agg = new ReportDataAggregator(events);
        assertEquals(2.0 / 3.0, agg.successRate(), 0.001);
    }

    @Test
    void errorFrequencyAggregatesCodes() {
        List<TelemetryEvent> events = List.of(
                new CompileEvent(1, 2, false, List.of(
                        new ErrorRecord("E1", "m", 1),
                        new ErrorRecord("E1", "m", 2)), 5),
                new CompileEvent(2, 1, false, List.of(
                        new ErrorRecord("E2", "m", 1)), 5));
        ReportDataAggregator agg = new ReportDataAggregator(events);
        assertEquals(2, agg.errorFrequency().get("E1"));
        assertEquals(1, agg.errorFrequency().get("E2"));
    }

    @Test
    void compileIntervalsComputed() {
        List<TelemetryEvent> events = List.of(
                new CompileEvent(1000, 0, true, List.of(), 5),
                new CompileEvent(3000, 0, true, List.of(), 5),
                new CompileEvent(6000, 0, true, List.of(), 5));
        ReportDataAggregator agg = new ReportDataAggregator(events);
        assertEquals(List.of(2000L, 3000L), agg.compileIntervals());
    }
}