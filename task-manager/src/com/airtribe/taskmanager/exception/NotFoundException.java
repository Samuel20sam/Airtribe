package com.airtribe.taskmanager.exception;

/** 404: no task has the requested ID. */
public class NotFoundException extends ApiException {

    private static final long serialVersionUID = 1L;

    public static final int STATUS = 404;
    public static final String CODE = "NOT_FOUND";

    public NotFoundException(String message) {
        super(STATUS, CODE, message, null);
    }

    public static NotFoundException task(int id) {
        return new NotFoundException("Task " + id + " not found");
    }
}
