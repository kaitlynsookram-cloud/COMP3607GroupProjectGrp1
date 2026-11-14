package com.bluej.analytics.model;

public interface TelemetryEvent {
    long getTimestamp();
    String getEventType();
}