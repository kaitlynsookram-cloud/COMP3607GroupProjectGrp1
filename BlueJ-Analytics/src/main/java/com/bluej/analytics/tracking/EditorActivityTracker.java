package com.bluej.analytics.tracking;

import bluej.extensions2.editor.DocumentListener;

import com.bluej.analytics.factory.EventFactory;
import com.bluej.analytics.observer.TelemetryCollector;

public class EditorActivityTracker implements DocumentListener {

    private final String className;
    private long lastEditTime = 0;

    private static final long IDLE_THRESHOLD = 5000;

    public EditorActivityTracker(String className) {
        this.className = className;
    }

    @Override
    public void textReplaced(int offset, String oldText,
                             String newText, int oldLength,
                             int newLength) {

        //to ignore notifs if no changes; was causing a lot of duplicating
        if (oldLength == 0 && newLength == 0) {
            return;
        }

        long now = System.currentTimeMillis();

        long idleTime = 0;

        if (lastEditTime != 0) {
            long gap = now - lastEditTime;

            if (gap > IDLE_THRESHOLD) {
                idleTime = gap;
            }
        }

        int charactersChanged = Math.max(
                oldLength,
                newLength
        );

        TelemetryCollector.INSTANCE.onEvent(
                EventFactory.createEditorEvent(
                        className,
                        idleTime,
                        charactersChanged
                )
        );

        lastEditTime = now;
    }
}
