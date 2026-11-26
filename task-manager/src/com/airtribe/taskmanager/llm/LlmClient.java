package com.airtribe.taskmanager.llm;

/**
 * Sends one prompt to a language model and returns its text reply.
 *
 * <p>An interface, so the parsers never depend on a particular provider or on the network.
 * The real implementation is {@link AnthropicLlmClient}; tests plug in fakes that return a fixed
 * reply or fail on purpose.
 *
 * <p>Implementations should throw {@code ServiceUnavailableException} when they are not
 * configured (no API key) and {@code BadGatewayException} when the call fails or times out.
 * Any other RuntimeException is also treated as a failed call (502) by the callers.
 */
public interface LlmClient {

    String complete(String prompt);
}
