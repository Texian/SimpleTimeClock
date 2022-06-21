package com.example.services;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class Time {
    private String id;
    private String date;
    private String description;
    private String hours;
    private String username;
    private String status;
    private String archive;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbPartitionKey
    public String getName() {
        return this.username;
    }
    public void setName(String username) {
        this.username = username;
    }

    public String getArchive() {
        return this.archive;
    }
    public void setArchive(String archive) {
        this.archive = archive;
    }

    public String getStatus() {
        return this.status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return this.username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getHours() {
        return this.hours;
    }
    public void setHours(String hours) {
        this.hours = hours;
    }

    public String getDate() {
        return this.date;
    }
    public void setDate(String date) {
        this.date = date;
    }

    public String getDescription() {
        return this.description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

}
