package com.airtribe.learntrack.ui;

/**
 * Thrown by {@link ConsoleInput} when there is nothing more to read (Ctrl+Z / Ctrl+D,
 * or piped input ran out). {@link Main} catches it and decides how to shut down.
 *
 * Unchecked (extends RuntimeException) on purpose: almost every UI method reads input,
 * and the exception should pass straight up to Main instead of being declared everywhere.
 * Package-private, like ConsoleInput, because only the ui package needs it.
 */
class InputClosedException extends RuntimeException {

    InputClosedException() {
        super("No more input to read.");
    }
}
