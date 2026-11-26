package com.airtribe.taskmanager.query;

import com.airtribe.taskmanager.model.Priority;
import com.airtribe.taskmanager.model.Task;

/** Keeps only tasks with one priority. */
public class PriorityFilter implements TaskFilter {

    private final Priority priority;

    public PriorityFilter(Priority priority) {
        this.priority = priority;
    }

    @Override
    public boolean matches(Task task) {
        return task.getPriority() == priority;
    }
}
