package com.bluej.analytics.model;

/** Common contract for all telemetry events for uniform iteration. */
public interface TelemetryEvent {
    long getTimestamp();
    String getEventType();
}