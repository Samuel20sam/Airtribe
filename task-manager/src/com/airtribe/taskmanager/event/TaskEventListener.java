package com.airtribe.taskmanager.event;

import com.airtribe.taskmanager.model.Task;

/**
 * Observer pattern: anything that wants to react to task changes implements this interface and
 * registers with {@code TaskService.addListener}. The service (the subject) calls
 * {@link #onEvent} after each change, without knowing what the listener does.
 */
public interface TaskEventListener {

    void onEvent(TaskEventType eventType, Task task);
}
