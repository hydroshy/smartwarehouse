package com.example.smartwarehouse;

public class RecentActivity {
    private String description;
    private String details;
    private String timestamp;

    public RecentActivity(String description, String details, String timestamp) {
        this.description = description;
        this.details = details;
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public String getDetails() {
        return details;
    }

    public String getTimestamp() {
        return timestamp;
    }
}