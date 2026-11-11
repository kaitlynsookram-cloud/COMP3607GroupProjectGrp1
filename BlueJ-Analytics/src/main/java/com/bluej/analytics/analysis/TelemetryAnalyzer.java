package com.bluej.analytics.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.bluej.analytics.model.CompileEvent;
import com.bluej.analytics.model.EditorEvent;
import com.bluej.analytics.model.ErrorRecord;
import com.bluej.analytics.model.TelemetryEvent;


public class TelemetryAnalyzer {

    private final List<TelemetryEvent> events;

    public TelemetryAnalyzer(List<TelemetryEvent> events) {
        this.events = new ArrayList<>(events);
    }

    /** Total active time = sum of editor keystroke intervals */
    public long totalActiveTimeMs() {
        return events.stream()
                .filter(e -> e instanceof EditorEvent)
                .mapToLong(e -> ((EditorEvent) e).getIdleTimeMs() == 0
                        ? 5000 : 0) // placeholder: keystroke delta
                .sum();
    }

    public long totalIdleTimeMs() {
        return events.stream()
                .filter(e -> e instanceof EditorEvent)
                .mapToLong(e -> ((EditorEvent) e).getIdleTimeMs())
                .sum();
    }

    /** Time between consecutive compilations, milli. */
    public List<Long> timeBetweenCompiles() {
        List<Long> stamps = events.stream()
                .filter(e -> e instanceof CompileEvent)
                .map(TelemetryEvent::getTimestamp)
                .sorted()
                .collect(Collectors.toList());
        List<Long> deltas = new ArrayList<>();
        for (int i = 1; i < stamps.size(); i++) {
            deltas.add(stamps.get(i) - stamps.get(i - 1));
        }
        return deltas;
    }

    /** Frequency of each error code. */
    public Map<String, Integer> errorFrequency() {
        Map<String, Integer> freq = new TreeMap<>();
        for (TelemetryEvent e : events) {
            if (e instanceof CompileEvent) {
                for (ErrorRecord er : ((CompileEvent) e).getErrors()) {
                    freq.merge(er.getErrorCode(), 1, Integer::sum);
                }
            }
        }
        return freq;
    }

    public double compileSuccessRate() {
        long total = events.stream().filter(e -> e instanceof CompileEvent).count();
        if (total == 0) return 0.0;
        long success = events.stream()
                .filter(e -> e instanceof CompileEvent && ((CompileEvent) e).isSuccessful())
                .count();
        return (double) success / total;
    }

    public int totalCompilations() {
        return (int) events.stream().filter(e -> e instanceof CompileEvent).count();
    }

    public int totalErrors() {
        return events.stream()
                .filter(e -> e instanceof CompileEvent)
                .mapToInt(e -> ((CompileEvent) e).getErrorCount())
                .sum();
    }

    /** Error-difference between successive compiles (for chart of progression). */
    public List<Integer> errorDifferenceSeries() {
        List<CompileEvent> compiles = events.stream()
                .filter(e -> e instanceof CompileEvent)
                .map(e -> (CompileEvent) e)
                .collect(Collectors.toList());
        List<Integer> series = new ArrayList<>();
        for (int i = 1; i < compiles.size(); i++) {
            series.add(compiles.get(i).getErrorCount() - compiles.get(i - 1).getErrorCount());
        }
        return series;
    }
}