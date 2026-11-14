package com.bluej.analytics.tracking;

import com.bluej.analytics.model.MethodDefinitionEvent;
import com.bluej.analytics.observer.TelemetryCollector;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MethodDefinitionTracker {

    private static final Pattern METHOD = Pattern.compile(
            "(?:public|private|protected|static|final|synchronized|abstract|native|\\s)+"
                    + "[\\w<>\\[\\], ?]+\\s+([a-zA-Z_$][\\w$]*)\\s*"
                    + "\\(([^)]*)\\)\\s*(?:throws\\s+[\\w., ]+)?\\{"
    );

    private final Map<String, Map<String, String>> previous =
            new HashMap<>();

    public void scan(String className, Path file)
            throws IOException {

        if (file == null || !Files.isRegularFile(file)) {
            return;
        }

        String source = Files.readString(file);

        Map<String, String> current = new HashMap<>();

        Matcher matcher = METHOD.matcher(source);

        while (matcher.find()) {

            String signature = matcher.group(1)
                    + "(" + matcher.group(2).trim() + ")";

            int open = source.indexOf('{', matcher.start());

            int depth = 0;
            int end = open;

            for (; end < source.length(); end++) {

                char ch = source.charAt(end);

                if (ch == '{') {
                    depth++;
                } else if (ch == '}') {
                    depth--;

                    if (depth == 0) {
                        end++;
                        break;
                    }
                }
            }

            current.put(
                    signature,
                    source.substring(
                            matcher.start(),
                            Math.min(end, source.length())
                    )
            );
        }

        Map<String, String> old = previous.get(className);

        if (old != null) {

            for (String signature : current.keySet()) {

                String action = null;

                if (!old.containsKey(signature)) {
                    action = "ADDED";
                } else if (!current.get(signature)
                        .equals(old.get(signature))) {
                    action = "MODIFIED";
                }

                if (action != null) {
                    record(className, signature, action);
                }
            }

            for (String signature : old.keySet()) {

                if (!current.containsKey(signature)) {
                    record(className, signature, "REMOVED");
                }
            }
        }

        previous.put(className, current);
    }

    public void remove(String className) {
        previous.remove(className);
    }

    private void record(
            String className,
            String signature,
            String action) {

        TelemetryCollector.INSTANCE.onEvent(
                new MethodDefinitionEvent(
                        System.currentTimeMillis(),
                        className,
                        signature,
                        action
                )
        );
    }
}
