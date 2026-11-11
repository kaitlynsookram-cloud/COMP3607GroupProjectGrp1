
package com.bluej.analytics.model;

public class ExecutionTelemetryEvent implements TelemetryEvent {

    private final long timestamp;
    private final String className;
    private final String methodName;
    private final String objectName;
    private final String outcome;
    private final String[] parameters;

    public ExecutionTelemetryEvent(
            long timestamp,
            String className,
            String methodName,
            String objectName,
            String outcome,
            String[] parameters) {

        this.timestamp = timestamp;
        this.className = className;
        this.methodName = methodName;
        this.objectName = objectName;
        this.outcome = outcome;
        this.parameters = parameters == null
                ? new String[0]
                : parameters.clone();
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getEventType() {
        return "EXECUTION_EVENT";
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getObjectName() {
        return objectName;
    }

    public String getOutcome() {
        return outcome;
    }

    public String[] getParameters() {
        return parameters.clone();
    }
}

