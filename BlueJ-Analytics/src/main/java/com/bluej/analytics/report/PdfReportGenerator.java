
package com.bluej.analytics.report;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.bluej.analytics.model.ClassLifecycleEvent;
import com.bluej.analytics.model.CompileEvent;
import com.bluej.analytics.model.ExecutionTelemetryEvent;
import com.bluej.analytics.model.MethodDefinitionEvent;
import com.bluej.analytics.model.SessionMetadata;
import com.bluej.analytics.model.TelemetryEvent;
import com.bluej.analytics.observer.TelemetryCollector;
/**
 * Makes a PDF report with a summary of telemetry data.
 * The summary has session information, compilation stats,
 * editor activity, error frequency and class and method activity timelines
 *
 * Made by: Anthony Lakicharan
 */

public class PdfReportGenerator implements ReportStrategy {

    private static final float LEFT = 50;
    private static final float TOP = 750;
    private static final float BOTTOM = 55;
    private static final float RIGHT = 550;

    private PDDocument document;
    private PDPageContentStream content;
    private float y;

    @Override
    public void generate(TelemetryCollector collector, String outputPath)
            throws IOException {

        ReportDataAggregator agg =
                new ReportDataAggregator(collector.getEvents());

        document = new PDDocument();

        try {
            newPage();

            heading("BlueJ Analytics - Session Report", 18);

            SessionMetadata metadata = collector.getMetadata();

            heading("Session Summary", 14);

            line("Student ID: " + metadata.getStudentId());

            line("Session started: " +
                    formatTime(metadata.getSessionStartMs()));

            if (metadata.getSessionEndMs() > 0) {
                line("Session ended: " +
                        formatTime(metadata.getSessionEndMs()));
            } else {
                line("Session status: In progress");
            }

            line("Session duration: " +
                    formatDuration(metadata.getSessionDurationMs()));

            space(10);

            heading("Compilation Statistics", 14);

            long compilationCount = collector.getEvents()
                    .stream()
                    .filter(e -> e instanceof CompileEvent)
                    .count();

            line("Compilation attempts: " + compilationCount);

            line(String.format(
                    "Compilation success rate: %.1f%%",
                    agg.successRate() * 100
            ));

            line("Total compilation errors: " +
                    metadata.getTotalErrors());

            space(10);

            heading("Editor Activity", 14);

            line("Estimated active editing time: " +
                    formatDuration(agg.activeTimeMs()));

            line("Recorded idle time: " +
                    formatDuration(agg.idleTimeMs()));

            line(String.format(
                    "Average time between edits: %.2f seconds",
                    agg.averageEditIntervalSeconds()
            ));

            long editEvents = collector.getEvents()
                    .stream()
                    .filter(e -> "EDITOR".equals(e.getEventType()))
                    .count();

            line("Recorded editing events: " + editEvents);

            space(10);

            heading("Student Feedback and Recommendations", 14);

            writeRecommendations(agg, compilationCount, editEvents);

            space(10);

            heading("Error Frequency Breakdown", 14);

            Map<String, Integer> errors = agg.errorFrequency();

            if (errors.isEmpty()) {
                line("No compilation errors recorded.");
            } else {
                for (Map.Entry<String, Integer> entry
                        : errors.entrySet()) {

                    line(entry.getKey() + ": " +
                            entry.getValue());
                }
            }

            space(15);

            heading("Compilation Error Trend", 14);

            List<Integer> trend = agg.errorDiffSeries();

            if (trend.isEmpty()) {
                line("Not enough compilation data for a chart.");
            } else {
                drawBarChart(trend);
            }

            space(20);

            heading("Class and Method Activity Timeline", 14);

            line("Class changes and method executions in time order:");

            space(8);

            List<TelemetryEvent> timeline = new ArrayList<>();

            for (TelemetryEvent event : collector.getEvents()) {
                if (event instanceof ClassLifecycleEvent ||
                        event instanceof ExecutionTelemetryEvent ||
                        event instanceof MethodDefinitionEvent) {
                    timeline.add(event);
                }
            }

            timeline.sort(
                    Comparator.comparingLong(
                            TelemetryEvent::getTimestamp
                    )
            );

            if (timeline.isEmpty()) {
                line("No class or method activity recorded.");
            }

            for (TelemetryEvent event : timeline) {

                String description = "";

                if (event instanceof ClassLifecycleEvent) {

                    ClassLifecycleEvent classEvent =
                            (ClassLifecycleEvent) event;

                    description = "CLASS: " +
                            classEvent.getClassName() +
                            " - " + classEvent.getAction();

                    if (classEvent.hasError()) {
                        description += " (error)";
                    }

                } else if (event instanceof MethodDefinitionEvent) {

                    MethodDefinitionEvent method =
                            (MethodDefinitionEvent) event;

                    description = "METHOD DEFINITION: " +
                            method.getClassName() +
                            "." + method.getMethodSignature() +
                            " - " + method.getAction();

                } else if (event instanceof ExecutionTelemetryEvent) {

                    ExecutionTelemetryEvent executionEvent =
                            (ExecutionTelemetryEvent) event;

                    description = "METHOD RUN: " +
                            executionEvent.getClassName() +
                            "." + executionEvent.getMethodName() +
                            " - " + executionEvent.getOutcome();
                }

                line(formatTime(event.getTimestamp()));

                line("    " + description);

                space(5);
            }

            closePage();

            document.save(outputPath);

        } finally {
            if (content != null) {
                content.close();
                content = null;
            }

            document.close();
        }
    }

