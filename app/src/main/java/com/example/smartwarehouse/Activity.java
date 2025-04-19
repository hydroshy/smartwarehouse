package com.example.smartwarehouse;

public class Activity implements Comparable<Activity> {
    private String description;
    private String date;

    public Activity(String description, String date) {
        this.description = description;
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public String getDate() {
        return date;
    }

    @Override
    public int compareTo(Activity other) {
        return other.date.compareTo(this.date); // Sắp xếp giảm dần theo ngày (mới nhất trước)
    }
}