package com.airtribe.taskmanager.parser;

import com.airtribe.taskmanager.llm.AnthropicLlmClient;
import com.airtribe.taskmanager.llm.LlmClient;

/**
 * Factory pattern: the one place that decides which {@link TaskParser} and
 * {@link BreakdownStrategy} the service gets. The service never creates them itself.
 *
 * <ul>
 *   <li>{@link Mode#AUTO}: the LLM versions when an API key is present, the rule-based ones otherwise.</li>
 *   <li>{@link Mode#LLM}: always the LLM versions. Without a key, every AI call answers 503.</li>
 *   <li>{@link Mode#RULE_BASED}: always the offline versions, even when a key is present.</li>
 * </ul>
 * The {@code (LlmClient)} methods plug in any client, for example the fakes used by the tests.
 */
public final class ParserFactory {

    public static final String API_KEY_ENV = "LLM_API_KEY";
    public static final String MODEL_ENV = "LLM_MODEL";

    public enum Mode {
        AUTO,
        LLM,
        RULE_BASED
    }

    private ParserFactory() {
    }

    /** The API key from the environment, or null. The key is never stored in code or files. */
    public static String apiKeyFromEnvironment() {
        return System.getenv(API_KEY_ENV);
    }

    public static boolean hasApiKey(String apiKey) {
        return apiKey != null && !apiKey.isBlank();
    }

    /** The mode AUTO resolves to for this key. */
    public static Mode resolve(Mode mode, String apiKey) {
        if (mode == Mode.AUTO) {
            return hasApiKey(apiKey) ? Mode.LLM : Mode.RULE_BASED;
        }
        return mode;
    }

    public static TaskParser createTaskParser(Mode mode, String apiKey) {
        switch (resolve(mode, apiKey)) {
            case LLM:
                return createTaskParser(defaultClient(apiKey));
            case RULE_BASED:
            default:
                return new RuleBasedTaskParser();
        }
    }

    public static TaskParser createTaskParser(LlmClient client) {
        return new LlmTaskParser(client);
    }

    public static BreakdownStrategy createBreakdownStrategy(Mode mode, String apiKey) {
        switch (resolve(mode, apiKey)) {
            case LLM:
                return createBreakdownStrategy(defaultClient(apiKey));
            case RULE_BASED:
            default:
                return new RuleBasedBreakdownStrategy();
        }
    }

    public static BreakdownStrategy createBreakdownStrategy(LlmClient client) {
        return new LlmBreakdownStrategy(client);
    }

    private static LlmClient defaultClient(String apiKey) {
        return new AnthropicLlmClient(apiKey, System.getenv(MODEL_ENV), AnthropicLlmClient.DEFAULT_TIMEOUT);
    }
}
