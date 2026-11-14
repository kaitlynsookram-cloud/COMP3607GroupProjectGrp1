package com.bluej.analytics.analysis;

import com.bluej.analytics.model.CompileEvent;
import com.bluej.analytics.model.EditorEvent;
import com.bluej.analytics.model.ErrorRecord;
import com.bluej.analytics.model.TelemetryEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class TelemetryAnalyzer {

    private final List<TelemetryEvent> events;

    public TelemetryAnalyzer(List<TelemetryEvent> events) {
        this.events = new ArrayList<>(events);
    }

    //to estime active editing time
    public long totalActiveTimeMs() {

        List<Long> timestamps = new ArrayList<>();

        for (TelemetryEvent event : events) {
            if (event instanceof EditorEvent) {
                timestamps.add(event.getTimestamp());
            }
        }

        if (timestamps.isEmpty()) {
            return 0;
        }

        timestamps.sort(Long::compareTo);

        //to estimate 1 sec for each recorded edit
        long activeTime = timestamps.size() * 1000L;

        //for short gaps in editing
        for (int i = 1; i < timestamps.size(); i++) {

            long gap = timestamps.get(i)
                    - timestamps.get(i - 1);

            if (gap > 0 && gap <= 5000) {
                activeTime += gap;
            }
        }

        return activeTime;
    }

    //total idle time
    public long totalIdleTimeMs() {

        long total = 0;

        for (TelemetryEvent event : events) {

            if (event instanceof EditorEvent) {

                EditorEvent editorEvent =
                        (EditorEvent) event;

                total += editorEvent.getIdleTimeMs();
            }
        }

        return total;
    }

    //avg time between editing
    public double averageEditIntervalSeconds() {

        List<Long> timestamps = new ArrayList<>();

        for (TelemetryEvent event : events) {

            if (event instanceof EditorEvent) {
                timestamps.add(event.getTimestamp());
            }
        }

        if (timestamps.size() < 2) {
            return 0;
        }

        timestamps.sort(Long::compareTo);

        long totalGap = 0;

        for (int i = 1; i < timestamps.size(); i++) {

            long gap = timestamps.get(i)
                    - timestamps.get(i - 1);

            if (gap > 0) {
                totalGap += gap;
            }
        }

        return totalGap / (1000.0 * (timestamps.size() - 1));
    }

    //for time between compilation
    public List<Long> timeBetweenCompiles() {

        List<Long> timestamps = events.stream()
                .filter(e -> e instanceof CompileEvent)
                .map(TelemetryEvent::getTimestamp)
                .sorted()
                .collect(Collectors.toList());

        List<Long> intervals = new ArrayList<>();

        for (int i = 1; i < timestamps.size(); i++) {

            intervals.add(
                    timestamps.get(i) -
                            timestamps.get(i - 1)
            );
        }

        return intervals;
    }

    //how often compilation error occurs
    public Map<String, Integer> errorFrequency() {

        Map<String, Integer> frequency =
                new TreeMap<>();

        for (TelemetryEvent event : events) {

            if (event instanceof CompileEvent) {

                CompileEvent compileEvent =
                        (CompileEvent) event;

                for (ErrorRecord error :
                        compileEvent.getErrors()) {

                    String code = error.getErrorCode();

                    frequency.put(
                            code,
                            frequency.getOrDefault(code, 0) + 1
                    );
                }
            }
        }

        return frequency;
    }

    //compile success rate
    public double compileSuccessRate() {

        int total = 0;
        int successful = 0;

        for (TelemetryEvent event : events) {

            if (event instanceof CompileEvent) {

                total++;

                CompileEvent compileEvent =
                        (CompileEvent) event;

                if (compileEvent.isSuccessful()) {
                    successful++;
                }
            }
        }

        if (total == 0) {
            return 0;
        }

        return (double) successful / total;
    }

    //compile attempts
    public int totalCompilations() {

        int count = 0;

        for (TelemetryEvent event : events) {

            if (event instanceof CompileEvent) {
                count++;
            }
        }

        return count;
    }

    //compiled errors count
    public int totalErrors() {

        int total = 0;

        for (TelemetryEvent event : events) {

            if (event instanceof CompileEvent) {

                CompileEvent compileEvent =
                        (CompileEvent) event;

                total += compileEvent.getErrorCount();
            }
        }

        return total;
    }

    //changes in error during compilation
    public List<Integer> errorDifferenceSeries() {

        List<CompileEvent> compilations = events.stream()
                .filter(e -> e instanceof CompileEvent)
                .map(e -> (CompileEvent) e)
                .sorted((a, b) ->
                        Long.compare(
                                a.getTimestamp(),
                                b.getTimestamp()
                        )
                )
                .collect(Collectors.toList());

        List<Integer> differences = new ArrayList<>();

        for (int i = 1; i < compilations.size(); i++) {

            int previous =
                    compilations.get(i - 1).getErrorCount();

            int current =
                    compilations.get(i).getErrorCount();

            differences.add(current - previous);
        }

        return differences;
    }
}
