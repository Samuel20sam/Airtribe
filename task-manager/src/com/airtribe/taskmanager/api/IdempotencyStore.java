package com.airtribe.taskmanager.api;

import com.airtribe.taskmanager.exception.ConflictException;
import com.airtribe.taskmanager.exception.ValidationException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Remembers the response of each successful request that carried an idempotency key, so a
 * client that retries (because the network failed) never creates a duplicate. Singleton: one
 * shared store for the whole program.
 *
 * <ul>
 *   <li>Same key, same request: the stored response is returned. The operation is not run again,
 *       so nothing is created and the parser is not called.</li>
 *   <li>Same key, different request: 422 IDEMPOTENCY_CONFLICT.</li>
 *   <li>Only successful (2xx) responses are stored, so a client can fix bad input and retry with
 *       the same key.</li>
 * </ul>
 *
 * <p>Thread safety: the maps are ConcurrentHashMaps, and each key has its own lock object, so two
 * threads retrying the same key at the same moment run the operation once; different keys do
 * not block each other.
 */
public final class IdempotencyStore {

    private static final IdempotencyStore INSTANCE = new IdempotencyStore();

    private final Map<String, StoredResponse> responses = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    private IdempotencyStore() {
    }

    public static IdempotencyStore getInstance() {
        return INSTANCE;
    }

    /** What is kept for each key: the request fingerprint and the response sent back. */
    private static final class StoredResponse {
        private final String fingerprint;
        private final ApiResponse response;

        StoredResponse(String fingerprint, ApiResponse response) {
            this.fingerprint = fingerprint;
            this.response = response;
        }
    }

    /**
     * Runs {@code operation} at most once per key.
     *
     * @param key         the client's idempotency key, or null to run without idempotency
     * @param fingerprint the request contents (must include the operation name)
     * @param operation   the real work; may throw an ApiException, which is not stored
     */
    public ApiResponse execute(String key, String fingerprint, Supplier<ApiResponse> operation) {
        if (key == null) {
            return operation.get();
        }
        if (key.isBlank()) {
            throw new ValidationException("idempotencyKey", "must not be blank when given");
        }
        Object lock = locks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            StoredResponse stored = responses.get(key);
            if (stored != null) {
                if (!stored.fingerprint.equals(fingerprint)) {
                    throw new ConflictException("Idempotency key '" + key
                            + "' was already used with a different request");
                }
                return stored.response;
            }
            ApiResponse response = operation.get();
            if (response.isSuccess()) {
                responses.put(key, new StoredResponse(fingerprint, response));
            }
            return response;
        }
    }

    public int size() {
        return responses.size();
    }

    /** Forgets every key. Used by Main sections and the TestRunner. */
    public void clear() {
        responses.clear();
        locks.clear();
    }
}
