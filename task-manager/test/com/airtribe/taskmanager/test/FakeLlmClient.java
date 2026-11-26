package com.airtribe.taskmanager.test;

import com.airtribe.taskmanager.llm.LlmClient;

/**
 * Stands in for the real model: returns fixed replies, never touches the network, and counts how
 * many times it was called so tests can prove a call did (or did not) happen.
 * With several replies, call 1 gets the first, call 2 the second, and so on; the last one repeats.
 */
public class FakeLlmClient implements LlmClient {

    private final String[] replies;
    private int calls;
    private String lastPrompt;

    public FakeLlmClient(String... replies) {
        if (replies.length == 0) {
            throw new IllegalArgumentException("give at least one reply");
        }
        this.replies = replies.clone();
    }

    @Override
    public synchronized String complete(String prompt) {
        lastPrompt = prompt;
        String reply = replies[Math.min(calls, replies.length - 1)];
        calls++;
        return reply;
    }

    public synchronized int getCalls() {
        return calls;
    }

    public synchronized String getLastPrompt() {
        return lastPrompt;
    }
}
