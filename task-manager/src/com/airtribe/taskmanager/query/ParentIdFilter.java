package com.airtribe.taskmanager.query;

import com.airtribe.taskmanager.model.Task;

/** Keeps only the subtasks of one parent task. */
public class ParentIdFilter implements TaskFilter {

    private final int parentId;

    public ParentIdFilter(int parentId) {
        this.parentId = parentId;
    }

    @Override
    public boolean matches(Task task) {
        return task.getParentId() != null && task.getParentId() == parentId;
    }
}
