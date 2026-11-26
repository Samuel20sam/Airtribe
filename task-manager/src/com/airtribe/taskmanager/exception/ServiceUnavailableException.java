package com.airtribe.taskmanager.exception;

/** 503: an AI feature was called but the service is not configured for it (no LLM_API_KEY). */
public class ServiceUnavailableException extends ApiException {

    private static final long serialVersionUID = 1L;

    public static final int STATUS = 503;
    public static final String CODE = "SERVICE_UNAVAILABLE";

    public ServiceUnavailableException(String message) {
        super(STATUS, CODE, message, null);
    }
}
