package com.airtribe.taskmanager.parser;

import com.airtribe.taskmanager.exception.ApiException;
import com.airtribe.taskmanager.exception.BadGatewayException;
import com.airtribe.taskmanager.llm.LlmClient;

/** Shared by the two LLM strategies: calls the client and turns any failure into a 502. */
final class LlmCall {

    private LlmCall() {
    }

    static String complete(LlmClient client, String prompt) {
        String reply;
        try {
            reply = client.complete(prompt);
        } catch (ApiException e) {
            throw e; // already a 502 or 503 with a clear message
        } catch (RuntimeException e) {
            throw new BadGatewayException("The language model call failed: " + e.getMessage());
        }
        if (reply == null || reply.isBlank()) {
            throw new BadGatewayException("The language model returned an empty reply");
        }
        return reply;
    }
}
