package com.airtribe.learntrack.entity;

/**
 * A learner. Inherits id, firstName, lastName and email from {@link Person}
 * and adds the batch and the active flag.
 */
public class Student extends Person {

    private String batch;
    private boolean active;

    public Student() {
        super();
        this.active = true;
    }

    // Constructor overloading: a student can be created without an email...
    public Student(int id, String firstName, String lastName, String batch) {
        this(id, firstName, lastName, "", batch);
    }

    // ...or with one. New students start out active.
    public Student(int id, String firstName, String lastName, String email, String batch) {
        super(id, firstName, lastName, email);
        this.batch = batch;
        this.active = true;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Method overriding: same signature as Person.getDisplayName(), specialised behaviour.
    @Override
    public String getDisplayName() {
        return super.getDisplayName() + " (Batch " + batch + ")";
    }

    @Override
    public String toString() {
        return super.toString() + " | " + (active ? "ACTIVE" : "INACTIVE");
    }
}
