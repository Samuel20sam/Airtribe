package com.airtribe.learntrack.entity;

/**
 * A trainer. Not used by the menus yet; it exists to show that several classes
 * can extend {@link Person} and each override getDisplayName() in its own way.
 */
public class Trainer extends Person {

    private String expertise;

    public Trainer() {
        super();
    }

    public Trainer(int id, String firstName, String lastName, String email, String expertise) {
        super(id, firstName, lastName, email);
        this.expertise = expertise;
    }

    public String getExpertise() {
        return expertise;
    }

    public void setExpertise(String expertise) {
        this.expertise = expertise;
    }

    @Override
    public String getDisplayName() {
        return super.getDisplayName() + " (Trainer: " + expertise + ")";
    }
}
