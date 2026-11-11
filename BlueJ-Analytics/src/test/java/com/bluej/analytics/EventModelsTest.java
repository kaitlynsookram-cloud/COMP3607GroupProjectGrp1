
package com.bluej.analytics;

import com.bluej.analytics.model.ProjectEvent;
import com.bluej.analytics.model.ClassLifecycleEvent;
import com.bluej.analytics.model.ExecutionTelemetryEvent;
import com.bluej.analytics.model.TelemetryEvent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EventModelsTest {

    @Test
    void projectEventStoresDetails() {
        ProjectEvent event = new ProjectEvent(
                1000L, "AnalyticsTest", "PACKAGE_OPENED"
        );

        assertEquals(1000L, event.getTimestamp());
        assertEquals("PROJECT_EVENT", event.getEventType());
        assertEquals("AnalyticsTest", event.getProjectName());
        assertEquals("PACKAGE_OPENED", event.getAction());
        assertInstanceOf(TelemetryEvent.class, event);
    }

    @Test
    void projectClosingEventStoresAction() {
        ProjectEvent event = new ProjectEvent(
                2000L, "AnalyticsTest", "PACKAGE_CLOSING"
        );

        assertEquals("PACKAGE_CLOSING", event.getAction());
    }

    @Test
    void classStateChangeStoresDetails() {
        ClassLifecycleEvent event = new ClassLifecycleEvent(
                3000L, "HelloWorld", "STATE_CHANGED",
                true, false, null
        );

        assertEquals(3000L, event.getTimestamp());
        assertEquals("CLASS_EVENT", event.getEventType());
        assertEquals("HelloWorld", event.getClassName());
        assertEquals("STATE_CHANGED", event.getAction());
        assertTrue(event.isCompiled());
        assertFalse(event.hasError());
        assertNull(event.getOldName());
    }

    @Test
    void classRenameStoresOldName() {
        ClassLifecycleEvent event = new ClassLifecycleEvent(
                4000L, "NewName", "CHANGED_NAME",
                false, false, "OldName"
        );

        assertEquals("CHANGED_NAME", event.getAction());
        assertEquals("NewName", event.getClassName());
        assertEquals("OldName", event.getOldName());
    }

    @Test
    void classRemovalStoresAction() {
        ClassLifecycleEvent event = new ClassLifecycleEvent(
                5000L, "DeletedClass", "REMOVED",
                false, false, null
        );

        assertEquals("REMOVED", event.getAction());
    }

    @Test
    void classCompilationErrorIsStored() {
        ClassLifecycleEvent event = new ClassLifecycleEvent(
                6000L, "TestClass", "STATE_CHANGED",
                false, true, null
        );

        assertFalse(event.isCompiled());
        assertTrue(event.hasError());
    }

    @Test
    void executionEventStoresDetails() {
        ExecutionTelemetryEvent event =
                new ExecutionTelemetryEvent(
                        7000L,
                        "HelloWorld",
                        "main",
                        null,
                        "NORMAL_EXIT",
                        new String[]{"test"}
                );

        assertEquals(7000L, event.getTimestamp());
        assertEquals("EXECUTION_EVENT", event.getEventType());
        assertEquals("HelloWorld", event.getClassName());
        assertEquals("main", event.getMethodName());
        assertNull(event.getObjectName());
        assertEquals("NORMAL_EXIT", event.getOutcome());
        assertArrayEquals(
                new String[]{"test"},
                event.getParameters()
        );
    }

    @Test
    void executionExceptionIsStored() {
        ExecutionTelemetryEvent event =
                new ExecutionTelemetryEvent(
                        8000L,
                        "Calculator",
                        "divide",
                        "calculator1",
                        "EXCEPTION_EXIT",
                        new String[]{"10", "0"}
                );

        assertEquals("EXCEPTION_EXIT", event.getOutcome());
        assertEquals("calculator1", event.getObjectName());
        assertEquals("divide", event.getMethodName());
    }

    @Test
    void executionTerminationIsStored() {
        ExecutionTelemetryEvent event =
                new ExecutionTelemetryEvent(
                        9000L,
                        "Loop",
                        "run",
                        null,
                        "TERMINATED_EXIT",
                        null
                );

        assertEquals("TERMINATED_EXIT", event.getOutcome());
        assertEquals(0, event.getParameters().length);
    }

    @Test
    void executionParametersAreProtectedFromChanges() {
        String[] original = {"5", "10"};

        ExecutionTelemetryEvent event =
                new ExecutionTelemetryEvent(
                        10000L,
                        "Calculator",
                        "add",
                        null,
                        "NORMAL_EXIT",
                        original
                );

        original[0] = "999";

        assertEquals("5", event.getParameters()[0]);

        String[] returned = event.getParameters();
        returned[1] = "999";

        assertEquals("10", event.getParameters()[1]);
    }
}
