package com.bluej.analytics.exporter;

import com.bluej.analytics.observer.TelemetryCollector;
import java.io.IOException;

/*Allows Bluej to prompt user before cache dump*/
public class PersistenceService {
    public void saveSession(String path) throws IOException {
        new JsonExporter().export(TelemetryCollector.INSTANCE, path);
    }
}