package com.bluej.analytics;

import com.bluej.analytics.factory.EventFactory;
import com.bluej.analytics.observer.TelemetryCollector;
import org.junit.jupiter.api.*;

import java.util.List;

// FIX: Add this static import so assertEquals can be used directly
import static org.junit.jupiter.api.Assertions.*; 

class TelemetryCollectorTest {

    @BeforeEach
    void reset() { TelemetryCollector.INSTANCE.resetSession(); }

    @Test
    void singletonIsSameInstance() {
        assertSame(TelemetryCollector.INSTANCE, TelemetryCollector.INSTANCE);
    }

    @Test
    void observerStoresEvents() {
        // FIX: Corrected typo from TeemetryCollector to TelemetryCollector
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