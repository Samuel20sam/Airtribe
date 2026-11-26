package com.airtribe.taskmanager.test;

import com.airtribe.taskmanager.llm.LlmClient;

/** A model that misbehaves in one chosen way, to prove bad output never gets saved. */
public class FaultyLlmClient implements LlmClient {

    public enum Fault {
        /** Valid JSON, but the title is empty. */
        EMPTY_TITLE,
        /** Valid JSON, but the due date is 2026-02-30. */
        IMPOSSIBLE_DATE,
        /** Valid JSON, but the title is a number instead of a string. */
        WRONG_TYPE,
        /** Not JSON at all. */
        MALFORMED,
        /** An empty reply. */
        EMPTY_REPLY,
        /** Throws, like a network error or a timeout. */
        THROWS,
        /** A breakdown reply where the second line is not title|description. */
        BAD_BREAKDOWN_LINE,
        /** A breakdown reply where the second subtask has an empty title. */
        BREAKDOWN_EMPTY_TITLE
    }

    private final Fault fault;
    private int calls;

    public FaultyLlmClient(Fault fault) {
        this.fault = fault;
    }

    @Override
    public synchronized String complete(String prompt) {
        calls++;
        switch (fault) {
            case EMPTY_TITLE:
                return "{\"title\": \"\", \"description\": \"Something\", \"dueDate\": null, \"recurrence\": null}";
            case IMPOSSIBLE_DATE:
                return "{\"title\": \"Pay rent\", \"description\": \"Monthly rent\", \"dueDate\": \"2026-02-30\", \"recurrence\": null}";
            case WRONG_TYPE:
                return "{\"title\": 42, \"description\": \"Something\", \"dueDate\": null, \"recurrence\": null}";
            case MALFORMED:
                return "Sure! Here is your task: title = Pay rent";
            case EMPTY_REPLY:
                return "";
            case THROWS:
                throw new RuntimeException("simulated timeout");
            case BAD_BREAKDOWN_LINE:
                return "Book venue|Find and book a place\nSend invites to everyone\nOrder food|Order catering";
            case BREAKDOWN_EMPTY_TITLE:
                return "Book venue|Find and book a place\n|Send the invites\nOrder food|Order catering";
            default:
                throw new IllegalStateException("unknown fault " + fault);
        }
    }

    public synchronized int getCalls() {
        return calls;
    }
}
