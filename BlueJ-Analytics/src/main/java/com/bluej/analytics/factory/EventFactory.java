package com.bluej.analytics.factory;

import com.bluej.analytics.model.*;
import java.util.List;

public class EventFactory {

    public static CompileEvent createCompileEvent(int errorCount, boolean successful,
        List<ErrorRecord> errors,
         long durationMs) 
         {
            return new CompileEvent(System.currentTimeMillis(), errorCount,
            successful, errors, durationMs);
         }

    public static EditorEvent createEditorEvent(String className,
    long idleTimeMs, int keystrokes)
    {
       return new EditorEvent(System.currentTimeMillis(), className, idleTimeMs, keystrokes);
    }
}