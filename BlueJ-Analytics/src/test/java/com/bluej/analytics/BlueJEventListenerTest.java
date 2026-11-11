
package com.bluej.analytics;

import bluej.extensions2.BClass;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.PackageEvent;
import bluej.extensions2.event.InvocationFinishedEvent;

import com.bluej.analytics.model.ClassLifecycleEvent;
import com.bluej.analytics.model.ExecutionTelemetryEvent;
import com.bluej.analytics.model.ProjectEvent;
import com.bluej.analytics.observer.TelemetryCollector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BlueJEventListenerTest {

    private BlueJAnalyticsExtension extension;

    @BeforeEach
    void setUp() {
        TelemetryCollector.INSTANCE.resetSession();
        extension = new BlueJAnalyticsExtension();
    }

    @Test
    void executionEventIsRecorded() {

        InvocationFinishedEvent event =
                mock(InvocationFinishedEvent.class);

        when(event.getClassName()).thenReturn("Calculator");
        when(event.getMethodName()).thenReturn("add");
        when(event.getObjectName()).thenReturn("calculator1");

        when(event.getParameters()).thenReturn(
                new String[]{"5", "10"}
        );

        when(event.getEventType()).thenReturn(
                InvocationFinishedEvent.EventType.NORMAL_EXIT
        );

        extension.eventOccurred(event);

        assertEquals(
                1,
                TelemetryCollector.INSTANCE.getEvents().size()
        );

        ExecutionTelemetryEvent saved =
                (ExecutionTelemetryEvent)
                        TelemetryCollector.INSTANCE.getEvents().get(0);

        assertEquals("EXECUTION_EVENT", saved.getEventType());
        assertEquals("Calculator", saved.getClassName());
        assertEquals("add", saved.getMethodName());
        assertEquals("calculator1", saved.getObjectName());
        assertEquals("NORMAL_EXIT", saved.getOutcome());

        assertArrayEquals(
                new String[]{"5", "10"},
                saved.getParameters()
        );
    }

    @Test
    void classEventIsRecorded() {

        ClassEvent event = mock(ClassEvent.class);
        BClass bClass = mock(BClass.class);

        when(event.getBClass()).thenReturn(bClass);
        when(bClass.getName()).thenReturn("HelloWorld");

        when(event.getEventType()).thenReturn(
                ClassEvent.EventType.STATE_CHANGED
        );

        when(event.isClassCompiled()).thenReturn(true);
        when(event.hasError()).thenReturn(false);

        extension.eventOccurred(event);

        assertEquals(
                1,
                TelemetryCollector.INSTANCE.getEvents().size()
        );

        ClassLifecycleEvent saved =
                (ClassLifecycleEvent)
                        TelemetryCollector.INSTANCE.getEvents().get(0);

        assertEquals("CLASS_EVENT", saved.getEventType());
        assertEquals("HelloWorld", saved.getClassName());
        assertEquals("STATE_CHANGED", saved.getAction());
        assertTrue(saved.isCompiled());
        assertFalse(saved.hasError());
    }

    @Test
    void classEventWithMissingClassIsRecorded() {

        ClassEvent event = mock(ClassEvent.class);

        when(event.getBClass()).thenReturn(null);

        when(event.getEventType()).thenReturn(
                ClassEvent.EventType.REMOVED
        );

        extension.eventOccurred(event);

        assertEquals(
                1,
                TelemetryCollector.INSTANCE.getEvents().size()
        );

        ClassLifecycleEvent saved =
                (ClassLifecycleEvent)
                        TelemetryCollector.INSTANCE.getEvents().get(0);

        assertEquals("Unknown", saved.getClassName());
        assertEquals("REMOVED", saved.getAction());
    }

    @Test
    void projectOpeningEventIsRecorded() {

        PackageEvent event = mock(PackageEvent.class);

        when(event.getPackage()).thenReturn(null);

        when(event.getEventType()).thenReturn(
                PackageEvent.EventType.PACKAGE_OPENED
        );

        extension.eventOccurred(event);

        assertEquals(
                1,
                TelemetryCollector.INSTANCE.getEvents().size()
        );

        ProjectEvent saved =
                (ProjectEvent)
                        TelemetryCollector.INSTANCE.getEvents().get(0);

        assertEquals("PROJECT_EVENT", saved.getEventType());
        assertEquals("PACKAGE_OPENED", saved.getAction());
        assertEquals("Unknown", saved.getProjectName());
        assertTrue(saved.getTimestamp() > 0);
    }

    @Test
    void projectClosingEventIsRecorded() {

        PackageEvent event = mock(PackageEvent.class);

        when(event.getPackage()).thenReturn(null);

        when(event.getEventType()).thenReturn(
                PackageEvent.EventType.PACKAGE_CLOSING
        );

        extension.eventOccurred(event);

        assertEquals(
                1,
                TelemetryCollector.INSTANCE.getEvents().size()
        );

        ProjectEvent saved =
                (ProjectEvent)
                        TelemetryCollector.INSTANCE.getEvents().get(0);

        assertEquals("PROJECT_EVENT", saved.getEventType());
        assertEquals("PACKAGE_CLOSING", saved.getAction());
        assertEquals("Unknown", saved.getProjectName());
    }
}
