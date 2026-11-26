package com.airtribe.taskmanager.exception;

import java.util.List;

/**
 * 502: the language model (or another parser / breakdown strategy) failed, timed out, or
 * returned output that does not pass validation. Nothing is saved when this is thrown.
 */
public class BadGatewayException extends ApiException {

    private static final long serialVersionUID = 1L;

    public static final int STATUS = 502;
    public static final String CODE = "BAD_GATEWAY";

    public BadGatewayException(String message) {
        super(STATUS, CODE, message, null);
    }

    public BadGatewayException(String message, List<FieldIssue> details) {
        super(STATUS, CODE, message, details);
    }
}