    // Recommendations based on recorded activity.
    private void writeRecommendations(
            ReportDataAggregator agg,
            long compilationCount,
            long editEvents) throws IOException {

        if (compilationCount == 0 && editEvents == 0) {

            line("Not enough activity has been recorded to give useful feedback.");

            line("Recommendation: Write some code and compile it to collect results.");

            return;
        }

        double successPercent = agg.successRate() * 100.0;

        long activeMs = agg.activeTimeMs();
        long idleMs = agg.idleTimeMs();

        // Activity thresholds: 6, 4, and 2 editing events.
        if (editEvents >= 6) {

            line("Activity level: High editing activity (6 or more editing events).");

            line("Recommendation: Keep making small changes and compile regularly");

            line("to catch mistakes before they become difficult to find.");

        } else if (editEvents >= 4) {

            line("Activity level: Medium editing activity (4 to 5 editing events).");

            line("Recommendation: Continue testing after each meaningful code change.");

        } else if (editEvents >= 2) {

            line("Activity level: Low editing activity (2 to 3 editing events).");

            line("Recommendation: Try more coding exercises and test your changes");

            line("to build confidence and produce more evidence of progress.");

        } else {

            line("Activity level: Very low editing activity (0 to 1 editing events).");

            line("Recommendation: Make more code changes and compile your work");

            line("to collect enough activity for useful feedback.");
        }

        // Feedback based on compilation results.
        if (compilationCount == 0) {

            line("No compilation attempts were recorded in this session.");

            line("Recommendation: Compile your code to check for syntax errors.");

        } else if (successPercent < 70.0) {

            line(String.format(
                    "Compilation success rate: %.1f%% (needs attention).",
                    successPercent
            ));

            line("Recommendation: Read each compiler message and fix errors");

            line("one at a time before making more changes.");

        } else if (successPercent < 90.0) {

            line(String.format(
                    "Compilation success rate: %.1f%% (some errors).",
                    successPercent
            ));

            line("Recommendation: Review the failed compilations and check");

            line("punctuation, brackets, and method definitions carefully.");

        } else {

            line(String.format(
                    "Compilation success rate: %.1f%% (mostly successful).",
                    successPercent
            ));

            line("Recommendation: Keep compiling regularly and testing methods");

            line("to maintain this result.");
        }

        // Feedback based on recorded idle time.
        if (idleMs > activeMs && idleMs > 0) {

            line("Recorded idle time is greater than estimated active editing time.");

            line("Recommendation: Review inactive periods when evaluating the");

            line("session. A long-open session may inflate recorded idle time.");
        }

        line("These suggestions are based on this session's recorded metrics.");
    }

    private void newPage() throws IOException {

        if (content != null) {
            content.close();
        }

        PDPage page = new PDPage();
        document.addPage(page);

        content = new PDPageContentStream(document, page);
        y = TOP;
    }

    private void closePage() throws IOException {

        if (content != null) {
            content.close();
            content = null;
        }
    }

