package com.airtribe.taskmanager.event;

/** What happened to a task. */
public enum TaskEventType {
    CREATED,
    UPDATED,
    /** Sent after UPDATED when an update changes "completed" from false to true. */
    COMPLETED,
    DELETED
}
