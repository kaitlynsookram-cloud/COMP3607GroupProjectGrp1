package com.bluej.analytics;

import com.bluej.analytics.model.EditorEvent;
import com.bluej.analytics.model.TelemetryEvent;
import com.bluej.analytics.observer.TelemetryCollector;
import com.bluej.analytics.tracking.EditorActivityTracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EditorActivityTrackerTest {

    private EditorActivityTracker tracker;

    @BeforeEach
    void setUp() {
        TelemetryCollector.INSTANCE.resetSession();
        tracker = new EditorActivityTracker("HelloWorld");
    }

    @Test
    void typingCreatesEditorEvent() {
        tracker.textReplaced(0, "", "Hello", 0, 5);

        List<TelemetryEvent> events =
                TelemetryCollector.INSTANCE.getEvents();

        assertEquals(1, events.size());
        assertInstanceOf(EditorEvent.class, events.get(0));
    }

    @Test
    void editorEventStoresCorrectClassName() {
        tracker.textReplaced(0, "", "abc", 0, 3);

        EditorEvent event = (EditorEvent)
                TelemetryCollector.INSTANCE.getEvents().get(0);

        assertEquals("HelloWorld", event.getClassName());
    }

    @Test
    void insertionRecordsCharacterCount() {
        tracker.textReplaced(0, "", "Hello", 0, 5);

        EditorEvent event = (EditorEvent)
                TelemetryCollector.INSTANCE.getEvents().get(0);

        assertEquals(5, event.getKeystrokeCount());
    }

    @Test
    void deletionRecordsCharacterCount() {
        tracker.textReplaced(0, "Hello", "", 5, 0);

        EditorEvent event = (EditorEvent)
                TelemetryCollector.INSTANCE.getEvents().get(0);

        assertEquals(5, event.getKeystrokeCount());
    }

    @Test
    void replacementRecordsLargerCharacterCount() {
        tracker.textReplaced(0, "Hi", "Hello", 2, 5);

        EditorEvent event = (EditorEvent)
                TelemetryCollector.INSTANCE.getEvents().get(0);

        assertEquals(5, event.getKeystrokeCount());
    }

    @Test
    void firstEditHasNoIdleTime() {
        tracker.textReplaced(0, "", "a", 0, 1);

        EditorEvent event = (EditorEvent)
                TelemetryCollector.INSTANCE.getEvents().get(0);

        assertEquals(0, event.getIdleTimeMs());
    }

    @Test
    void multipleEditsCreateMultipleEvents() {
        tracker.textReplaced(0, "", "a", 0, 1);
        tracker.textReplaced(1, "", "b", 0, 1);
        tracker.textReplaced(2, "", "c", 0, 1);

        assertEquals(
                3,
                TelemetryCollector.INSTANCE.getEvents().size()
        );
    }

    @Test
    void editorEventHasTimestamp() {
        long before = System.currentTimeMillis();

        tracker.textReplaced(0, "", "Test", 0, 4);

        long after = System.currentTimeMillis();

        EditorEvent event = (EditorEvent)
                TelemetryCollector.INSTANCE.getEvents().get(0);

        assertTrue(event.getTimestamp() >= before);
        assertTrue(event.getTimestamp() <= after);
    }

    @Test
    void editorEventHasCorrectType() {
        tracker.textReplaced(0, "", "Test", 0, 4);

        EditorEvent event = (EditorEvent)
                TelemetryCollector.INSTANCE.getEvents().get(0);

        assertEquals("EDITOR", event.getEventType());
    }
}
