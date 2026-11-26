package com.airtribe.learntrack.ui;

import com.airtribe.learntrack.exception.InvalidInputException;
import com.airtribe.learntrack.util.InputValidator;

import java.util.Scanner;

/**
 * Reads typed input for {@link Main}. No "public" keyword, so this class has default
 * (package-private) access: only classes in the ui package can use it.
 */
class ConsoleInput {

    private final Scanner scanner = new Scanner(System.in);

    /**
     * Shows the prompt and returns what the user typed, trimmed.
     * Throws InputClosedException when input has ended; the caller decides what to do about it.
     */
    String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            throw new InputClosedException();
        }
        return scanner.nextLine().trim();
    }

    /** Reads a whole number; throws InvalidInputException if the user typed something else. */
    int readInt(String prompt, String fieldName) throws InvalidInputException {
        return InputValidator.parseInt(readLine(prompt), fieldName);
    }
}
