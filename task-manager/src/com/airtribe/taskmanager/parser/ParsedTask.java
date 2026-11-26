package com.airtribe.taskmanager.parser;

import com.airtribe.taskmanager.model.TaskRequest;

/**
 * What a {@link TaskParser} understood from a sentence. Every value is a raw String and may be
 * wrong (an empty title, an impossible date), so it is turned into a {@link TaskRequest} and goes
 * through the normal validation before anything is saved.
 */
public final class ParsedTask {

    private final String title;
    private final String description;
    private final String dueDate;
    private final String recurrence;

    public ParsedTask(String title, String description, String dueDate, String recurrence) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.recurrence = recurrence;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    /** YYYY-MM-DD or null. */
    public String getDueDate() {
        return dueDate;
    }

    /** DAILY, WEEKLY, MONTHLY, YEARLY or null. */
    public String getRecurrence() {
        return recurrence;
    }

    /** A new task request: not completed, default priority. */
    public TaskRequest toTaskRequest() {
        return new TaskRequest(title, description, "false", dueDate, null, recurrence);
    }

    @Override
    public String toString() {
        return "ParsedTask{title='" + title + "', description='" + description + "', dueDate='" + dueDate
                + "', recurrence='" + recurrence + "'}";
    }
}
