
package com.bluej.analytics.model;

public class ClassLifecycleEvent implements TelemetryEvent {

    private final long timestamp;
    private final String className;
    private final String action;
    private final boolean compiled;
    private final boolean hasError;
    private final String oldName;

    public ClassLifecycleEvent(long timestamp, String className,
                               String action, boolean compiled,
                               boolean hasError, String oldName) {
        this.timestamp = timestamp;
        this.className = className;
        this.action = action;
        this.compiled = compiled;
        this.hasError = hasError;
        this.oldName = oldName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getEventType() {
        return "CLASS_EVENT";
    }

    public String getClassName() {
        return className;
    }

    public String getAction() {
        return action;
    }

    public boolean isCompiled() {
        return compiled;
    }

    public boolean hasError() {
        return hasError;
    }

    public String getOldName() {
        return oldName;
    }
}
