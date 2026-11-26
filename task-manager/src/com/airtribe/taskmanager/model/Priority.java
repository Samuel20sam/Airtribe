package com.airtribe.taskmanager.model;

/** How important a task is. A task without a priority gets {@link #MEDIUM}. */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH;

    /**
     * Finds the priority whose name matches exactly (upper case), or returns null.
     * Returning null instead of throwing lets the validator report a clear message.
     */
    public static Priority fromName(String name) {
        for (Priority p : values()) {
            if (p.name().equals(name)) {
                return p;
            }
        }
        return null;
    }
}
