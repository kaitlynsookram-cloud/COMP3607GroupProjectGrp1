package com.bluej.analytics.observer;
/**
 * Observer interface for handling telemetry events
 */
import com.bluej.analytics.model.TelemetryEvent;

public interface TelemetryObserver {
    void onEvent(TelemetryEvent event);
}