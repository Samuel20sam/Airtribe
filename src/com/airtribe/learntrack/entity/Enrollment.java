package com.airtribe.learntrack.entity;

import java.time.LocalDate;

/**
 * Links one student to one course. It stores the two IDs rather than the objects,
 * so the services decide how to look the student and course up.
 */
public class Enrollment {

    private int id;
    private int studentId;
    private int courseId;
    private LocalDate enrollmentDate;
    private EnrollmentStatus status;

    public Enrollment() {
    }

    // Overloaded: a new enrollment starts today and is ACTIVE.
    public Enrollment(int id, int studentId, int courseId) {
        this(id, studentId, courseId, LocalDate.now(), EnrollmentStatus.ACTIVE);
    }

    public Enrollment(int id, int studentId, int courseId, LocalDate enrollmentDate, EnrollmentStatus status) {
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.enrollmentDate = enrollmentDate;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "[Enrollment " + id + "] Student " + studentId + " -> Course " + courseId
                + " | " + enrollmentDate + " | " + status;
    }
}
