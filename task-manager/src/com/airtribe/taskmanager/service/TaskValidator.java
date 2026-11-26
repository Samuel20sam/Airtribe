package com.airtribe.taskmanager.service;

import com.airtribe.taskmanager.exception.FieldIssue;
import com.airtribe.taskmanager.exception.ValidationException;
import com.airtribe.taskmanager.model.Priority;
import com.airtribe.taskmanager.model.Recurrence;
import com.airtribe.taskmanager.model.Task;
import com.airtribe.taskmanager.model.TaskRequest;
import com.airtribe.taskmanager.util.DateHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks a raw {@link TaskRequest} against every rule and collects all problems, so one response
 * can report several mistakes at once. The same validator checks requests typed by a person and
 * tasks produced by the AI parser ("never trust the model").
 *
 * <p>Rules:
 * <ul>
 *   <li>title and description must not be empty or blank (and have a maximum length)</li>
 *   <li>completed, if given, must be exactly "true" or "false" (missing means false)</li>
 *   <li>dueDate, if given, must be a real date in YYYY-MM-DD format</li>
 *   <li>priority, if given, must be LOW, MEDIUM or HIGH (missing means MEDIUM)</li>
 *   <li>recurrence, if given, must be DAILY, WEEKLY, MONTHLY or YEARLY</li>
 * </ul>
 * "Not given" means null or blank for the optional fields.
 */
public class TaskValidator {

    public static final int MAX_TITLE_LENGTH = 200;
    public static final int MAX_DESCRIPTION_LENGTH = 2000;

    /**
     * Validates the request and returns a builder already filled with the cleaned-up values.
     * The caller adds the ID, parent and creation time.
     *
     * @throws ValidationException listing every rule that failed
     */
    public Task.Builder validate(TaskRequest request) {
        if (request == null) {
            throw new ValidationException("request", "must not be null");
        }
        List<FieldIssue> issues = new ArrayList<>();

        String title = checkText(request.getTitle(), "title", MAX_TITLE_LENGTH, issues);
        String description = checkText(request.getDescription(), "description", MAX_DESCRIPTION_LENGTH, issues);

        boolean completed = false;
        String rawCompleted = request.getCompleted();
        if (rawCompleted != null) {
            if (rawCompleted.equals("true")) {
                completed = true;
            } else if (!rawCompleted.equals("false")) {
                issues.add(new FieldIssue("completed", "must be exactly \"true\" or \"false\""));
            }
        }

        String dueDate = null;
        if (isGiven(request.getDueDate())) {
            if (DateHelper.isValidDate(request.getDueDate())) {
                dueDate = request.getDueDate();
            } else {
                issues.add(new FieldIssue("dueDate", "must be a real date in YYYY-MM-DD format"));
            }
        }

        Priority priority = null;
        if (isGiven(request.getPriority())) {
            priority = parsePriority(request.getPriority(), "priority", issues);
        }

        Recurrence recurrence = null;
        if (isGiven(request.getRecurrence())) {
            recurrence = Recurrence.fromName(request.getRecurrence());
            if (recurrence == null) {
                issues.add(new FieldIssue("recurrence", "must be DAILY, WEEKLY, MONTHLY, or YEARLY"));
            }
        }

        if (!issues.isEmpty()) {
            throw new ValidationException(issues);
        }
        return new Task.Builder(title, description, completed)
                .dueDate(dueDate)
                .priority(priority)
                .recurrence(recurrence);
    }

    /**
     * Turns a priority name into a {@link Priority}. Adds an issue and returns null when the
     * name is not exactly LOW, MEDIUM or HIGH.
     */
    public Priority parsePriority(String value, String field, List<FieldIssue> issues) {
        Priority priority = Priority.fromName(value);
        if (priority == null) {
            issues.add(new FieldIssue(field, "must be LOW, MEDIUM, or HIGH"));
        }
        return priority;
    }

    private static String checkText(String value, String field, int maxLength, List<FieldIssue> issues) {
        if (value == null || value.isBlank()) {
            issues.add(new FieldIssue(field, "must not be empty"));
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            issues.add(new FieldIssue(field, "must be at most " + maxLength + " characters"));
            return null;
        }
        return trimmed;
    }

    private static boolean isGiven(String value) {
        return value != null && !value.isBlank();
    }
}
