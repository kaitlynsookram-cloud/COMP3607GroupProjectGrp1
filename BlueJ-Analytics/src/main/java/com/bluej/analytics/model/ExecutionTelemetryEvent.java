package com.bluej.analytics.model;
/**
 * ExecutionTelemetryEvent
 * a program execution event,noting the class, method
 * object, outcome and parameters.This event also stores the runtime exception
 * details when necessary  
 */


public class ExecutionTelemetryEvent implements TelemetryEvent {

    private final long timestamp;
    private final String className;
    private final String methodName;
    private final String objectName;
    private final String outcome;
    private final String[] parameters;

    private final boolean runtimeException;
    private final String exceptionMessage;

 /**
 *Makes an execution event with no rutime exception details
 *Original constructor kept for compatibility with existing code
 */
    public ExecutionTelemetryEvent(
            long timestamp,
            String className,
            String methodName,
            String objectName,
            String outcome,
            String[] parameters) {

        this(timestamp, className, methodName, objectName,
                outcome, parameters, false, null);
    }

    /**Makes an execution event with runtime exception details */
    public ExecutionTelemetryEvent(
            long timestamp,
            String className,
            String methodName,
            String objectName,
            String outcome,
            String[] parameters,
            boolean runtimeException,
            String exceptionMessage) {

        this.timestamp = timestamp;
        this.className = className;
        this.methodName = methodName;
        this.objectName = objectName;
        this.outcome = outcome;
        this.parameters = parameters == null
                ? new String[0]
                : parameters.clone();

        this.runtimeException = runtimeException;
        this.exceptionMessage = exceptionMessage;
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

    public boolean hasRuntimeException() {
        return runtimeException;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }
}
