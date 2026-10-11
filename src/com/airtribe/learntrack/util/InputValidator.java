package com.airtribe.learntrack.util;

import com.airtribe.learntrack.exception.InvalidInputException;

/**
 * Small, stateless helpers for checking and cleaning text input.
 * Every method is static, so you call them as InputValidator.methodName(...).
 */
public final class InputValidator {

    private InputValidator() {
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** Returns the trimmed text, or throws if it is null/empty. */
    public static String requireNonBlank(String value, String fieldName) throws InvalidInputException {
        if (isBlank(value)) {
            throw new InvalidInputException(fieldName + " must not be empty.");
        }
        return value.trim();
    }

    /** Returns the trimmed text, or "" when the value is null. */
    public static String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /** An email is optional: blank becomes "", anything else must look like name@domain.tld. */
    public static String cleanOptionalEmail(String email) throws InvalidInputException {
        if (isBlank(email)) {
            return "";
        }
        String cleaned = email.trim();
        if (!isValidEmail(cleaned)) {
            throw new InvalidInputException("'" + cleaned + "' is not a valid email address.");
        }
        return cleaned;
    }

    /** Converts text to an int, with a friendly error instead of a raw NumberFormatException. */
    public static int parseInt(String text, String fieldName) throws InvalidInputException {
        if (isBlank(text)) {
            throw new InvalidInputException(fieldName + " is required.");
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new InvalidInputException(fieldName + " must be a whole number, but you entered '" + text.trim() + "'.");
        }
    }

    public static int requirePositive(int value, String fieldName) throws InvalidInputException {
        if (value <= 0) {
            throw new InvalidInputException(fieldName + " must be greater than 0.");
        }
        return value;
    }

    private static boolean isValidEmail(String email) {
        return email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    }
}
