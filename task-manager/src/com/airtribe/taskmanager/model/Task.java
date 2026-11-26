package com.airtribe.taskmanager.model;

import com.airtribe.taskmanager.util.DateHelper;

/**
 * An immutable task. Every field is private and final and there are no setters.
 * The only way to create a task is through {@link Builder}; to "change" a task,
 * copy it with {@link #toBuilder()} and build a new one.
 *
 * <p>The builder protects the object's own rules (title and description are never blank,
 * the due date is a real date). The friendly, field-by-field error messages for API callers
 * come earlier, from {@code TaskValidator} in the service layer.
 */
public final class Task {

    private final int id;
    private final String title;
    private final String description;
    private final boolean completed;
    private final String dueDate;
    private final Priority priority;
    private final Integer parentId;
    private final Recurrence recurrence;
    private final long createdAt;

    private Task(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.description = builder.description;
        this.completed = builder.completed;
        this.dueDate = builder.dueDate;
        this.priority = builder.priority;
        this.parentId = builder.parentId;
        this.recurrence = builder.recurrence;
        this.createdAt = builder.createdAt;
    }

    /** 0 until the repository saves the task and assigns a real ID (starting at 1). */
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return completed;
    }

    /** YYYY-MM-DD, or null when the task has no due date. */
    public String getDueDate() {
        return dueDate;
    }

    public Priority getPriority() {
        return priority;
    }

    /** ID of the parent task, or null for a top-level task. */
    public Integer getParentId() {
        return parentId;
    }

    /** How often the task repeats, or null. */
    public Recurrence getRecurrence() {
        return recurrence;
    }

    /** Creation time in milliseconds since 1970 ({@code System.currentTimeMillis()}). */
    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isSubtask() {
        return parentId != null;
    }

    /** A builder pre-filled with this task's values. */
    public Builder toBuilder() {
        return new Builder(title, description, completed)
                .id(id)
                .dueDate(dueDate)
                .priority(priority)
                .parentId(parentId)
                .recurrence(recurrence)
                .createdAt(createdAt);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('#').append(id)
                .append(completed ? " [x] " : " [ ] ")
                .append(title)
                .append(" (").append(priority);
        if (dueDate != null) {
            sb.append(", due ").append(dueDate);
        }
        if (recurrence != null) {
            sb.append(", ").append(recurrence);
        }
        if (parentId != null) {
            sb.append(", subtask of #").append(parentId);
        }
        return sb.append(')').toString();
    }

    /**
     * Builds a {@link Task}. Title, description and completed are required and go in the
     * constructor; everything else is optional and has a sensible default.
     */
    public static final class Builder {
        private final String title;
        private final String description;
        private final boolean completed;

        private int id;
        private String dueDate;
        private Priority priority = Priority.MEDIUM;
        private Integer parentId;
        private Recurrence recurrence;
        private long createdAt = System.currentTimeMillis();

        public Builder(String title, String description, boolean completed) {
            this.title = requireText(title, "title");
            this.description = requireText(description, "description");
            this.completed = completed;
        }

        public Builder id(int id) {
            if (id < 0) {
                throw new IllegalArgumentException("id must not be negative");
            }
            this.id = id;
            return this;
        }

        public Builder dueDate(String dueDate) {
            if (dueDate != null && !DateHelper.isValidDate(dueDate)) {
                throw new IllegalArgumentException("dueDate must be a real YYYY-MM-DD date: " + dueDate);
            }
            this.dueDate = dueDate;
            return this;
        }

        /** Null means "not given", so the default MEDIUM is kept. */
        public Builder priority(Priority priority) {
            this.priority = priority == null ? Priority.MEDIUM : priority;
            return this;
        }

        public Builder parentId(Integer parentId) {
            if (parentId != null && parentId <= 0) {
                throw new IllegalArgumentException("parentId must be positive");
            }
            this.parentId = parentId;
            return this;
        }

        public Builder recurrence(Recurrence recurrence) {
            this.recurrence = recurrence;
            return this;
        }

        public Builder createdAt(long createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Task build() {
            return new Task(this);
        }

        private static String requireText(String value, String field) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field + " must not be empty");
            }
            return value;
        }
    }
}
