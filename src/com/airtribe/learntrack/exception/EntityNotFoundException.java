package com.airtribe.learntrack.exception;

/**
 * Thrown when a Student, Course or Enrollment with the requested ID does not exist.
 * It is a checked exception, so callers are forced to handle it (try-catch) or declare it (throws).
 */
public class EntityNotFoundException extends Exception {

    public EntityNotFoundException(String message) {
        super(message);
    }

    // Constructor overloading: builds a standard message from the entity name and ID.
    public EntityNotFoundException(String entityName, int id) {
        super(entityName + " with ID " + id + " was not found.");
    }
}
