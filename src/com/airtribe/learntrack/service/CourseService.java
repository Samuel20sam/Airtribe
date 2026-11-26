package com.airtribe.learntrack.service;

import com.airtribe.learntrack.entity.Course;
import com.airtribe.learntrack.exception.EntityNotFoundException;
import com.airtribe.learntrack.exception.InvalidInputException;
import com.airtribe.learntrack.util.IdGenerator;
import com.airtribe.learntrack.util.InputValidator;

import java.util.ArrayList;

/**
 * All the business logic for courses. Data lives in memory in an ArrayList.
 * Callers only ever get copies of the stored courses, so every change has to go through this service.
 */
public class CourseService {

    private final ArrayList<Course> courses = new ArrayList<>();

    public Course addCourse(String courseName, String description, int durationInWeeks)
            throws InvalidInputException {
        String cleanName = InputValidator.requireNonBlank(courseName, "Course name");
        InputValidator.requirePositive(durationInWeeks, "Duration in weeks");
        ensureNameIsUnique(cleanName);

        Course course = new Course(IdGenerator.getNextCourseId(), cleanName,
                InputValidator.trimOrEmpty(description), durationInWeeks);
        courses.add(course);
        return new Course(course);
    }

    // Method overloading: a course without a description.
    public Course addCourse(String courseName, int durationInWeeks) throws InvalidInputException {
        return addCourse(courseName, "", durationInWeeks);
    }

    public Course findCourseById(int id) throws EntityNotFoundException {
        return new Course(findStoredCourse(id));
    }

    /** Returns a new list of course copies: callers can change neither our list nor the courses in it. */
    public ArrayList<Course> listCourses() {
        return listCourses(false);
    }

    // Overloaded: pass true to get only the active courses.
    public ArrayList<Course> listCourses(boolean activeOnly) {
        ArrayList<Course> result = new ArrayList<>();
        for (Course course : courses) {
            if (!activeOnly || course.isActive()) {
                result.add(new Course(course));
            }
        }
        return result;
    }

    public Course activateCourse(int id) throws EntityNotFoundException {
        return setCourseActive(id, true);
    }

    public Course deactivateCourse(int id) throws EntityNotFoundException {
        return setCourseActive(id, false);
    }

    private Course setCourseActive(int id, boolean active) throws EntityNotFoundException {
        Course course = findStoredCourse(id);
        course.setActive(active);
        return new Course(course);
    }

    /** Returns the course object we actually store. Only this class may change it, so it is never handed out. */
    private Course findStoredCourse(int id) throws EntityNotFoundException {
        for (Course course : courses) {
            if (course.getId() == id) {
                return course;
            }
        }
        throw new EntityNotFoundException("Course", id);
    }

    private void ensureNameIsUnique(String courseName) throws InvalidInputException {
        for (Course course : courses) {
            if (course.getCourseName().equalsIgnoreCase(courseName)) {
                throw new InvalidInputException("A course named '" + courseName + "' already exists.");
            }
        }
    }
}
