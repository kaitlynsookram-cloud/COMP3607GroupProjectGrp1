package com.bluej.analytics.observer;

import com.bluej.analytics.model.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public enum TelemetryCollector implements TelemetryObserver, TelemetrySubject {

    INSTANCE;

    private final List<TelemetryEvent> events = new CopyOnWriteArrayList<>();
    private final List<TelemetryObserver> observers = new CopyOnWriteArrayList<>();
    private SessionMetadata metadata = new SessionMetadata("816043852",
            System.currentTimeMillis());

    @Override
    public void onEvent(TelemetryEvent event) {
        events.add(event);
        notifyObservers(event);
    }

    @Override
    public void registerObserver(TelemetryObserver o) { observers.add(o); }

    @Override
    public void removeObserver(TelemetryObserver o) { observers.remove(o); }

    @Override
    public void notifyObservers(TelemetryEvent e) {
        for (TelemetryObserver o : observers) o.onEvent(e);
    }

    public List<TelemetryEvent> getEvents() { return Collections.unmodifiableList(events); }

    public void resetSession() {
        events.clear();
        metadata = new SessionMetadata("816043852", System.currentTimeMillis());
    }

    public SessionMetadata getMetadata() { return metadata; }
    public void setMetadata(SessionMetadata m) { this.metadata = m; }
}