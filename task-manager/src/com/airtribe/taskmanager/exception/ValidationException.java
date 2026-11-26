package com.airtribe.taskmanager.exception;

import java.util.List;

/** 400: the client's input breaks one or more rules. Lists every problem found, not just the first. */
public class ValidationException extends ApiException {

    private static final long serialVersionUID = 1L;

    public static final int STATUS = 400;
    public static final String CODE = "VALIDATION_ERROR";

    public ValidationException(List<FieldIssue> issues) {
        super(STATUS, CODE, "Validation failed", issues);
    }

    public ValidationException(String field, String issue) {
        this(List.of(new FieldIssue(field, issue)));
    }
}
