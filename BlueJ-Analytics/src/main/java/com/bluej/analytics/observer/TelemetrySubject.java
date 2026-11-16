package com.bluej.analytics.observer;
/** 
 * Interface for managing observers and notifying them of telemetry events.    
 */

public interface TelemetrySubject {
    void registerObserver(TelemetryObserver o);
    void removeObserver(TelemetryObserver o);
    void notifyObservers(com.bluej.analytics.model.TelemetryEvent e);
}