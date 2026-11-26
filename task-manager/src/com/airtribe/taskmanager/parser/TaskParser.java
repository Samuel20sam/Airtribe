package com.airtribe.taskmanager.parser;

/**
 * Strategy pattern: turns a plain-English sentence into a {@link ParsedTask}.
 *
 * <p>The service only knows this interface, so any parser can be plugged in:
 * {@link LlmTaskParser} asks a language model, {@link RuleBasedTaskParser} works offline with
 * String methods. To add another one, implement this method and return it from
 * {@link ParserFactory} (or pass it straight to the {@code TaskService} constructor).
 *
 * <p>Contract for implementations:
 * <ul>
 *   <li>{@code today} is the current date as YYYY-MM-DD; use it to resolve "tomorrow",
 *       "next Friday" or "the 1st".</li>
 *   <li>Return values as you understood them, without validating them; the service validates.</li>
 *   <li>Throw {@code BadGatewayException} (502) if the source fails or its output cannot be read,
 *       and {@code ServiceUnavailableException} (503) if the parser is not configured.</li>
 * </ul>
 */
public interface TaskParser {

    ParsedTask parse(String text, String today);
}
