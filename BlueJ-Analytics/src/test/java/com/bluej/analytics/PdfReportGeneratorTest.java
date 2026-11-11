
package com.bluej.analytics;

import com.bluej.analytics.model.ClassLifecycleEvent;
import com.bluej.analytics.model.CompileEvent;
import com.bluej.analytics.model.ErrorRecord;
import com.bluej.analytics.model.ExecutionTelemetryEvent;
import com.bluej.analytics.model.SessionMetadata;
import com.bluej.analytics.observer.TelemetryCollector;
import com.bluej.analytics.report.PdfReportGenerator;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PdfReportGeneratorTest {

    @TempDir
    Path tempDir;

    private final TelemetryCollector collector =
            TelemetryCollector.INSTANCE;

    @AfterEach
    void cleanup() {
        collector.resetSession();
    }

    private String generateAndRead(String filename)
            throws Exception {

        Path file = tempDir.resolve(filename);

        new PdfReportGenerator().generate(
                collector, file.toString()
        );

        try (PDDocument document =
                     PDDocument.load(file.toFile())) {

            return new PDFTextStripper().getText(document);
        }
    }

    @Test
    void pdfFileIsCreated() throws Exception {

        collector.resetSession();

        Path file = tempDir.resolve("report.pdf");

        new PdfReportGenerator().generate(
                collector, file.toString()
        );

        assertTrue(Files.exists(file));
        assertTrue(Files.size(file) > 0);
    }

    @Test
    void pdfContainsStudentId() throws Exception {

        collector.resetSession();

        collector.setMetadata(
                new SessionMetadata("STUDENT001", 1000)
        );

        String text = generateAndRead("student.pdf");

        assertTrue(text.contains("STUDENT001"));
        assertTrue(text.contains("Session Report"));
        assertTrue(text.contains("Session Summary"));
    }

    @Test
    void pdfContainsCompilationStatistics() throws Exception {

        collector.resetSession();

        collector.onEvent(new CompileEvent(
                1000, 0, true, List.of(), 50
        ));

        collector.onEvent(new CompileEvent(
                2000, 1, false, List.of(), 60
        ));

        String text = generateAndRead("compilations.pdf");

        assertTrue(text.contains("Compilation attempts: 2"));
        assertTrue(text.contains(
                "Compilation success rate: 50.0%"
        ));
    }

    @Test
    void pdfContainsErrorFrequency() throws Exception {

        collector.resetSession();

        collector.onEvent(new CompileEvent(
                1000,
                1,
                false,
                List.of(
                        new ErrorRecord(
                                "E1", "Syntax error", 5
                        )
                ),
                50
        ));

        String text = generateAndRead("errors.pdf");

        assertTrue(text.contains(
                "Error Frequency Breakdown"
        ));

        assertTrue(text.contains("E1: 1"));
    }

    @Test
    void emptyPdfHasOnePage() throws Exception {

        collector.resetSession();

        Path file = tempDir.resolve("pages.pdf");

        new PdfReportGenerator().generate(
                collector, file.toString()
        );

        try (PDDocument document =
                     PDDocument.load(file.toFile())) {

            assertEquals(1, document.getNumberOfPages());
        }
    }

    @Test
    void emptySessionShowsZeroCompilations() throws Exception {

        collector.resetSession();

        String text = generateAndRead("empty-session.pdf");

        assertTrue(text.contains(
                "Compilation attempts: 0"
        ));
    }

    @Test
    void pdfContainsSessionDuration() throws Exception {

        collector.resetSession();

        SessionMetadata metadata =
                new SessionMetadata("STUDENT001", 1000);

        metadata.setSessionEndMs(61000);

        collector.setMetadata(metadata);

        String text = generateAndRead("duration.pdf");

        assertTrue(text.contains(
                "Session duration: 00:01:00"
        ));
    }

    @Test
    void pdfContainsClassTimeline() throws Exception {

        collector.resetSession();

        collector.onEvent(
                new ClassLifecycleEvent(
                        1000,
                        "HelloWorld",
                        "STATE_CHANGED",
                        true,
                        false,
                        null
                )
        );

        String text = generateAndRead("class-timeline.pdf");

        assertTrue(text.contains(
                "Class and Method Activity Timeline"
        ));

        assertTrue(text.contains("HelloWorld"));
        assertTrue(text.contains("STATE_CHANGED"));
    }

    @Test
    void pdfContainsMethodExecutionTimeline() throws Exception {

        collector.resetSession();

        collector.onEvent(
                new ExecutionTelemetryEvent(
                        2000,
                        "Calculator",
                        "add",
                        "calculator1",
                        "NORMAL_EXIT",
                        new String[]{"5", "10"}
                )
        );

        String text = generateAndRead("method-timeline.pdf");

        assertTrue(text.contains("METHOD RUN"));
        assertTrue(text.contains("Calculator.add"));
        assertTrue(text.contains("NORMAL_EXIT"));
    }

    @Test
    void longTimelineCreatesMultiplePages() throws Exception {

        collector.resetSession();

        for (int i = 0; i < 60; i++) {

            collector.onEvent(
                    new ClassLifecycleEvent(
                            1000L + i * 1000L,
                            "Class" + i,
                            "STATE_CHANGED",
                            true,
                            false,
                            null
                    )
            );
        }

        Path file = tempDir.resolve("long-report.pdf");

        new PdfReportGenerator().generate(
                collector, file.toString()
        );

        try (PDDocument document =
                     PDDocument.load(file.toFile())) {

            assertTrue(document.getNumberOfPages() > 1);

            String text =
                    new PDFTextStripper().getText(document);

            assertTrue(text.contains("Class0"));
            assertTrue(text.contains("Class59"));
        }
    }
}
