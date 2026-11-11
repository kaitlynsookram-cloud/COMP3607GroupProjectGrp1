package com.bluej.analytics;

import com.bluej.analytics.analysis.TelemetryAnalyzer;
import com.bluej.analytics.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TelemetryAnalyzerTest {

    @Test
    void errorDifferenceSeriesIsCorrect() {
        List<TelemetryEvent> events = List.of(
                new CompileEvent(1, 5, false, List.of(), 1),
                new CompileEvent(2, 3, false, List.of(), 1),
                new CompileEvent(3, 0, true, List.of(), 1));
        List<Integer> diff = new TelemetryAnalyzer(events).errorDifferenceSeries();
        assertEquals(List.of(-2, -3), diff);
    }

    @Test
    void idleTimeSumsEditorEvents() {
        List<TelemetryEvent> events = List.of(
                new EditorEvent(1, "C", 100, 5),
                new EditorEvent(2, "C", 200, 5));
        assertEquals(300, new TelemetryAnalyzer(events).totalIdleTimeMs());
    }
}