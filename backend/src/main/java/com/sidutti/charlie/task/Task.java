package com.sidutti.charlie.task;



public class Task {
    private  String description;

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private  String agent;

    public Task(String description, String agent) {
        this.description = description;
        this.agent = agent;
    }

    public Task(String description) {
        this(description, null);
    }

    public String getDescription() {
        return description;
    }

    public String getAgent() {
        return agent;
    }
}

