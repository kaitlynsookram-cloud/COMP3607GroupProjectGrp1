package com.bluej.analytics.model;

public class MethodDefinitionEvent implements TelemetryEvent {

    private final long timestamp;
    private final String className;
    private final String methodSignature;
    private final String action;

    public MethodDefinitionEvent(
            long timestamp,
            String className,
            String methodSignature,
            String action) {

        this.timestamp = timestamp;
        this.className = className;
        this.methodSignature = methodSignature;
        this.action = action;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getEventType() {
        return "METHOD_DEFINITION";
    }

    public String getClassName() {
        return className;
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String getAction() {
        return action;
    }
}

