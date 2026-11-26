package com.airtribe.taskmanager.query;

import com.airtribe.taskmanager.model.Task;

import java.util.Locale;

/** Keeps tasks whose title or description contains the search text, ignoring upper/lower case. */
public class SearchFilter implements TaskFilter {

    private final String term;

    public SearchFilter(String term) {
        this.term = term.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean matches(Task task) {
        return task.getTitle().toLowerCase(Locale.ROOT).contains(term)
                || task.getDescription().toLowerCase(Locale.ROOT).contains(term);
    }
}
