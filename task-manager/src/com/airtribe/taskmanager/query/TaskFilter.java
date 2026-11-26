package com.airtribe.taskmanager.query;

import com.airtribe.taskmanager.model.Task;

/**
 * One rule a task must pass to appear in a list. Each filter is its own class, so a new filter
 * is a new class: no existing class has to change (Open/Closed principle). A query keeps only
 * the tasks that pass every filter.
 */
public interface TaskFilter {

    boolean matches(Task task);
}
