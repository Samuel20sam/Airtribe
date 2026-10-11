package com.airtribe.learntrack.ui;

import com.airtribe.learntrack.entity.Course;
import com.airtribe.learntrack.entity.Enrollment;
import com.airtribe.learntrack.entity.Student;
import com.airtribe.learntrack.exception.EntityNotFoundException;
import com.airtribe.learntrack.exception.InvalidInputException;
import com.airtribe.learntrack.service.CourseService;
import com.airtribe.learntrack.service.EnrollmentService;
import com.airtribe.learntrack.service.StudentService;

import java.util.ArrayList;

/**
 * Console UI. Main only shows menus, reads input and calls the services;
 * the rules (validation, "is the student active?", ...) live in the service classes.
 */
public class Main {

    private static final String DIVIDER = "----------------------------------------";

    private final ConsoleInput input = new ConsoleInput();
    private final StudentService studentService = new StudentService();
    private final CourseService courseService = new CourseService();
    private final EnrollmentService enrollmentService = new EnrollmentService(studentService, courseService);

    public static void main(String[] args) {
        new Main().run();
    }

    // ---------------------------------------------------------------- main menu

    private void run() {
        System.out.println("=== LearnTrack: Student & Course Management ===");
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = input.readLine("Choose an option: ");
            switch (choice) {
                case "1":
                    runStudentMenu();
                    break;
                case "2":
                    runCourseMenu();
                    break;
                case "3":
                    runEnrollmentMenu();
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    printOptionNotFound(choice);
            }
        }
        System.out.println("Goodbye!");
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("MAIN MENU");
        System.out.println("  1. Student Management");
        System.out.println("  2. Course Management");
        System.out.println("  3. Enrollment Management");
        System.out.println("  0. Exit");
        System.out.println(DIVIDER);
    }

    // ------------------------------------------------------------ student menu

    private void runStudentMenu() {
        boolean inMenu = true;
        while (inMenu) {
            printStudentMenu();
            String choice = input.readLine("Choose an option: ");
            try {
                switch (choice) {
                    case "1":
                        addStudent();
                        break;
                    case "2":
                        viewAllStudents();
                        break;
                    case "3":
                        searchStudentById();
                        break;
                    case "4":
                        updateStudent();
                        break;
                    case "5":
                        deactivateStudent();
                        break;
                    case "6":
                        reactivateStudent();
                        break;
                    case "0":
                        inMenu = false;
                        break;
                    default:
                        printOptionNotFound(choice);
                }
            } catch (InvalidInputException | EntityNotFoundException e) {
                printError(e);
            }
        }
    }

    private void printStudentMenu() {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("STUDENT MANAGEMENT");
        System.out.println("  1. Add new student");
        System.out.println("  2. View all students");
        System.out.println("  3. Search student by ID");
        System.out.println("  4. Update student");
        System.out.println("  5. Deactivate student");
        System.out.println("  6. Reactivate student");
        System.out.println("  0. Back to main menu");
        System.out.println(DIVIDER);
    }

    private void addStudent() throws InvalidInputException {
        String firstName = input.readLine("First name: ");
        String lastName = input.readLine("Last name: ");
        String email = input.readLine("Email (press Enter to skip): ");
        String batch = input.readLine("Batch: ");

        // Two overloaded versions of addStudent: with and without an email.
        Student student = email.isEmpty()
                ? studentService.addStudent(firstName, lastName, batch)
                : studentService.addStudent(firstName, lastName, email, batch);
        System.out.println("Student added: " + student);
    }

    private void viewAllStudents() {
        ArrayList<Student> students = studentService.listStudents();
        if (students.isEmpty()) {
            System.out.println("No students yet. Add one from the student menu.");
            return;
        }
        for (Student student : students) {
            System.out.println(student);
        }
    }

    private void searchStudentById() throws InvalidInputException, EntityNotFoundException {
        int id = input.readInt("Student ID: ", "Student ID");
        System.out.println(studentService.findStudentById(id));
    }

    private void updateStudent() throws InvalidInputException, EntityNotFoundException {
        int id = input.readInt("Student ID to update: ", "Student ID");
        System.out.println("Current: " + studentService.findStudentById(id));
        System.out.println("Press Enter to keep the current value.");

        String firstName = input.readLine("New first name: ");
        String lastName = input.readLine("New last name: ");
        String email = input.readLine("New email: ");
        String batch = input.readLine("New batch: ");

        Student updated = studentService.updateStudent(id, firstName, lastName, email, batch);
        System.out.println("Student updated: " + updated);
    }

    private void deactivateStudent() throws InvalidInputException, EntityNotFoundException {
        int id = input.readInt("Student ID to deactivate: ", "Student ID");
        System.out.println("Student deactivated: " + studentService.deactivateStudent(id));
    }

    private void reactivateStudent() throws InvalidInputException, EntityNotFoundException {
        int id = input.readInt("Student ID to reactivate: ", "Student ID");
        System.out.println("Student reactivated: " + studentService.activateStudent(id));
    }

    // ------------------------------------------------------------- course menu

    private void runCourseMenu() {
        boolean inMenu = true;
        while (inMenu) {
            printCourseMenu();
            String choice = input.readLine("Choose an option: ");
            try {
                switch (choice) {
                    case "1":
                        addCourse();
                        break;
                    case "2":
                        viewAllCourses();
                        break;
                    case "3":
                        activateCourse();
                        break;
                    case "4":
                        deactivateCourse();
                        break;
                    case "0":
                        inMenu = false;
                        break;
                    default:
                        printOptionNotFound(choice);
                }
            } catch (InvalidInputException | EntityNotFoundException e) {
                printError(e);
            }
        }
    }

    private void printCourseMenu() {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("COURSE MANAGEMENT");
        System.out.println("  1. Add new course");
        System.out.println("  2. View all courses");
        System.out.println("  3. Activate course");
        System.out.println("  4. Deactivate course");
        System.out.println("  0. Back to main menu");
        System.out.println(DIVIDER);
    }

    private void addCourse() throws InvalidInputException {
        String courseName = input.readLine("Course name: ");
        String description = input.readLine("Description (press Enter to skip): ");
        int durationInWeeks = input.readInt("Duration in weeks: ", "Duration");

        // Two overloaded versions of addCourse: with and without a description.
        Course course = description.isEmpty()
                ? courseService.addCourse(courseName, durationInWeeks)
                : courseService.addCourse(courseName, description, durationInWeeks);
        System.out.println("Course added: " + course);
    }

    private void viewAllCourses() {
        ArrayList<Course> courses = courseService.listCourses();
        if (courses.isEmpty()) {
            System.out.println("No courses yet. Add one from the course menu.");
            return;
        }
        for (Course course : courses) {
            System.out.println(course);
        }
    }

    private void activateCourse() throws InvalidInputException, EntityNotFoundException {
        int id = input.readInt("Course ID to activate: ", "Course ID");
        System.out.println("Course activated: " + courseService.activateCourse(id));
    }

    private void deactivateCourse() throws InvalidInputException, EntityNotFoundException {
        int id = input.readInt("Course ID to deactivate: ", "Course ID");
        System.out.println("Course deactivated: " + courseService.deactivateCourse(id));
    }

    // --------------------------------------------------------- enrollment menu

    private void runEnrollmentMenu() {
        boolean inMenu = true;
        while (inMenu) {
            printEnrollmentMenu();
            String choice = input.readLine("Choose an option: ");
            try {
                switch (choice) {
                    case "1":
                        enrollStudent();
                        break;
                    case "2":
                        viewEnrollmentsForStudent();
                        break;
                    case "3":
                        markEnrollmentCompleted();
                        break;
                    case "4":
                        markEnrollmentCancelled();
                        break;
                    case "5":
                        viewAllEnrollments();
                        break;
                    case "0":
                        inMenu = false;
                        break;
                    default:
                        printOptionNotFound(choice);
                }
            } catch (InvalidInputException | EntityNotFoundException e) {
                printError(e);
            }
        }
    }

    private void printEnrollmentMenu() {
        System.out.println();
        System.out.println(DIVIDER);
        System.out.println("ENROLLMENT MANAGEMENT");
        System.out.println("  1. Enroll a student in a course");
        System.out.println("  2. View enrollments for a student");
        System.out.println("  3. Mark enrollment as completed");
        System.out.println("  4. Mark enrollment as cancelled");
        System.out.println("  5. View all enrollments");
        System.out.println("  0. Back to main menu");
        System.out.println(DIVIDER);
    }

    private void enrollStudent() throws InvalidInputException, EntityNotFoundException {
        int studentId = input.readInt("Student ID: ", "Student ID");
        int courseId = input.readInt("Course ID: ", "Course ID");
        Enrollment enrollment = enrollmentService.enrollStudent(studentId, courseId);
        System.out.println("Enrolled: " + enrollment);
    }

    private void viewEnrollmentsForStudent() throws InvalidInputException, EntityNotFoundException {
        int studentId = input.readInt("Student ID: ", "Student ID");
        Student student = studentService.findStudentById(studentId);
        ArrayList<Enrollment> enrollments = enrollmentService.listEnrollmentsForStudent(studentId);

        if (enrollments.isEmpty()) {
            System.out.println(student.getFullName() + " has no enrollments yet.");
            return;
        }
        System.out.println("Enrollments for " + student.getDisplayName() + ":");
        for (Enrollment enrollment : enrollments) {
            Course course = courseService.findCourseById(enrollment.getCourseId());
            System.out.println("  [Enrollment " + enrollment.getId() + "] " + course.getCourseName()
                    + " | " + enrollment.getEnrollmentDate() + " | " + enrollment.getStatus());
        }
    }

    private void markEnrollmentCompleted() throws InvalidInputException, EntityNotFoundException {
        int enrollmentId = input.readInt("Enrollment ID: ", "Enrollment ID");
        System.out.println("Updated: " + enrollmentService.markCompleted(enrollmentId));
    }

    private void markEnrollmentCancelled() throws InvalidInputException, EntityNotFoundException {
        int enrollmentId = input.readInt("Enrollment ID: ", "Enrollment ID");
        System.out.println("Updated: " + enrollmentService.markCancelled(enrollmentId));
    }

    private void viewAllEnrollments() {
        ArrayList<Enrollment> enrollments = enrollmentService.listEnrollments();
        if (enrollments.isEmpty()) {
            System.out.println("No enrollments yet.");
            return;
        }
        for (Enrollment enrollment : enrollments) {
            System.out.println(enrollment);
        }
    }

    // ----------------------------------------------------------------- helpers

    private void printOptionNotFound(String choice) {
        System.out.println("Option '" + choice + "' not found. Please pick a number from the menu.");
    }

    private void printError(Exception e) {
        System.out.println("Error: " + e.getMessage());
    }
}
