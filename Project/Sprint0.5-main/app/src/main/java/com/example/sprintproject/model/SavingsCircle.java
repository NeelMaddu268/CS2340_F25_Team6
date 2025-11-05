package com.example.sprintproject.model;
import java.util.List;
import java.util.Map;

public class SavingsCircle {
    private String id;
    private String name;
    private String creatorId;
    private String creatorEmail;
    private List<String> memberIds;
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

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

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

    public void setInvite(String invite) {
        this.invite = invite;
    }

    public String getInvite() {
        return invite;
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
}
