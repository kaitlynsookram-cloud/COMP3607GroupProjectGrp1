
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
import com.bluej.analytics.model.ProjectEvent;
import com.bluej.analytics.model.SessionMetadata;
import com.bluej.analytics.observer.TelemetryCollector;
import com.bluej.analytics.report.PdfReportGenerator;
import com.bluej.analytics.tracking.EditorActivityTracker;
import com.bluej.analytics.tracking.MethodDefinitionTracker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

public class BlueJAnalyticsExtension extends Extension
        implements ExtensionEventListener {

    private long compilationStartTime = 0;

    private final MethodDefinitionTracker methodDefinitions =
            new MethodDefinitionTracker();

    private final List<ErrorRecord> compilationErrors =
            new ArrayList<>();

    private final Map<String, EditorActivityTracker> editorTrackers =
            new HashMap<>();

    private final Map<String, JavaEditor> trackedEditors =
            new HashMap<>();

    private final Map<String, BClass> knownClasses =
            new HashMap<>();

    private final Map<String, String> sourceSnapshots =
            new HashMap<>();

    private final Map<String, Long> lastSourceEdit =
            new HashMap<>();

    private static final long IDLE_THRESHOLD_MS = 5000;

    private Timer editorCheckTimer;

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

    // Start tracking when BlueJ opens.
    @Override
    public void startup(BlueJ bluej) {

        bluej.addExtensionEventListener(this);

        editorCheckTimer = new Timer(
                500,
                e -> checkOpenEditors()
        );

        editorCheckTimer.start();

        System.out.println(
                "BlueJ Analytics extension started!"
        );
    }

    // Check which event happened.
    @Override
    public synchronized void eventOccurred(ExtensionEvent event) {

        if (event instanceof InvocationFinishedEvent) {

            InvocationFinishedEvent e =
                    (InvocationFinishedEvent) event;

            String outcome = e.getEventType().name();

            boolean runtimeException =
                    e.getEventType() ==
                            InvocationFinishedEvent.EventType.EXCEPTION_EXIT;

            TelemetryCollector.INSTANCE.onEvent(
                    EventFactory.createExecutionEvent(
                            e.getClassName(),
                            e.getMethodName(),
                            e.getObjectName(),
                            outcome,
                            e.getParameters(),
                            runtimeException,
                            null
                    )
            );

            if (runtimeException) {
                System.out.println(
                        "Runtime exception detected in " +
                                e.getClassName() + "." +
                                e.getMethodName()
                );
            }

            saveJsonFile();
            savePdfReport();
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

            if (e.getEventType() ==
                    ClassEvent.EventType.REMOVED) {

                removeEditorTracker(name);
                methodDefinitions.remove(name);
                knownClasses.remove(name);
                sourceSnapshots.remove(name);
                lastSourceEdit.remove(name);

            } else if (c != null) {

                if (e.getOldName() != null &&
                        !e.getOldName().equals(name)) {

                    removeEditorTracker(e.getOldName());
                    knownClasses.remove(e.getOldName());
                    sourceSnapshots.remove(e.getOldName());
                    lastSourceEdit.remove(e.getOldName());
                }

                knownClasses.put(name, c);

                attachEditorTracker(c);
                recordSourceActivity(c);
                scanMethodDefinitions(c);
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

                compilationStartTime =
                        System.currentTimeMillis();

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

    // Look for changes in the methods.
    private void scanMethodDefinitions(BClass c) {

        try {

            Object file = c.getClass()
                    .getMethod("getJavaFile")
                    .invoke(c);

            if (file instanceof java.io.File) {

                methodDefinitions.scan(
                        c.getName(),
                        ((java.io.File) file).toPath()
                );

            } else if (file instanceof Path) {

                methodDefinitions.scan(
                        c.getName(),
                        (Path) file
                );
            }

        } catch (ReflectiveOperationException ignored) {

        } catch (IOException e) {

            System.err.println(
                    "Method scan failed: " +
                            e.getMessage()
            );
        }
    }

    // Compare the code with the last saved version.
    private void recordSourceActivity(BClass c) {

        try {
            Path path = null;

            try {
                Object file = c.getClass()
                        .getMethod("getJavaFile")
                        .invoke(c);

                if (file instanceof java.io.File) {
                    path = ((java.io.File) file).toPath();
                } else if (file instanceof Path) {
                    path = (Path) file;
                }

            } catch (ReflectiveOperationException e) {
            }

            if (path == null || !Files.isRegularFile(path)) {

                Object pkg = c.getPackage();

                Object directory = pkg.getClass()
                        .getMethod("getDir")
                        .invoke(pkg);

                if (directory instanceof java.io.File) {
                    path = ((java.io.File) directory).toPath();
                } else if (directory instanceof Path) {
                    path = (Path) directory;
                } else {
                    return;
                }

                String simpleName = c.getName();
                int dot = simpleName.lastIndexOf('.');

                if (dot >= 0) {
                    simpleName = simpleName.substring(dot + 1);
                }

                path = path.resolve(simpleName + ".java");
            }

            if (!Files.isRegularFile(path)) {
                System.err.println("Source file not found: " + path);
                return;
            }

            String name = c.getName();
            String current = Files.readString(path);
            String previous = sourceSnapshots.put(name, current);

            if (previous == null || previous.equals(current)) {
                return;
            }

            int start = 0;
            int limit = Math.min(previous.length(), current.length());

            while (start < limit &&
                    previous.charAt(start) == current.charAt(start)) {
                start++;
            }

            int oldEnd = previous.length() - 1;
            int newEnd = current.length() - 1;

            while (oldEnd >= start &&
                    newEnd >= start &&
                    previous.charAt(oldEnd) == current.charAt(newEnd)) {
                oldEnd--;
                newEnd--;
            }

            int changed = Math.max(
                    oldEnd - start + 1,
                    newEnd - start + 1
            );

            long now = System.currentTimeMillis();
            Long last = lastSourceEdit.put(name, now);

            long idle = 0;

            if (last != null && now - last > 5000) {
                idle = now - last;
            }

            TelemetryCollector.INSTANCE.onEvent(
                    EventFactory.createEditorEvent(
                            name,
                            idle,
                            changed
                    )
            );

            saveJsonFile();

        } catch (Exception e) {
            System.err.println(
                    "Source tracking error: " + e.getMessage()
            );
        }
    }

    // Check if anything changed in the editor.
    private synchronized void checkOpenEditors() {

        for (BClass c :
                new ArrayList<>(knownClasses.values())) {

            attachEditorTracker(c);
            recordSourceActivity(c);
        }
    }

    // Listen for changes while typing.
    private void attachEditorTracker(BClass c) {

        try {

            JavaEditor editor =
                    c.getJavaEditorIfOpen();

            if (editor == null) {
                return;
            }

            String className = c.getName();

            if (editorTrackers.containsKey(className)) {

                if (trackedEditors.get(className) == editor) {
                    return;
                }

                removeEditorTracker(className);
            }

            EditorActivityTracker tracker =
                    new EditorActivityTracker(className);

            editor.addDocumentListener(tracker);

            editorTrackers.put(className, tracker);
            trackedEditors.put(className, editor);

            System.out.println(
                    "Editor tracker attached: " +
                            className
            );

        } catch (Exception e) {

            System.err.println(
                    "Could not attach editor tracker: " +
                            e.getMessage()
            );
        }
    }

    private void removeEditorTracker(String className) {

        EditorActivityTracker tracker =
                editorTrackers.remove(className);

        JavaEditor editor =
                trackedEditors.remove(className);

        if (tracker != null && editor != null) {

            try {

                editor.removeDocumentListener(tracker);

            } catch (Exception e) {

                System.err.println(
                        "Could not remove editor tracker: " +
                                e.getMessage()
                );
            }
        }
    }

    // Keep track of compilations and errors.
    private void saveCompilation(boolean successful) {

        if (compilationStartTime == 0) {
            return;
        }

        long duration =
                System.currentTimeMillis() -
                        compilationStartTime;

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

    // Save the data to the JSON file.
    private void saveJsonFile() {

        try {

            TelemetryCollector.INSTANCE
                    .getMetadata()
                    .updateSessionDuration();

            Files.createDirectories(
                    outputPath.getParent()
            );

            new PersistenceService().saveSession(
                    outputPath.toString()
            );

        } catch (IOException e) {

            System.err.println(
                    "JSON export failed: " +
                            e.getMessage()
            );
        }
    }

    // Make the PDF report.
    private void savePdfReport() {

        try {

            Path pdfPath = outputPath.getParent()
                    .resolve("analytics-report.pdf");

            Files.createDirectories(
                    pdfPath.getParent()
            );

            new PdfReportGenerator().generate(
                    TelemetryCollector.INSTANCE,
                    pdfPath.toString()
            );

        } catch (IOException e) {

            System.err.println(
                    "PDF report failed: " +
                            e.getMessage()
            );
        }
    }

    @Override
    public void terminate() {

        if (editorCheckTimer != null) {
            editorCheckTimer.stop();
        }

        for (String className :
                new ArrayList<>(editorTrackers.keySet())) {

            removeEditorTracker(className);
        }

        TelemetryCollector.INSTANCE
                .getMetadata()
                .setSessionEndMs(
                        System.currentTimeMillis()
                );

        saveJsonFile();
        savePdfReport();

        System.out.println(
                "BlueJ Analytics extension stopped."
        );
    }
}
