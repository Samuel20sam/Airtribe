package com.airtribe.learntrack.exception;

/**
 * Thrown when user input (or a business rule) is not acceptable,
 * e.g. an empty name, a badly formatted email, or enrolling an inactive student.
 */
public class InvalidInputException extends Exception {

    public InvalidInputException(String message) {
        super(message);
    }
}
