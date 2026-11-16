package com.bluej.analytics.model;
/**
 * structure for telemetry events, each event following this structure
 * must provide a timestamp and event type
 */


public interface TelemetryEvent {
    long getTimestamp();
    String getEventType();
}