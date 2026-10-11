package com.airtribe.learntrack.service;

import com.airtribe.learntrack.entity.Course;
import com.airtribe.learntrack.exception.EntityNotFoundException;
import com.airtribe.learntrack.exception.InvalidInputException;
import com.airtribe.learntrack.util.IdGenerator;
import com.airtribe.learntrack.util.InputValidator;

import java.util.ArrayList;

/** All the business logic for courses. Data lives in memory in an ArrayList. */
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
        return course;
    }

    // Method overloading: a course without a description.
    public Course addCourse(String courseName, int durationInWeeks) throws InvalidInputException {
        return addCourse(courseName, "", durationInWeeks);
    }

    public Course findCourseById(int id) throws EntityNotFoundException {
        for (Course course : courses) {
            if (course.getId() == id) {
                return course;
            }
        }
        throw new EntityNotFoundException("Course", id);
    }

    /** Returns a copy, so callers cannot change our internal list by accident. */
    public ArrayList<Course> listCourses() {
        return new ArrayList<>(courses);
    }

    // Overloaded: pass true to get only the active courses.
    public ArrayList<Course> listCourses(boolean activeOnly) {
        if (!activeOnly) {
            return listCourses();
        }
        ArrayList<Course> activeCourses = new ArrayList<>();
        for (Course course : courses) {
            if (course.isActive()) {
                activeCourses.add(course);
            }
        }
        return activeCourses;
    }

    public Course activateCourse(int id) throws EntityNotFoundException {
        return setCourseActive(id, true);
    }

    public Course deactivateCourse(int id) throws EntityNotFoundException {
        return setCourseActive(id, false);
    }

    private Course setCourseActive(int id, boolean active) throws EntityNotFoundException {
        Course course = findCourseById(id);
        course.setActive(active);
        return course;
    }

    private void ensureNameIsUnique(String courseName) throws InvalidInputException {
        for (Course course : courses) {
            if (course.getCourseName().equalsIgnoreCase(courseName)) {
                throw new InvalidInputException("A course named '" + courseName + "' already exists.");
            }
        }
    }
}
