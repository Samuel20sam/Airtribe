package com.airtribe.taskmanager.api;

import com.airtribe.taskmanager.exception.ApiException;
import com.airtribe.taskmanager.exception.FieldIssue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory pattern: the only place that builds the error shape, so every error looks the same:
 * <pre>
 * {"error": {"code": "VALIDATION_ERROR", "message": "Validation failed",
 *            "details": [{"field": "title", "issue": "must not be empty"}]}}
 * </pre>
 * {@code code} and {@code message} are always strings; {@code details} is always a list
 * (empty when there is nothing to add).
 */
public final class ErrorResponseFactory {

    public static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";

    private ErrorResponseFactory() {
    }

    public static ApiResponse from(ApiException e) {
        return new ApiResponse(e.getStatus(), errorBody(e.getCode(), e.getMessage(), e.getDetails()));
    }

    /**
     * 500 for anything unexpected. The exception's own message is not sent to the client, because
     * it may expose internal details; it is logged instead.
     */
    public static ApiResponse internalError(Exception e) {
        System.err.println("Unexpected error: " + e);
        return new ApiResponse(500, errorBody(INTERNAL_ERROR_CODE, "An unexpected error occurred", List.of()));
    }

    private static Map<String, Object> errorBody(String code, String message, List<FieldIssue> issues) {
        List<Object> details = new ArrayList<>();
        for (FieldIssue issue : issues) {
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("field", String.valueOf(issue.getField()));
            detail.put("issue", String.valueOf(issue.getIssue()));
            details.add(detail);
        }
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", String.valueOf(code));
        error.put("message", String.valueOf(message));
        error.put("details", details);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        return body;
    }
}
