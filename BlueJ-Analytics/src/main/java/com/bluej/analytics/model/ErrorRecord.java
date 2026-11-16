package com.bluej.analytics.model;
/**
 * ErrorRecord
 * This class represents a compilation error , holdig its error code,
 * error message and line number where it happened
 */

public class ErrorRecord {
    private final String errorCode;
    private final String message;
    private final int lineNumber;

    public ErrorRecord(String errorCode, String message, int lineNumber) {
        this.errorCode = errorCode;
        this.message = message;
        this.lineNumber = lineNumber;
    }

    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public int getLineNumber() { return lineNumber; }
}