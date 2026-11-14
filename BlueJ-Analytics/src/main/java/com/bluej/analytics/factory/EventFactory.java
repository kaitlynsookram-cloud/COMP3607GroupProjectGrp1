
package com.bluej.analytics.factory;

import com.bluej.analytics.model.*;

import java.util.List;

public class EventFactory {

    //compilation event creation
    public static CompileEvent createCompileEvent(
            int errorCount,
            boolean successful,
            List<ErrorRecord> errors,
            long durationMs) {

        return new CompileEvent(
                System.currentTimeMillis(),
                errorCount,
                successful,
                errors,
                durationMs
        );
    }

    //editor activity events
    public static EditorEvent createEditorEvent(
            String className,
            long idleTimeMs,
            int keystrokes) {

        return new EditorEvent(
                System.currentTimeMillis(),
                className,
                idleTimeMs,
                keystrokes
        );
    }

    public static ExecutionTelemetryEvent createExecutionEvent(
            String className,
            String methodName,
            String objectName,
            String outcome,
            String[] parameters,
            boolean runtimeException,
            String exceptionMessage) {

        return new ExecutionTelemetryEvent(
                System.currentTimeMillis(),
                className,
                methodName,
                objectName,
                outcome,
                parameters,
                runtimeException,
                exceptionMessage
        );
    }
}
