package com.airtribe.taskmanager.parser;

import com.airtribe.taskmanager.model.Task;
import com.airtribe.taskmanager.model.TaskRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Works offline: splits the task's description on commas and the word "and"
 * ("book venue, send invites and order cake" -> 3 subtasks). If the description has nothing to
 * split, the title is tried. If neither splits into at least two parts, an empty list is returned
 * and the service answers 502 because there is nothing to save.
 */
public class RuleBasedBreakdownStrategy implements BreakdownStrategy {

    private static final String SEPARATORS = "(?i)\\s*,\\s*(?:and\\s+)?|\\s+and\\s+";

    @Override
    public List<TaskRequest> breakDown(Task task) {
        List<String> parts = split(task.getDescription());
        if (parts.size() < 2) {
            parts = split(task.getTitle());
        }
        List<TaskRequest> subtasks = new ArrayList<>();
        if (parts.size() < 2) {
            return subtasks;
        }
        for (String part : parts) {
            String title = Character.toUpperCase(part.charAt(0)) + part.substring(1);
            subtasks.add(new TaskRequest(title, "Part of: " + task.getTitle(), "false",
                    task.getDueDate(), task.getPriority().name()));
        }
        return subtasks;
    }

    private static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        for (String piece : text.split(SEPARATORS)) {
            String part = piece.trim();
            while (part.endsWith(".")) {
                part = part.substring(0, part.length() - 1).trim();
            }
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        return parts;
    }
}
