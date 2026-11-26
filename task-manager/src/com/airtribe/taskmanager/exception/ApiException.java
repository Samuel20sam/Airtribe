package com.airtribe.taskmanager.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for every error the API reports on purpose. It carries everything needed to build
 * the error response: the HTTP-style status, a machine-readable code, a human message and an
 * optional list of field issues. {@code ErrorResponseFactory} turns it into the error shape.
 *
 * <p>It extends RuntimeException, so service methods do not need {@code throws} clauses and
 * the error can travel up to {@code TaskApi}, which catches it in one place.
 */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;
    private final String code;
    @SuppressWarnings("serial") // exceptions are never serialized in this program
    private final List<FieldIssue> details;

    protected ApiException(int status, String code, String message, List<FieldIssue> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(details));
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public List<FieldIssue> getDetails() {
        return details;
    }
}
