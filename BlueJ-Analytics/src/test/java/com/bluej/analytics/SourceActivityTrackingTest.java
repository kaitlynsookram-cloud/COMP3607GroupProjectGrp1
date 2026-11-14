package com.bluej.analytics;

import bluej.extensions2.BClass;

import com.bluej.analytics.observer.TelemetryCollector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SourceActivityTrackingTest {

    private BlueJAnalyticsExtension extension;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        TelemetryCollector.INSTANCE.resetSession();
        extension = new BlueJAnalyticsExtension();
    }

    private void scan(BClass c) throws Exception {
        Method method = BlueJAnalyticsExtension.class
                .getDeclaredMethod("recordSourceActivity", BClass.class);

        method.setAccessible(true);
        method.invoke(extension, c);
    }

    private BClass createClass(Path path) throws Exception {
        BClass c = mock(BClass.class);

        when(c.getName()).thenReturn("TestClass");

        when(c.getJavaFile()).thenReturn(path.toFile());

        return c;
    }

    @Test
    void firstScanDoesNotRecordEditing() throws Exception {
        Path file = tempDir.resolve("TestClass.java");
        Files.writeString(file, "class TestClass {}");

        BClass c = createClass(file);

        scan(c);

        assertTrue(TelemetryCollector.INSTANCE.getEvents().isEmpty());
    }

    @Test
    void unchangedSourceDoesNotRecordEditing() throws Exception {
        Path file = tempDir.resolve("TestClass.java");
        Files.writeString(file, "class TestClass {}");

        BClass c = createClass(file);

        scan(c);
        scan(c);

        assertTrue(TelemetryCollector.INSTANCE.getEvents().isEmpty());
    }

    @Test
    void addingTextRecordsEditing() throws Exception {
        Path file = tempDir.resolve("TestClass.java");
        Files.writeString(file, "class TestClass {}");

        BClass c = createClass(file);

        scan(c);

        Files.writeString(file,
                "class TestClass { int number = 5; }");

        scan(c);

        assertEquals(1,
                TelemetryCollector.INSTANCE.getEvents().size());
    }

    @Test
    void deletingTextRecordsEditing() throws Exception {
        Path file = tempDir.resolve("TestClass.java");
        Files.writeString(file,
                "class TestClass { int number = 5; }");

        BClass c = createClass(file);

        scan(c);

        Files.writeString(file, "class TestClass {}");

        scan(c);

        assertEquals(1,
                TelemetryCollector.INSTANCE.getEvents().size());
    }

    @Test
    void replacingTextRecordsEditing() throws Exception {
        Path file = tempDir.resolve("TestClass.java");
        Files.writeString(file,
                "class TestClass { int a = 1; }");

        BClass c = createClass(file);

        scan(c);

        Files.writeString(file,
                "class TestClass { int b = 2; }");

        scan(c);

        assertEquals(1,
                TelemetryCollector.INSTANCE.getEvents().size());
    }

    @Test
    void multipleSavedChangesRecordMultipleEvents() throws Exception {
        Path file = tempDir.resolve("TestClass.java");
        Files.writeString(file, "class TestClass {}");

        BClass c = createClass(file);

        scan(c);

        Files.writeString(file, "class TestClass { int a; }");
        scan(c);

        Files.writeString(file, "class TestClass { int a; int b; }");
        scan(c);

        assertEquals(2,
                TelemetryCollector.INSTANCE.getEvents().size());
    }

    @Test
    void missingFileDoesNotRecordEditing() throws Exception {
        Path file = tempDir.resolve("Missing.java");

        BClass c = createClass(file);

        scan(c);

        assertTrue(TelemetryCollector.INSTANCE.getEvents().isEmpty());
    }

    @Test
    void idleTimeIsDetectedAfterLongGap() throws Exception {
        Path file = tempDir.resolve("TestClass.java");
        Files.writeString(file, "class TestClass {}");

        BClass c = createClass(file);

        scan(c);

        Files.writeString(file, "class TestClass { int a; }");
        scan(c);

        java.lang.reflect.Field field =
                BlueJAnalyticsExtension.class
                        .getDeclaredField("lastSourceEdit");

        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, Long> times =
                (Map<String, Long>) field.get(extension);

        times.put("TestClass",
                System.currentTimeMillis() - 7000);

        Files.writeString(file,
                "class TestClass { int a; int b; }");

        scan(c);

        assertEquals(2,
                TelemetryCollector.INSTANCE.getEvents().size());
    }
    @Test
    void sourceWithOnlyWhitespaceChangesRecordsEditing() throws Exception {
        Path file = tempDir.resolve("TestClass.java");
        Files.writeString(file, "class TestClass {}");

        BClass c = createClass(file);

        scan(c);

        Files.writeString(file, "class TestClass {  }");

        scan(c);

        assertEquals(1,
                TelemetryCollector.INSTANCE.getEvents().size());
    }

    @Test
    void sourceChangedAtBeginningRecordsEditing() throws Exception {
        Path file = tempDir.resolve("TestClass.java");
        Files.writeString(file, "class TestClass {}");

        BClass c = createClass(file);

        scan(c);

        Files.writeString(file, "public class TestClass {}");

        scan(c);

        assertEquals(1,
                TelemetryCollector.INSTANCE.getEvents().size());
    }
}
