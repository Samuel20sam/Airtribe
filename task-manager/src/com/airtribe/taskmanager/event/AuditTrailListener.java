package com.airtribe.taskmanager.event;

import com.airtribe.taskmanager.model.Task;

import java.util.EnumMap;
import java.util.Map;

/** Counts how many events of each type it has seen. */
public class AuditTrailListener implements TaskEventListener {

    private final Map<TaskEventType, Integer> counts = new EnumMap<>(TaskEventType.class);

    @Override
    public synchronized void onEvent(TaskEventType eventType, Task task) {
        counts.merge(eventType, 1, Integer::sum);
    }

    public synchronized int getCount(TaskEventType eventType) {
        return counts.getOrDefault(eventType, 0);
    }

    /** A copy of the counts, with every event type present (0 when never seen). */
    public synchronized Map<TaskEventType, Integer> getCounts() {
        Map<TaskEventType, Integer> copy = new EnumMap<>(TaskEventType.class);
        for (TaskEventType type : TaskEventType.values()) {
            copy.put(type, counts.getOrDefault(type, 0));
        }
        return copy;
    }

    public synchronized int getTotal() {
        int total = 0;
        for (int count : counts.values()) {
            total += count;
        }
        return total;
    }
}
