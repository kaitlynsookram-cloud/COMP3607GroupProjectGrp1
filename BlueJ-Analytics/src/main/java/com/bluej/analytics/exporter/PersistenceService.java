package com.bluej.analytics.exporter;

import java.io.IOException;

import com.bluej.analytics.observer.TelemetryCollector;

/**
 * Promts user before dumping cache required to prevent errors 
 * that occurred without confirmation
 *
 * Made by: Kaitlyn Sookram
 */

//for BlueJ prompt user before cache dump; needed for code as errors occured without it
public class PersistenceService {
    public void saveSession(String path) throws IOException {
        new JsonExporter().export(TelemetryCollector.INSTANCE, path);
    }
}