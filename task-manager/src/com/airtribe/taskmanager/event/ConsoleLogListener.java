package com.airtribe.taskmanager.event;

import com.airtribe.taskmanager.model.Task;

import java.io.PrintStream;

/** Prints one line per event, for example {@code [event] CREATED #1 [ ] Pay rent (MEDIUM)}. */
public class ConsoleLogListener implements TaskEventListener {

    private final PrintStream out;

    public ConsoleLogListener() {
        this(System.out);
    }

    public ConsoleLogListener(PrintStream out) {
        this.out = out;
    }

    @Override
    public void onEvent(TaskEventType eventType, Task task) {
        out.println("  [event] " + eventType + " " + task);
    }
}
