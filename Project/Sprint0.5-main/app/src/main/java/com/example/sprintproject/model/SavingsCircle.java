package com.example.sprintproject.model;

import java.util.List;

public class SavingsCircle {
    private final String name;
    private final List<User> members;
    private final int challengeAmount;

    public SavingsCircle(String name, List<User> members, int challengeAmount) {
        this.name = name;
        this.members = members;
        this.challengeAmount = challengeAmount;
    }

    public String getName() {
        return name;
    }

    public List<User> getMembers() {
        return members;
    }

    public int getChallengeAmount() {
        return challengeAmount;
    }
}
