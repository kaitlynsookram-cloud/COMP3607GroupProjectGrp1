package com.bluej.analytics.model;

public class EditorEvent implements TelemetryEvent {
    private final long timestamp;
    private final String className;
    private final long idleTimeMs;
    private final int keystrokeCount;

    public EditorEvent(long timestamp, String className, long idleTimeMs, int keystrokeCount) {
        this.timestamp = timestamp;
        this.className = className;
        this.idleTimeMs = idleTimeMs;
        this.keystrokeCount = keystrokeCount;
    }

    @Override public long getTimestamp() { return timestamp; }
    @Override public String getEventType() { return "EDITOR"; }
    public String getClassName() { return className; }
    public long getIdleTimeMs() { return idleTimeMs; }
    public int getKeystrokeCount() { return keystrokeCount; }
}