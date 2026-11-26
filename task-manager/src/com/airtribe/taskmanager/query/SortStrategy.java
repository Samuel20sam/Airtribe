package com.airtribe.taskmanager.query;

import com.airtribe.taskmanager.model.Task;

import java.util.Comparator;

/**
 * Strategy pattern: one way of ordering tasks. The service looks strategies up by
 * {@link #name()} (the value of {@code sortBy}), so adding a sort order means writing a new class
 * and registering it with {@code TaskService.registerSortStrategy}; no existing class changes.
 */
public interface SortStrategy {

    /** The {@code sortBy} value that selects this strategy, for example "createdAt". */
    String name();

    /** A comparator for ascending or descending order. Ties should be broken by task ID. */
    Comparator<Task> comparator(boolean ascending);
}
