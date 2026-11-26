package com.airtribe.taskmanager.api;

import com.airtribe.taskmanager.util.SimpleJson;

/**
 * What every {@code TaskApi} operation returns: an HTTP-style status code and a JSON-style body
 * (Maps, Lists, Strings, numbers, booleans). If the service is later put behind real HTTP
 * endpoints, the status becomes the HTTP status and {@link #toJson()} becomes the response body.
 */
public final class ApiResponse {

    private final int status;
    private final Object body;

    public ApiResponse(int status, Object body) {
        this.status = status;
        this.body = body;
    }

    public static ApiResponse ok(Object body) {
        return new ApiResponse(200, body);
    }

    public static ApiResponse created(Object body) {
        return new ApiResponse(201, body);
    }

    public int getStatus() {
        return status;
    }

    public Object getBody() {
        return body;
    }

    /** True for 2xx statuses. */
    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }

    /** The body as compact JSON on one line. */
    public String toJson() {
        return SimpleJson.write(body);
    }

    /** The body as indented JSON. */
    public String toPrettyJson() {
        return SimpleJson.writePretty(body);
    }

    @Override
    public String toString() {
        return status + " " + toJson();
    }
}
