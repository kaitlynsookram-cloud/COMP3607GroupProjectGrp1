package com.bluej.analytics.observer;

import com.bluej.analytics.model.TelemetryEvent;

public interface TelemetryObserver {
    void onEvent(TelemetryEvent event);
}