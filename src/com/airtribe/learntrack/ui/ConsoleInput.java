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

    /** Shows the prompt and returns what the user typed, trimmed. */
    String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            // Input ended (e.g. Ctrl+Z / Ctrl+D or piped input ran out): leave cleanly instead of crashing.
            System.out.println();
            System.out.println("Input closed. Goodbye!");
            System.exit(0);
        }
        return scanner.nextLine().trim();
    }

    /** Reads a whole number; throws InvalidInputException if the user typed something else. */
    int readInt(String prompt, String fieldName) throws InvalidInputException {
        return InputValidator.parseInt(readLine(prompt), fieldName);
    }
}
