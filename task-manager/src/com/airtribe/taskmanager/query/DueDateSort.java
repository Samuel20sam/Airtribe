package com.airtribe.taskmanager.query;

import com.airtribe.taskmanager.model.Task;

import java.util.Comparator;

/**
 * Orders tasks by due date. YYYY-MM-DD strings sort correctly as text, so no date parsing is
 * needed. Tasks without a due date always come last, in both directions.
 */
public class DueDateSort implements SortStrategy {

    public static final String NAME = "dueDate";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Comparator<Task> comparator(boolean ascending) {
        Comparator<String> dates = ascending ? Comparator.naturalOrder() : Comparator.reverseOrder();
        Comparator<Task> byId = Comparator.comparingInt(Task::getId);
        return Comparator.comparing(Task::getDueDate, Comparator.nullsLast(dates))
                .thenComparing(ascending ? byId : byId.reversed());
    }
}
