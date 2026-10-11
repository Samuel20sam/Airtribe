package com.airtribe.learntrack.service;

import com.airtribe.learntrack.entity.Student;
import com.airtribe.learntrack.exception.EntityNotFoundException;
import com.airtribe.learntrack.exception.InvalidInputException;
import com.airtribe.learntrack.util.IdGenerator;
import com.airtribe.learntrack.util.InputValidator;

import java.util.ArrayList;

/** All the business logic for students. Data lives in memory in an ArrayList. */
public class StudentService {

    private static final int NO_ID = -1;

    private final ArrayList<Student> students = new ArrayList<>();

    /** Adds a student with an email. All input is validated before an ID is used up. */
    public Student addStudent(String firstName, String lastName, String email, String batch)
            throws InvalidInputException {
        String cleanFirstName = InputValidator.requireNonBlank(firstName, "First name");
        String cleanLastName = InputValidator.requireNonBlank(lastName, "Last name");
        String cleanBatch = InputValidator.requireNonBlank(batch, "Batch");
        String cleanEmail = InputValidator.cleanOptionalEmail(email);
        ensureEmailIsUnique(cleanEmail, NO_ID);

        Student student = new Student(IdGenerator.getNextStudentId(), cleanFirstName, cleanLastName,
                cleanEmail, cleanBatch);
        students.add(student);
        return student;
    }

    // Method overloading: same name, different parameters. Adds a student without an email.
    public Student addStudent(String firstName, String lastName, String batch) throws InvalidInputException {
        return addStudent(firstName, lastName, "", batch);
    }

    public Student findStudentById(int id) throws EntityNotFoundException {
        for (Student student : students) {
            if (student.getId() == id) {
                return student;
            }
        }
        throw new EntityNotFoundException("Student", id);
    }

    /** Returns a copy, so callers cannot change our internal list by accident. */
    public ArrayList<Student> listStudents() {
        return new ArrayList<>(students);
    }

    // Overloaded: pass true to get only the active students.
    public ArrayList<Student> listStudents(boolean activeOnly) {
        if (!activeOnly) {
            return listStudents();
        }
        ArrayList<Student> activeStudents = new ArrayList<>();
        for (Student student : students) {
            if (student.isActive()) {
                activeStudents.add(student);
            }
        }
        return activeStudents;
    }

    /**
     * Updates a student. A null/blank argument means "keep the current value".
     * The only check that can fail (email) runs first, so a failed update changes nothing.
     */
    public Student updateStudent(int id, String firstName, String lastName, String email, String batch)
            throws EntityNotFoundException, InvalidInputException {
        Student student = findStudentById(id);

        if (!InputValidator.isBlank(email)) {
            String cleanEmail = InputValidator.cleanOptionalEmail(email);
            ensureEmailIsUnique(cleanEmail, id);
            student.setEmail(cleanEmail);
        }
        if (!InputValidator.isBlank(firstName)) {
            student.setFirstName(firstName.trim());
        }
        if (!InputValidator.isBlank(lastName)) {
            student.setLastName(lastName.trim());
        }
        if (!InputValidator.isBlank(batch)) {
            student.setBatch(batch.trim());
        }
        return student;
    }

    /**
     * Permanently deletes a student. The menu does not use this: it deactivates students instead,
     * so their enrollments still point at a student that exists.
     */
    public void removeStudent(int id) throws EntityNotFoundException {
        Student student = findStudentById(id);
        students.remove(student);
    }

    public Student deactivateStudent(int id) throws EntityNotFoundException {
        return setStudentActive(id, false);
    }

    public Student activateStudent(int id) throws EntityNotFoundException {
        return setStudentActive(id, true);
    }

    private Student setStudentActive(int id, boolean active) throws EntityNotFoundException {
        Student student = findStudentById(id);
        student.setActive(active);
        return student;
    }

    private void ensureEmailIsUnique(String email, int ignoredStudentId) throws InvalidInputException {
        if (email.isEmpty()) {
            return;
        }
        for (Student student : students) {
            boolean sameEmail = email.equalsIgnoreCase(student.getEmail());
            if (sameEmail && student.getId() != ignoredStudentId) {
                throw new InvalidInputException("Another student already uses the email " + email + ".");
            }
        }
    }
}
