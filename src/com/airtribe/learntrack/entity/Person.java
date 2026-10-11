package com.airtribe.learntrack.entity;

/**
 * Base class for every person in LearnTrack (students, trainers, ...).
 * Fields are private; other classes go through the getters and setters (encapsulation).
 */
public class Person {

    private int id;
    private String firstName;
    private String lastName;
    private String email;

    public Person() {
    }

    public Person(int id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /** The name shown to users. Subclasses override this to add their own details. */
    public String getDisplayName() {
        return getFullName();
    }

    // toString() calls getDisplayName(); at run time Java picks the subclass version (polymorphism).
    @Override
    public String toString() {
        return "[ID " + id + "] " + getDisplayName() + " | " + emailOrPlaceholder();
    }

    private String emailOrPlaceholder() {
        return (email == null || email.isEmpty()) ? "(no email)" : email;
    }
}
