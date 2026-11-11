package com.bluej.analytics;

import bluej.extensions2.BlueJ;
import bluej.extensions2.Extension;
import bluej.extensions2.BClass;
import bluej.extensions2.editor.JavaEditor;
import bluej.extensions2.event.ClassEvent;
import bluej.extensions2.event.CompileEvent;
import bluej.extensions2.event.ExtensionEvent;
import bluej.extensions2.event.ExtensionEventListener;
import bluej.extensions2.event.InvocationFinishedEvent;
import bluej.extensions2.event.PackageEvent;

import com.bluej.analytics.exporter.PersistenceService;
import com.bluej.analytics.factory.EventFactory;
import com.bluej.analytics.model.ClassLifecycleEvent;
import com.bluej.analytics.model.ErrorRecord;
import com.bluej.analytics.model.ExecutionTelemetryEvent;
import com.bluej.analytics.model.ProjectEvent;
import com.bluej.analytics.model.SessionMetadata;
import com.bluej.analytics.observer.TelemetryCollector;
import com.bluej.analytics.report.PdfReportGenerator;
import com.bluej.analytics.tracking.EditorActivityTracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlueJAnalyticsExtension extends Extension
        implements ExtensionEventListener {

    private long compilationStartTime = 0;

    private final List<ErrorRecord> compilationErrors =
            new ArrayList<>();

    // Store one editor tracker for each class.
    private final Map<String, EditorActivityTracker> editorTrackers =
            new HashMap<>();

    // Store the editor so its listener can be removed later.
    private final Map<String, JavaEditor> trackedEditors =
            new HashMap<>();

    private final Path outputPath = Path.of(
            System.getProperty("user.home"),
            "bluej",
            "analytics-session.json"
    );

    @Override
    public String getName() {
        return "BlueJ Analytics";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public boolean isCompatible() {
        return true;
    }

    @Override
    public void startup(BlueJ bluej) {
        bluej.addExtensionEventListener(this);
        System.out.println("BlueJ Analytics extension started!");
    }

    @Override
    public synchronized void eventOccurred(ExtensionEvent event) {

        if (event instanceof InvocationFinishedEvent) {
            InvocationFinishedEvent e =
                    (InvocationFinishedEvent) event;

            TelemetryCollector.INSTANCE.onEvent(
                    new ExecutionTelemetryEvent(
                            System.currentTimeMillis(),
                            e.getClassName(),
                            e.getMethodName(),
                            e.getObjectName(),
                            e.getEventType().name(),
                            e.getParameters()
                    )
            );

            saveJsonFile();
            return;
        }

        if (event instanceof ClassEvent) {
            ClassEvent e = (ClassEvent) event;
            BClass c = e.getBClass();

            String name = c == null
                    ? "Unknown"
                    : c.getName();

            TelemetryCollector.INSTANCE.onEvent(
                    new ClassLifecycleEvent(
                            System.currentTimeMillis(),
                            name,
                            e.getEventType().name(),
                            e.isClassCompiled(),
                            e.hasError(),
                            e.getOldName()
                    )
            );

            if (c != null) {
                attachEditorTracker(c);
            }

            saveJsonFile();
            return;
        }

        if (event instanceof PackageEvent) {
            PackageEvent e = (PackageEvent) event;

            String name = e.getPackage() == null
                    ? "Unknown"
                    : e.getPackage().toString();

            TelemetryCollector.INSTANCE.onEvent(
                    new ProjectEvent(
                            System.currentTimeMillis(),
                            name,
                            e.getEventType().name()
                    )
            );

            saveJsonFile();
            return;
        }

        if (!(event instanceof CompileEvent)) {
            return;
        }

        CompileEvent e = (CompileEvent) event;

        switch (e.getEventType()) {

            case COMPILE_START_EVENT:
                compilationStartTime = System.currentTimeMillis();
                compilationErrors.clear();
                break;

            case COMPILE_ERROR_EVENT:
                compilationErrors.add(
                        new ErrorRecord(
                                "COMPILER_ERROR",
                                e.getErrorMessage(),
                                e.getErrorLineNumber()
                        )
                );
                break;

            case COMPILE_DONE_EVENT:
                saveCompilation(true);
                break;

            case COMPILE_FAILED_EVENT:
                saveCompilation(false);
                break;

            default:
                break;
        }
    }

    // Attach only one document listener per class.
    private void attachEditorTracker(BClass c) {
        try {
            JavaEditor editor = c.getJavaEditorIfOpen();

            if (editor == null) {
                return;
            }

            String className = c.getName();

            // BlueJ may return different JavaEditor objects
            // for the same class. Avoid attaching extra listeners.
            if (editorTrackers.containsKey(className)) {
                return;
            }

            EditorActivityTracker tracker =
                    new EditorActivityTracker(className);

            editor.addDocumentListener(tracker);

            editorTrackers.put(className, tracker);
            trackedEditors.put(className, editor);

            System.out.println(
                    "Editor tracker attached: " + className
            );

        } catch (Exception e) {
            System.err.println(
                    "Could not attach editor tracker: " +
                            e.getMessage()
            );
        }
    }

    // Record compilation results and update session statistics.
    private void saveCompilation(boolean successful) {

        if (compilationStartTime == 0) {
            return;
        }

        long duration =
                System.currentTimeMillis() - compilationStartTime;

        TelemetryCollector.INSTANCE.onEvent(
                EventFactory.createCompileEvent(
                        compilationErrors.size(),
                        successful,
                        new ArrayList<>(compilationErrors),
                        duration
                )
        );

        SessionMetadata metadata =
                TelemetryCollector.INSTANCE.getMetadata();

        metadata.setTotalCompilations(
                metadata.getTotalCompilations() + 1
        );

        metadata.setTotalErrors(
                metadata.getTotalErrors() +
                        compilationErrors.size()
        );

        metadata.updateSessionDuration();

        compilationStartTime = 0;
        compilationErrors.clear();

        saveJsonFile();
        savePdfReport();
    }

    // Export the session information as JSON.
    private void saveJsonFile() {
        try {
            TelemetryCollector.INSTANCE
                    .getMetadata()
                    .updateSessionDuration();

            Files.createDirectories(outputPath.getParent());

            new PersistenceService().saveSession(
                    outputPath.toString()
            );

        } catch (IOException e) {
            System.err.println(
                    "JSON export failed: " + e.getMessage()
            );
        }
    }

    // Generate the PDF analytics report.
    private void savePdfReport() {
        try {
            Path pdfPath = outputPath.getParent()
                    .resolve("analytics-report.pdf");

            Files.createDirectories(pdfPath.getParent());

            new PdfReportGenerator().generate(
                    TelemetryCollector.INSTANCE,
                    pdfPath.toString()
            );

        } catch (IOException e) {
            System.err.println(
                    "PDF report failed: " + e.getMessage()
            );
        }
    }

    // Save the session and remove listeners when BlueJ closes.
    @Override
    public void terminate() {

        for (Map.Entry<String, EditorActivityTracker> entry
                : editorTrackers.entrySet()) {

            JavaEditor editor =
                    trackedEditors.get(entry.getKey());

            if (editor != null) {
                editor.removeDocumentListener(
                        entry.getValue()
                );
            }
        }

        editorTrackers.clear();
        trackedEditors.clear();

        TelemetryCollector.INSTANCE
                .getMetadata()
                .setSessionEndMs(System.currentTimeMillis());

        saveJsonFile();
        savePdfReport();

        System.out.println(
                "BlueJ Analytics extension stopped."
        );
    }
}
