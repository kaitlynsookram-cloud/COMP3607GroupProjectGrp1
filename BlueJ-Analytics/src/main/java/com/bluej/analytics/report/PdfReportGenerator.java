package com.bluej.analytics.report;

import com.bluej.analytics.model.ClassLifecycleEvent;
import com.bluej.analytics.model.CompileEvent;
import com.bluej.analytics.model.ExecutionTelemetryEvent;
import com.bluej.analytics.model.SessionMetadata;
import com.bluej.analytics.model.TelemetryEvent;
import com.bluej.analytics.observer.TelemetryCollector;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

            line("Active time: " +
                    formatDuration(agg.activeTimeMs()));

            line("Idle time: " +
                    formatDuration(agg.idleTimeMs()));

            long editEvents = collector.getEvents()
                    .stream()
                    .filter(e -> "EDITOR".equals(e.getEventType()))
                    .count();

            line("Recorded editing events: " + editEvents);

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
                        event instanceof ExecutionTelemetryEvent) {
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

    // Draw the compilation error trend without overlapping
    // the next section of the report.
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

        // Reserve enough space for the entire chart.
        float chartHeight = 220;
        checkSpace(chartHeight);

        int displayed = Math.min(values.size(), 12);

        float chartWidth = RIGHT - LEFT;
        float slotWidth = chartWidth / displayed;
        float barWidth = slotWidth * 0.55f;

        // The zero line is in the middle of the graph.
        float zeroY = y - 105;

        // Maximum height above or below the zero line.
        float maxBarHeight = 75;

        // Draw the horizontal zero line.
        content.setStrokingColor(80, 80, 80);
        content.setLineWidth(1);

        content.moveTo(LEFT, zeroY);
        content.lineTo(RIGHT, zeroY);
        content.stroke();

        // Label the zero line.
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

            // Reset text colour.
            content.setNonStrokingColor(0, 0, 0);

            // Display the change in errors above or below each bar.
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

            // Label each compilation interval.
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

        // Move below the entire chart before continuing.
        y -= chartHeight;
    }

    private void drawChartText(String text, float x,
                               float textY, int size)
            throws IOException {

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
