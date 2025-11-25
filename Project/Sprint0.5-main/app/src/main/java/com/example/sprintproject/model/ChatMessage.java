package com.example.sprintproject.model;

import com.google.firebase.Timestamp;

public class ChatMessage {
    private String role;
    private String content;
    private Timestamp timestamp;

    public ChatMessage() {

    }

    public ChatMessage(String role, String content, Timestamp timestamp) {
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getRole() {
        return role;
    }
    public String getContent() { return content; }
    public Timestamp getTimestamp() { return timestamp; }

    public void setRole(String role) { this.role = role; }
    public void setContent(String content) { this.content = content; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
