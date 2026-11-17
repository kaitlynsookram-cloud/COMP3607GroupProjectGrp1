package com.bluej.analytics.model;
/**
 * Structure for telemetry events, each event following this structure
 * must provide a timestamp and event type
 * 
 * Made by: Kaydance Hamilton
 */


public interface TelemetryEvent {
    long getTimestamp();
    String getEventType();
}