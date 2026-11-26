package com.airtribe.learntrack.service;

import com.airtribe.learntrack.entity.Course;
import com.airtribe.learntrack.entity.Enrollment;
import com.airtribe.learntrack.entity.EnrollmentStatus;
import com.airtribe.learntrack.entity.Student;
import com.airtribe.learntrack.exception.EntityNotFoundException;
import com.airtribe.learntrack.exception.InvalidInputException;
import com.airtribe.learntrack.util.IdGenerator;

import java.util.ArrayList;

/**
 * Business logic for enrollments. It needs the student and course services
 * to check that both sides of an enrollment really exist.
 * Callers only ever get copies of the stored enrollments, so status changes must go through this service.
 */
public class EnrollmentService {

    private final ArrayList<Enrollment> enrollments = new ArrayList<>();
    private final StudentService studentService;
    private final CourseService courseService;

    public EnrollmentService(StudentService studentService, CourseService courseService) {
        this.studentService = studentService;
        this.courseService = courseService;
    }

    public Enrollment enrollStudent(int studentId, int courseId)
            throws EntityNotFoundException, InvalidInputException {
        Student student = studentService.findStudentById(studentId);
        Course course = courseService.findCourseById(courseId);

        if (!student.isActive()) {
            throw new InvalidInputException("Student " + studentId + " is inactive and cannot be enrolled.");
        }
        if (!course.isActive()) {
            throw new InvalidInputException("Course " + courseId + " is inactive and cannot take new students.");
        }
        if (hasActiveEnrollment(studentId, courseId)) {
            throw new InvalidInputException("Student " + studentId + " is already enrolled in course "
                    + courseId + ".");
        }

        Enrollment enrollment = new Enrollment(IdGenerator.getNextEnrollmentId(), studentId, courseId);
        enrollments.add(enrollment);
        return new Enrollment(enrollment);
    }

    public Enrollment findEnrollmentById(int id) throws EntityNotFoundException {
        return new Enrollment(findStoredEnrollment(id));
    }

    /** Returns a new list of enrollment copies: callers can change neither our list nor the enrollments in it. */
    public ArrayList<Enrollment> listEnrollments() {
        ArrayList<Enrollment> result = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            result.add(new Enrollment(enrollment));
        }
        return result;
    }

    /** Throws EntityNotFoundException if the student does not exist; returns an empty list if they have none. */
    public ArrayList<Enrollment> listEnrollmentsForStudent(int studentId) throws EntityNotFoundException {
        studentService.findStudentById(studentId);

        ArrayList<Enrollment> result = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            if (enrollment.getStudentId() == studentId) {
                result.add(new Enrollment(enrollment));
            }
        }
        return result;
    }

    /**
     * Permanently deletes a student, but only when no enrollment (of any status) refers to them,
     * so an enrollment can never point at a student that no longer exists. It lives here, not in
     * StudentService, because this is the service that knows about enrollments.
     */
    public void removeStudent(int studentId) throws EntityNotFoundException, InvalidInputException {
        if (hasAnyEnrollment(studentId)) {
            throw new InvalidInputException("Student " + studentId
                    + " has enrollments and cannot be removed. Deactivate the student instead.");
        }
        studentService.removeStudent(studentId);
    }

    public Enrollment markCompleted(int enrollmentId) throws EntityNotFoundException, InvalidInputException {
        return changeStatus(enrollmentId, EnrollmentStatus.COMPLETED);
    }

    public Enrollment markCancelled(int enrollmentId) throws EntityNotFoundException, InvalidInputException {
        return changeStatus(enrollmentId, EnrollmentStatus.CANCELLED);
    }

    /** Only an ACTIVE enrollment can move on to COMPLETED or CANCELLED. */
    private Enrollment changeStatus(int enrollmentId, EnrollmentStatus newStatus)
            throws EntityNotFoundException, InvalidInputException {
        Enrollment enrollment = findStoredEnrollment(enrollmentId);
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new InvalidInputException("Enrollment " + enrollmentId + " is already "
                    + enrollment.getStatus() + " and cannot be changed.");
        }
        enrollment.setStatus(newStatus);
        return new Enrollment(enrollment);
    }

    /** Returns the enrollment object we actually store. Only this class may change it, so it is never handed out. */
    private Enrollment findStoredEnrollment(int id) throws EntityNotFoundException {
        for (Enrollment enrollment : enrollments) {
            if (enrollment.getId() == id) {
                return enrollment;
            }
        }
        throw new EntityNotFoundException("Enrollment", id);
    }

    private boolean hasAnyEnrollment(int studentId) {
        for (Enrollment enrollment : enrollments) {
            if (enrollment.getStudentId() == studentId) {
                return true;
            }
        }
        return false;
    }

    private boolean hasActiveEnrollment(int studentId, int courseId) {
        for (Enrollment enrollment : enrollments) {
            boolean sameStudentAndCourse = enrollment.getStudentId() == studentId
                    && enrollment.getCourseId() == courseId;
            if (sameStudentAndCourse && enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
                return true;
            }
        }
        return false;
    }
}
