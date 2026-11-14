package com.bluej.analytics;

import com.bluej.analytics.tracking.MethodDefinitionTracker;
import com.bluej.analytics.observer.TelemetryCollector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class MethodDefinitionTrackerTest {

    private MethodDefinitionTracker tracker;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        TelemetryCollector.INSTANCE.resetSession();
        tracker = new MethodDefinitionTracker();
    }

    private Path createFile(String content) throws Exception {
        Path file = tempDir.resolve("TestClass.java");
        Files.writeString(file, content);
        return file;
    }

    @Test
    void nullFileDoesNotRecordEvents() throws Exception {
        tracker.scan("TestClass", null);

        assertTrue(
                TelemetryCollector.INSTANCE.getEvents().isEmpty()
        );
    }

    @Test
    void missingFileDoesNotRecordEvents() throws Exception {
        Path file = tempDir.resolve("Missing.java");

        tracker.scan("TestClass", file);

        assertTrue(
                TelemetryCollector.INSTANCE.getEvents().isEmpty()
        );
    }

    @Test
    void firstScanDoesNotRecordChanges() throws Exception {
        Path file = createFile(
                "public class TestClass {" +
                        " public void hello() {}" +
                        "}"
        );

        tracker.scan("TestClass", file);

        assertTrue(
                TelemetryCollector.INSTANCE.getEvents().isEmpty()
        );
    }

    @Test
    void unchangedMethodDoesNotRecordChanges() throws Exception {
        Path file = createFile(
                "public class TestClass {" +
                        " public void hello() {}" +
                        "}"
        );

        tracker.scan("TestClass", file);
        tracker.scan("TestClass", file);

        assertTrue(
                TelemetryCollector.INSTANCE.getEvents().isEmpty()
        );
    }

    @Test
    void addedMethodRecordsEvent() throws Exception {
        Path file = createFile(
                "public class TestClass {" +
                        " public void hello() {}" +
                        "}"
        );

        tracker.scan("TestClass", file);

        Files.writeString(file,
                "public class TestClass {" +
                        " public void hello() {}" +
                        " public void goodbye() {}" +
                        "}"
        );

        tracker.scan("TestClass", file);

        assertEquals(
                1,
                TelemetryCollector.INSTANCE.getEvents().size()
        );
    }

    @Test
    void modifiedMethodRecordsEvent() throws Exception {
        Path file = createFile(
                "public class TestClass {" +
                        " public int calculate() { return 1; }" +
                        "}"
        );

        tracker.scan("TestClass", file);

        Files.writeString(file,
                "public class TestClass {" +
                        " public int calculate() { return 2; }" +
                        "}"
        );

        tracker.scan("TestClass", file);

        assertEquals(
                1,
                TelemetryCollector.INSTANCE.getEvents().size()
        );
    }

    @Test
    void removedMethodRecordsEvent() throws Exception {
        Path file = createFile(
                "public class TestClass {" +
                        " public void hello() {}" +
                        " public void goodbye() {}" +
                        "}"
        );

        tracker.scan("TestClass", file);

        Files.writeString(file,
                "public class TestClass {" +
                        " public void hello() {}" +
                        "}"
        );

        tracker.scan("TestClass", file);

        assertEquals(
                1,
                TelemetryCollector.INSTANCE.getEvents().size()
        );
    }

    @Test
    void multipleMethodChangesAreRecorded() throws Exception {
        Path file = createFile(
                "public class TestClass {" +
                        " public int first() { return 1; }" +
                        " public void second() {}" +
                        "}"
        );

        tracker.scan("TestClass", file);

        Files.writeString(file,
                "public class TestClass {" +
                        " public int first() { return 5; }" +
                        " public void third() {}" +
                        "}"
        );

        tracker.scan("TestClass", file);

        assertEquals(
                3,
                TelemetryCollector.INSTANCE.getEvents().size()
        );
    }

    @Test
    void methodWithParametersIsDetected() throws Exception {
        Path file = createFile(
                "public class TestClass {" +
                        " public int add(int a, int b) { return a + b; }" +
                        "}"
        );

        tracker.scan("TestClass", file);

        Files.writeString(file,
                "public class TestClass {" +
                        " public int add(int a, int b) { return a - b; }" +
                        "}"
        );

        tracker.scan("TestClass", file);

        assertEquals(
                1,
                TelemetryCollector.INSTANCE.getEvents().size()
        );
    }

    @Test
    void nestedBracesAreHandled() throws Exception {
        Path file = createFile(
                "public class TestClass {" +
                        " public void check() {" +
                        " if (true) { System.out.println(1); }" +
                        " }" +
                        "}"
        );

        tracker.scan("TestClass", file);

        Files.writeString(file,
                "public class TestClass {" +
                        " public void check() {" +
                        " if (true) { System.out.println(2); }" +
                        " }" +
                        "}"
        );

        tracker.scan("TestClass", file);

        assertEquals(
                1,
                TelemetryCollector.INSTANCE.getEvents().size()
        );
    }

    @Test
    void removingClassClearsPreviousSnapshot() throws Exception {
        Path file = createFile(
                "public class TestClass {" +
                        " public void hello() {}" +
                        "}"
        );

        tracker.scan("TestClass", file);

        tracker.remove("TestClass");

        Files.writeString(file,
                "public class TestClass {" +
                        " public void goodbye() {}" +
                        "}"
        );

        tracker.scan("TestClass", file);

        assertTrue(
                TelemetryCollector.INSTANCE.getEvents().isEmpty()
        );
    }

    @Test
    void separateClassesHaveSeparateSnapshots() throws Exception {
        Path first = tempDir.resolve("First.java");
        Path second = tempDir.resolve("Second.java");

        Files.writeString(first,
                "public class First {" +
                        " public void one() {}" +
                        "}"
        );

        Files.writeString(second,
                "public class Second {" +
                        " public void two() {}" +
                        "}"
        );

        tracker.scan("First", first);
        tracker.scan("Second", second);

        Files.writeString(first,
                "public class First {" +
                        " public void one() {}" +
                        " public void three() {}" +
                        "}"
        );

        tracker.scan("First", first);

        assertEquals(
                1,
                TelemetryCollector.INSTANCE.getEvents().size()
        );
    }
}