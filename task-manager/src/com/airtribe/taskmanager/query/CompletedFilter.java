package com.airtribe.taskmanager.query;

import com.airtribe.taskmanager.model.Task;

/** Keeps only completed tasks, or only open tasks. */
public class CompletedFilter implements TaskFilter {

    private final boolean completed;

    public CompletedFilter(boolean completed) {
        this.completed = completed;
    }

    @Override
    public boolean matches(Task task) {
        return task.isCompleted() == completed;
    }
}
