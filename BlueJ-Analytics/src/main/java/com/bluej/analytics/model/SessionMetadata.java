package com.bluej.analytics.model;

public class SessionMetadata {

    private final String studentId;
    private final long sessionStartMs;
    private long sessionEndMs;
    private int totalCompilations;
    private int totalErrors;
    private long sessionDurationMs;

    public SessionMetadata(String studentId, long sessionStartMs) {
        this.studentId = studentId;
        this.sessionStartMs = sessionStartMs;
    }

    public String getStudentId() {
        return studentId;
    }

    public long getSessionStartMs() {
        return sessionStartMs;
    }

    public long getSessionEndMs() {
        return sessionEndMs;
    }

    public void setSessionEndMs(long v) {
        this.sessionEndMs = v;
        this.sessionDurationMs = Math.max(0, v - sessionStartMs);
    }

    public int getTotalCompilations() {
        return totalCompilations;
    }

    public void setTotalCompilations(int v) {
        this.totalCompilations = v;
    }

    public int getTotalErrors() {
        return totalErrors;
    }

    public void setTotalErrors(int v) {
        this.totalErrors = v;
    }

    public long getSessionDurationMs() {
        return sessionDurationMs;
    }

    public void updateSessionDuration() {
        sessionDurationMs =
                Math.max(0, System.currentTimeMillis() - sessionStartMs);
    }
}
