package com.airtribe.learntrack.util;

/**
 * Hands out unique IDs. The counters are static, so they belong to the class itself
 * and are shared by the whole application (there is only ever one set of counters).
 */
public final class IdGenerator {

    private static int studentIdCounter = 0;
    private static int courseIdCounter = 0;
    private static int enrollmentIdCounter = 0;

    // Utility class: nobody should create an IdGenerator object.
    private IdGenerator() {
    }

    public static int getNextStudentId() {
        studentIdCounter++;
        return studentIdCounter;
    }

    public static int getNextCourseId() {
        courseIdCounter++;
        return courseIdCounter;
    }

    public static int getNextEnrollmentId() {
        enrollmentIdCounter++;
        return enrollmentIdCounter;
    }
}
