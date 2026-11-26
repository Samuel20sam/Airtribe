package com.airtribe.taskmanager.exception;

/** 422: the request conflicts with an earlier one, for example an idempotency key reused for a different request. */
public class ConflictException extends ApiException {

    private static final long serialVersionUID = 1L;

    public static final int STATUS = 422;
    public static final String CODE = "IDEMPOTENCY_CONFLICT";

    public ConflictException(String message) {
        super(STATUS, CODE, message, null);
    }
}
