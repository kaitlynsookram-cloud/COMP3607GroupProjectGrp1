package com.bluej.analytics.observer;

public interface TelemetrySubject {
    void registerObserver(TelemetryObserver o);
    void removeObserver(TelemetryObserver o);
    void notifyObservers(com.bluej.analytics.model.TelemetryEvent e);
}