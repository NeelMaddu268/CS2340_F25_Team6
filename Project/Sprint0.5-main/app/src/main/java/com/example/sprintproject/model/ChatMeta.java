package com.example.sprintproject.model;

import com.google.firebase.Timestamp;
import java.util.List;

public class ChatMeta {
    private String title;
    private String summary;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private List<String> referencedChatIds;

    public ChatMeta() {

    }

    public String getTitle() {
        return title;
    }
    public String getSummary() {
        return summary;
    }
    public Timestamp getCreatedAt() {
        return createdAt;
    }
    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
    public List<String> getReferencedChatIds() {
        return referencedChatIds;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public void setSummary(String summary) {
        this.summary = summary;
    }
    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
    public void setReferencedChatIds(List<String> referencedChatIds) {
        this.referencedChatIds = referencedChatIds;
    }
}
