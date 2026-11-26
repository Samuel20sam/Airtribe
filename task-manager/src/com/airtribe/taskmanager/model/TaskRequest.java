package com.airtribe.taskmanager.model;

/**
 * The raw input for creating or updating a task, exactly as a client sent it.
 * Every field is a String (even "completed"), so the service's validation decides what
 * is acceptable instead of the Java type system silently converting values.
 *
 * <p>Required: title, description. Optional (null or blank means "not given"):
 * completed (default "false"), dueDate, priority (default "MEDIUM"), recurrence.
 */
public class TaskRequest {

    private String title;
    private String description;
    private String completed;
    private String dueDate;
    private String priority;
    private String recurrence;

    public TaskRequest() {
    }

    public TaskRequest(String title, String description, String completed, String dueDate, String priority) {
        this(title, description, completed, dueDate, priority, null);
    }

    public TaskRequest(String title, String description, String completed, String dueDate, String priority,
                       String recurrence) {
        this.title = title;
        this.description = description;
        this.completed = completed;
        this.dueDate = dueDate;
        this.priority = priority;
        this.recurrence = recurrence;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCompleted() {
        return completed;
    }

    public void setCompleted(String completed) {
        this.completed = completed;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }

    /**
     * All field values joined into one String. Two requests with the same contents have the
     * same fingerprint. Used by the idempotency store to spot "same key, different request".
     * Each value is written with its length so that, for example, title "a|b" + description "c"
     * can never collide with title "a" + description "b|c".
     */
    public String fingerprint() {
        StringBuilder sb = new StringBuilder();
        for (String value : new String[] {title, description, completed, dueDate, priority, recurrence}) {
            if (value == null) {
                sb.append("-1:;");
            } else {
                sb.append(value.length()).append(':').append(value).append(';');
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "TaskRequest{title='" + title + "', description='" + description + "', completed='" + completed
                + "', dueDate='" + dueDate + "', priority='" + priority + "', recurrence='" + recurrence + "'}";
    }
}
