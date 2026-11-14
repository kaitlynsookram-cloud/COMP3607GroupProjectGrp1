package com.bluej.analytics;

import bluej.extensions2.event.CompileEvent;

import com.bluej.analytics.model.TelemetryEvent;
import com.bluej.analytics.observer.TelemetryCollector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class BlueJAnalyticsExtensionTest {

    private BlueJAnalyticsExtension extension;

    @BeforeEach
    void setUp() {
        TelemetryCollector.INSTANCE.resetSession();
        extension = new BlueJAnalyticsExtension();
    }

    private CompileEvent createEvent(
            CompileEvent.EventType type) {

        return new CompileEvent(
                type,
                new File[]{new File("Test.java")}
        );
    }

    @Test
    void extensionHasCorrectName() {
        assertEquals(
                "BlueJ Analytics",
                extension.getName()
        );
    }

    @Test
    void extensionHasCorrectVersion() {
        assertEquals(
                "1.0",
                extension.getVersion()
        );
    }

    @Test
    void extensionIsCompatible() {
        assertTrue(extension.isCompatible());
    }

    @Test
    void compilationStartDoesNotSaveEvent() {

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_START_EVENT
                )
        );

        assertTrue(
                TelemetryCollector.INSTANCE
                        .getEvents()
                        .isEmpty()
        );
    }

    @Test
    void successfulCompilationIsRecorded() {

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_START_EVENT
                )
        );

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_DONE_EVENT
                )
        );

        List<TelemetryEvent> events =
                TelemetryCollector.INSTANCE.getEvents();

        assertEquals(1, events.size());

        com.bluej.analytics.model.CompileEvent saved =
                (com.bluej.analytics.model.CompileEvent)
                        events.get(0);

        assertEquals(
                "COMPILE",
                saved.getEventType()
        );
    }

    @Test
    void failedCompilationIsRecorded() {

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_START_EVENT
                )
        );

        CompileEvent error =
                createEvent(
                        CompileEvent.EventType.COMPILE_ERROR_EVENT
                );

        error.setErrorMessage("Missing semicolon");
        error.setErrorLineNumber(5);

        extension.eventOccurred(error);

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_FAILED_EVENT
                )
        );

        List<TelemetryEvent> events =
                TelemetryCollector.INSTANCE.getEvents();

        assertEquals(1, events.size());

        assertInstanceOf(
                com.bluej.analytics.model.CompileEvent.class,
                events.get(0)
        );
    }

    @Test
    void duplicateCompletionDoesNotCreateAnotherRecord() {

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_START_EVENT
                )
        );

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_DONE_EVENT
                )
        );

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_DONE_EVENT
                )
        );

        assertEquals(
                1,
                TelemetryCollector.INSTANCE
                        .getEvents()
                        .size()
        );
    }

    @Test
    void completionWithoutStartIsIgnored() {

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_DONE_EVENT
                )
        );

        assertTrue(
                TelemetryCollector.INSTANCE
                        .getEvents()
                        .isEmpty()
        );
    }

    @Test
    void multipleCompilationsAreRecorded() {

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_START_EVENT
                )
        );

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_DONE_EVENT
                )
        );

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_START_EVENT
                )
        );

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_FAILED_EVENT
                )
        );

        assertEquals(
                2,
                TelemetryCollector.INSTANCE
                        .getEvents()
                        .size()
        );
    }

    @Test
    void compilationWarningDoesNotCreateRecord() {

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_START_EVENT
                )
        );

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_WARNING_EVENT
                )
        );

        assertTrue(
                TelemetryCollector.INSTANCE
                        .getEvents()
                        .isEmpty()
        );

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_DONE_EVENT
                )
        );

        assertEquals(
                1,
                TelemetryCollector.INSTANCE
                        .getEvents()
                        .size()
        );
    }

    @Test
    void multipleCompilationErrorsAreRecorded() {

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_START_EVENT
                )
        );

        CompileEvent firstError =
                createEvent(
                        CompileEvent.EventType.COMPILE_ERROR_EVENT
                );

        firstError.setErrorMessage("Missing semicolon");
        firstError.setErrorLineNumber(3);

        extension.eventOccurred(firstError);

        CompileEvent secondError =
                createEvent(
                        CompileEvent.EventType.COMPILE_ERROR_EVENT
                );

        secondError.setErrorMessage("Unknown variable");
        secondError.setErrorLineNumber(8);

        extension.eventOccurred(secondError);

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_FAILED_EVENT
                )
        );

        assertEquals(
                1,
                TelemetryCollector.INSTANCE
                        .getEvents()
                        .size()
        );

        com.bluej.analytics.model.CompileEvent saved =
                (com.bluej.analytics.model.CompileEvent)
                        TelemetryCollector.INSTANCE
                                .getEvents()
                                .get(0);

        assertNotNull(saved);
    }

    @Test
    void newCompilationClearsPreviousErrors() {

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_START_EVENT
                )
        );

        CompileEvent error =
                createEvent(
                        CompileEvent.EventType.COMPILE_ERROR_EVENT
                );

        error.setErrorMessage("Syntax error");
        error.setErrorLineNumber(4);

        extension.eventOccurred(error);

        // Start a new compilation before the first one finishes.
        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_START_EVENT
                )
        );

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_DONE_EVENT
                )
        );

        assertEquals(
                1,
                TelemetryCollector.INSTANCE
                        .getEvents()
                        .size()
        );
    }

    @Test
    void terminationExportsSessionFiles() throws Exception {

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_START_EVENT
                )
        );

        extension.eventOccurred(
                createEvent(
                        CompileEvent.EventType.COMPILE_DONE_EVENT
                )
        );

        extension.terminate();

        Path jsonPath = Path.of(
                System.getProperty("user.home"),
                "bluej",
                "analytics-session.json"
        );

        Path pdfPath = Path.of(
                System.getProperty("user.home"),
                "bluej",
                "analytics-report.pdf"
        );

        assertTrue(Files.exists(jsonPath));
        assertTrue(Files.exists(pdfPath));

        assertTrue(Files.size(jsonPath) > 0);
        assertTrue(Files.size(pdfPath) > 0);
    }

    @Test
    void editorCheckTimerIsInitiallyNull() throws Exception {

        java.lang.reflect.Field field =
                BlueJAnalyticsExtension.class
                        .getDeclaredField("editorCheckTimer");

        field.setAccessible(true);

        assertNull(field.get(extension));
    }

    @Test
    void editorTrackingStartsWithNoKnownClasses()
            throws Exception {

        java.lang.reflect.Field field =
                BlueJAnalyticsExtension.class
                        .getDeclaredField("knownClasses");

        field.setAccessible(true);

        Map<?, ?> knownClasses =
                (Map<?, ?>) field.get(extension);

        assertNotNull(knownClasses);
        assertTrue(knownClasses.isEmpty());
    }

    @Test
    void checkingWithoutKnownClassesDoesNotCreateEvents()
            throws Exception {

        java.lang.reflect.Method method =
                BlueJAnalyticsExtension.class
                        .getDeclaredMethod("checkOpenEditors");

        method.setAccessible(true);

        method.invoke(extension);

        assertTrue(
                TelemetryCollector.INSTANCE
                        .getEvents()
                        .isEmpty()
        );
    }

    @Test
    void removingUnknownEditorDoesNotCauseError()
            throws Exception {

        java.lang.reflect.Method method =
                BlueJAnalyticsExtension.class
                        .getDeclaredMethod(
                                "removeEditorTracker",
                                String.class
                        );

        method.setAccessible(true);

        assertDoesNotThrow(
                () -> method.invoke(
                        extension,
                        "UnknownClass"
                )
        );
    }

    @Test
    void terminatingWithoutStartupDoesNotRequireTimer()
            throws Exception {

        assertDoesNotThrow(
                () -> extension.terminate()
        );
    }
}
