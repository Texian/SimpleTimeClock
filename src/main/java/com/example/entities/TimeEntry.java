package com.example.entities;

public class TimeEntry {
    private String id;
    private String name;
    private String hours;
    private String date;
    private String description;
    private String status;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() { return this.date; }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    public void setHours(String hours) {
        this.hours = hours;
    }

    public String getHours() {
        return this.hours;
    }
}