    private void checkSpace(float needed) throws IOException {

        if (y - needed < BOTTOM) {
            newPage();
        }
    }

    private void heading(String text, int size)
            throws IOException {

        checkSpace(35);

        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, size);
        content.newLineAtOffset(LEFT, y);
        content.showText(safeText(text));
        content.endText();

        y -= size + 15;
    }

    private void line(String text) throws IOException {

        String cleaned = safeText(text);
        int maxCharacters = 85;

        while (cleaned.length() > maxCharacters) {

            int split = cleaned.lastIndexOf(
                    ' ', maxCharacters
            );

            if (split <= 0) {
                split = maxCharacters;
            }

            writeLine(cleaned.substring(0, split).trim());

            cleaned = cleaned.substring(split).trim();
        }

        writeLine(cleaned);
    }

    private void writeLine(String text) throws IOException {

        checkSpace(20);

        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 10);
        content.newLineAtOffset(LEFT, y);
        content.showText(safeText(text));
        content.endText();

        y -= 16;
    }

    private void space(float amount) throws IOException {

        checkSpace(amount);
        y -= amount;
    }

    private void drawBarChart(List<Integer> values)
            throws IOException {

        int max = values.stream()
                .mapToInt(Math::abs)
                .max()
                .orElse(0);

        if (max == 0) {
            line("No changes in error counts to display.");
            return;
        }

        float chartHeight = 220;
        checkSpace(chartHeight);

        int displayed = Math.min(values.size(), 12);

        float chartWidth = RIGHT - LEFT;
        float slotWidth = chartWidth / displayed;
        float barWidth = slotWidth * 0.55f;

        float zeroY = y - 105;

        float maxBarHeight = 75;

        content.setStrokingColor(80, 80, 80);
        content.setLineWidth(1);

        content.moveTo(LEFT, zeroY);
        content.lineTo(RIGHT, zeroY);
        content.stroke();

        drawChartText("0", LEFT - 12, zeroY - 3, 8);

        for (int i = 0; i < displayed; i++) {

            int value = values.get(i);

            float height =
                    ((float) Math.abs(value) / max)
                            * maxBarHeight;

            float x = LEFT +
                    i * slotWidth +
                    (slotWidth - barWidth) / 2;

            float barY;

            if (value >= 0) {
                barY = zeroY;
                content.setNonStrokingColor(60, 130, 190);
            } else {
                barY = zeroY - height;
                content.setNonStrokingColor(210, 90, 90);
            }

            if (height > 0) {
                content.addRect(
                        x,
                        barY,
                        barWidth,
                        height
                );

                content.fill();
            }

            content.setNonStrokingColor(0, 0, 0);

            float valueY;

            if (value >= 0) {
                valueY = zeroY + height + 6;
            } else {
                valueY = zeroY - height - 12;
            }

            drawChartText(
                    String.valueOf(value),
                    x + 2,
                    valueY,
                    8
            );

            drawChartText(
                    String.valueOf(i + 1),
                    x + 2,
                    zeroY - maxBarHeight - 25,
                    8
            );
        }

        content.setNonStrokingColor(0, 0, 0);

        drawChartText(
                "Compilation interval",
                LEFT + 180,
                zeroY - maxBarHeight - 43,
                9
        );

        drawChartText(
                "Blue: increase in errors    Red: decrease in errors",
                LEFT,
                zeroY + maxBarHeight + 15,
                9
        );

        y -= chartHeight;
    }

    private void drawChartText(
            String text,
            float x,
            float textY,
            int size) throws IOException {

        content.beginText();
        content.setFont(PDType1Font.HELVETICA, size);
        content.newLineAtOffset(x, textY);
        content.showText(safeText(text));
        content.endText();
    }

    private String formatTime(long timestamp) {

        if (timestamp <= 0) {
            return "Unknown";
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss"
                );

        return formatter.format(new Date(timestamp));
    }

    private String formatDuration(long milliseconds) {

        long seconds = Math.max(0, milliseconds) / 1000;

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        return String.format(
                "%02d:%02d:%02d",
                hours,
                minutes,
                remainingSeconds
        );
    }

    private String safeText(String text) {

        if (text == null) {
            return "Unknown";
        }

        return text.replaceAll(
                "[^\\x20-\\x7E]",
                "?"
        );
    }
}
