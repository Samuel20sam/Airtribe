package com.airtribe.taskmanager.exception;

/** One problem with one input field, for example field "title", issue "must not be empty". */
public final class FieldIssue {

    private final String field;
    private final String issue;

    public FieldIssue(String field, String issue) {
        this.field = field;
        this.issue = issue;
    }

    public String getField() {
        return field;
    }

    public String getIssue() {
        return issue;
    }

    @Override
    public String toString() {
        return field + ": " + issue;
    }
}
