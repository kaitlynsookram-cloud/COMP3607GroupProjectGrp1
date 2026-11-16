package com.bluej.analytics;

import java.util.List;

import com.bluej.analytics.analysis.TelemetryAnalyzer;
import com.bluej.analytics.exporter.JsonExporter;
import com.bluej.analytics.model.ErrorRecord;
import com.bluej.analytics.observer.TelemetryCollector;
import com.bluej.analytics.report.PdfReportGenerator;
/**
 * Creates sample events, exports data and generates a PDF report 
 * to demonstrate the telemetry analytics system
 * App
 */


public class App {
     /**
     * Makes sample compilation events,exports telemetry data 
     * & displays the compilation success rate
     * 
     */
    public static void main(String[] args) throws Exception {
        TelemetryCollector c = TelemetryCollector.INSTANCE;
        long t = System.currentTimeMillis();

        c.onEvent(EventFactoryHelper.of(t, 3, false, "C1", 1));
        c.onEvent(EventFactoryHelper.of(t + 5000, 2, false, "C2", 2));
        c.onEvent(EventFactoryHelper.of(t + 12000, 0, true, null, 0));

        new JsonExporter().export(c, "session_data.json");
        new PdfReportGenerator().generate(c, "session_report.pdf");

        TelemetryAnalyzer a = new TelemetryAnalyzer(c.getEvents());
        System.out.println("Success rate = " + a.compileSuccessRate());
    }
    /**
     * EventFactoryHelper:
     * for creating sample compilation events
     */
    
    static class EventFactoryHelper {
        static com.bluej.analytics.model.CompileEvent of(
                long ts, int errors, boolean ok, String code, int line) {
            List<ErrorRecord> list = code == null
                    ? List.of() : List.of(new ErrorRecord(code, "msg", line));
            return new com.bluej.analytics.model.CompileEvent(ts, errors, ok, list, 100);
        }
    }
}