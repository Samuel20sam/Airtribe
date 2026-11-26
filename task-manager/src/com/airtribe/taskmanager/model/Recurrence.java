package com.airtribe.taskmanager.model;

/**
 * How often a task repeats. Optional: most tasks have no recurrence.
 * The AI parser fills it in for text such as "every month".
 */
public enum Recurrence {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;

    /** Finds the recurrence whose name matches exactly (upper case), or returns null. */
    public static Recurrence fromName(String name) {
        for (Recurrence r : values()) {
            if (r.name().equals(name)) {
                return r;
            }
        }
        return null;
    }
}
