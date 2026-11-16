
package com.bluej.analytics.factory;

import java.util.List;

import com.bluej.analytics.model.CompileEvent;
import com.bluej.analytics.model.EditorEvent;
import com.bluej.analytics.model.ErrorRecord;
import com.bluej.analytics.model.ExecutionTelemetryEvent;
/**
* Create telemetry events and then assigns
* a timestamp when events are created
*
*Made by: Anthony Lakicharan
*/


public class EventFactory {
        /**
        * Makes a compilation event with compilation results and 
        * errors found
        * @return a new CompileEvent
        */


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
    /**
     * Makes editor eventused to record user editing activity
     * 
     * @return a new EditorEvent
     */

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
     /**
     * Makes excution event containing method excution
     *  details and runtime errors found
     * @return a new ExecutionTelementryEvent
     */


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
