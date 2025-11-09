package com.example.sprintproject.model;
import java.util.List;
import java.util.Map;

public class SavingsCircle {
    private String id;
    private String name;
    private String creatorId;
    private String creatorEmail;
    private AppDate creatorDateJoined;
    private Map<String, String> datesJoined;
    private List<String> memberIds;
    private List<String> memberEmails;
    private Map<String, Double> contributions;
    private double spent;
    private String invite;
    private String title;
    private double goal;
    private String frequency;
    private String notes;

    public SavingsCircle() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorEmail() {
        return creatorEmail;
    }

    public void setCreatorEmail(String creatorEmail) {
        this.creatorEmail = creatorEmail;
    }

    public AppDate getCreatorDateJoined() {
        return creatorDateJoined;
    }

    public void setCreatorDateJoined(AppDate creatorDateJoined) {
        this.creatorDateJoined = creatorDateJoined;
    }

    public Map<String, String> getDatesJoined() {
        return datesJoined;
    }

    public void setDatesJoined(Map<String, String> datesJoined) {
        this.datesJoined = datesJoined; }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    public List<String> getMemberEmails() {
        return memberEmails; }

    public void setMemberEmails(List<String> memberEmails) {
        this.memberEmails = memberEmails; }

    public Map<String, Double> getContributions() {
        return contributions;
    }

    public void setContributions(Map<String, Double> contributions) {
        this.contributions = contributions;
    }

    public double getSpent() {
        return spent;
    }

    public void setSpent(double spent) {
        this.spent = spent;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getGoal() {
        return goal;
    }

    public void setGoal(double goal) {
        this.goal = goal;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getInvite() {
        return invite;
    }

    public void setInvite(String invite) {
        this.invite = invite;
    }
}
