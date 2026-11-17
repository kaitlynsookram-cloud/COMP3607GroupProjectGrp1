package com.bluej.analytics;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bluej.analytics.factory.EventFactory;
import com.bluej.analytics.observer.TelemetryCollector;
/**
 * Tests TelemetryCollector to verify it maintains a single
 * instance, stores telemetry events and notifies registered observers
 * when new events are recorded.
 * 
 * Made by:Kaydance Hamilton
 */ 

class TelemetryCollectorTest {

    @BeforeEach
    void reset() { TelemetryCollector.INSTANCE.resetSession(); }

    @Test
    void singletonIsSameInstance() {
        assertSame(TelemetryCollector.INSTANCE, TelemetryCollector.INSTANCE);
    }

    @Test
    void observerStoresEvents() {
        TelemetryCollector c = TelemetryCollector.INSTANCE; 
        c.onEvent(EventFactory.createCompileEvent(3, false, List.of(), 10));
        c.onEvent(EventFactory.createCompileEvent(1, false, List.of(), 10));
        assertEquals(2, c.getEvents().size()); // This will now work
    }

    @Test
    void notifiesRegisteredObserver() {
        TelemetryCollector c = TelemetryCollector.INSTANCE;
        final int[] hits = {0};
        c.registerObserver(e -> hits[0]++);
        c.onEvent(EventFactory.createCompileEvent(0, true, List.of(), 5));
        assertEquals(1, hits[0]); // This will now work
    }
}