package com.airtribe.learntrack.service;

import com.airtribe.learntrack.entity.Student;
import com.airtribe.learntrack.exception.EntityNotFoundException;
import com.airtribe.learntrack.exception.InvalidInputException;
import com.airtribe.learntrack.util.IdGenerator;
import com.airtribe.learntrack.util.InputValidator;

import java.util.ArrayList;

/**
 * All the business logic for students. Data lives in memory in an ArrayList.
 * Callers only ever get copies of the stored students, so every change has to go through this service.
 */
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
        return new Student(student);
    }

    // Method overloading: same name, different parameters. Adds a student without an email.
    public Student addStudent(String firstName, String lastName, String batch) throws InvalidInputException {
        return addStudent(firstName, lastName, "", batch);
    }

    public Student findStudentById(int id) throws EntityNotFoundException {
        return new Student(findStoredStudent(id));
    }

    /** Returns a new list of student copies: callers can change neither our list nor the students in it. */
    public ArrayList<Student> listStudents() {
        return listStudents(false);
    }

    // Overloaded: pass true to get only the active students.
    public ArrayList<Student> listStudents(boolean activeOnly) {
        ArrayList<Student> result = new ArrayList<>();
        for (Student student : students) {
            if (!activeOnly || student.isActive()) {
                result.add(new Student(student));
            }
        }
        return result;
    }

    /**
     * Updates a student. A null/blank argument means "keep the current value".
     * The only check that can fail (email) runs first, so a failed update changes nothing.
     */
    public Student updateStudent(int id, String firstName, String lastName, String email, String batch)
            throws EntityNotFoundException, InvalidInputException {
        Student student = findStoredStudent(id);

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
        return new Student(student);
    }

    /**
     * Permanently deletes a student. Package-private on purpose: this class cannot see enrollments,
     * so code outside the service package must call EnrollmentService.removeStudent, which refuses
     * while any enrollment still refers to the student. The menu deactivates students instead.
     */
    void removeStudent(int id) throws EntityNotFoundException {
        students.remove(findStoredStudent(id));
    }

    public Student deactivateStudent(int id) throws EntityNotFoundException {
        return setStudentActive(id, false);
    }

    public Student activateStudent(int id) throws EntityNotFoundException {
        return setStudentActive(id, true);
    }

    private Student setStudentActive(int id, boolean active) throws EntityNotFoundException {
        Student student = findStoredStudent(id);
        student.setActive(active);
        return new Student(student);
    }

    /** Returns the student object we actually store. Only this class may change it, so it is never handed out. */
    private Student findStoredStudent(int id) throws EntityNotFoundException {
        for (Student student : students) {
            if (student.getId() == id) {
                return student;
            }
        }
        throw new EntityNotFoundException("Student", id);
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
