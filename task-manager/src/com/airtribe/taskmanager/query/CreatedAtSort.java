package com.airtribe.taskmanager.query;

import com.airtribe.taskmanager.model.Task;

import java.util.Comparator;

/** Orders tasks by creation time. Tasks created in the same millisecond keep their ID order. */
public class CreatedAtSort implements SortStrategy {

    public static final String NAME = "createdAt";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Comparator<Task> comparator(boolean ascending) {
        Comparator<Task> byCreated = Comparator.comparingLong(Task::getCreatedAt)
                .thenComparingInt(Task::getId);
        return ascending ? byCreated : byCreated.reversed();
    }
}
