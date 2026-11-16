package com.bluej.analytics.model;
/**
 * An event that notes actions done on a project,
 * project name and action done.
 */

public class ProjectEvent implements TelemetryEvent {

    private final long timestamp;
    private final String projectName;
    private final String action;

    public ProjectEvent(long timestamp, String projectName, String action) {
        this.timestamp = timestamp;
        this.projectName = projectName;
        this.action = action;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getEventType() {
        return "PROJECT_EVENT";
    }

    public String getProjectName() {
        return projectName;
    }

    public String getAction() {
        return action;
    }
}
