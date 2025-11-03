package com.example.sprintproject.model;
import java.util.List;

public class SavingsCircle {
    private String name;
    private List<User> members;
    private int challengeAmount;
    private String email;
    private String invite;
    private String title;
    private double goal;
    private String frequency;
    private String notes;

    public SavingsCircle() {

    }

    public SavingsCircle(String name, List<User> members, int challengeAmount) {
        this.name = name;
        this.members = members;
        this.challengeAmount = challengeAmount;
    }

    public SavingsCircle(String name, String email, String invite, String title, double goal, String frequency, String notes) {
        this.name = name;
        this.email = email;
        this.invite = invite;
        this.title = title;
        this.goal = goal;
        this.frequency = frequency;
        this.notes = notes;
    }

    public SavingsCircle(String name, String email, String invite, String title, double goal, String frequency) {
        this.name = name;
        this.email = email;
        this.invite = invite;
        this.title = title;
        this.goal = goal;
        this.frequency = frequency;
        this.notes = null;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getInvite() {
        return invite;
    }

    public String getTitle() {
        return title;
    }

    public double getGoal() {
        return goal;
    }

    public String getFrequency() {
        return frequency;
    }

    public String getNotes() {
        return notes;
    }

    public List<User> getMembers() {
        return members;
    }

    public int getChallengeAmount() {
        return challengeAmount;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setInvite(String invite) {
        this.invite = invite;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setGoal(double goal) {
        this.goal = goal;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
