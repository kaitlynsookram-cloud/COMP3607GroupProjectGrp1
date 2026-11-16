package com.bluej.analytics.exporter;

import com.bluej.analytics.observer.TelemetryCollector;
import java.io.IOException;

/**Promts user before dumping cache required to prevent errors 
 * that occurrec without confirmation*/

//for BlueJ prompt user before cache dump; needed for code as errors occured without it
public class PersistenceService {
    public void saveSession(String path) throws IOException {
        new JsonExporter().export(TelemetryCollector.INSTANCE, path);
    }
}