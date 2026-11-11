package com.bluej.analytics.model;
import java.util.List;

public class CompileEvent implements TelemetryEvent {
    private final long timestamp;
    private final int errorCount;
    private final boolean successful;
    private final List<ErrorRecord> errors;
    private final long compileDurationMs;

    public CompileEvent(long timestamp, int errorCount, boolean successful,
                        List<ErrorRecord> errors, long compileDurationMs) {
        this.timestamp = timestamp;
        this.errorCount = errorCount;
        this.successful = successful; // Make sure this is spelled correctly!
        this.errors = errors;
        this.compileDurationMs = compileDurationMs;
    }

    @Override public long getTimestamp() { return timestamp; }
    @Override public String getEventType() { return "COMPILE"; }
    public int getErrorCount() { return errorCount; }
    public boolean isSuccessful() { return successful; }
    public List<ErrorRecord> getErrors() { return errors; }
    public long getCompileDurationMs() { return compileDurationMs; }
}