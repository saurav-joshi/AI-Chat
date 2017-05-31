package com.aibot.entity;

public class Context {
    String location;
    String time;
    String [] latlong;

    public Context(String location, String time) {
        this.location = location;
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String[] getLatlong() {
        return latlong;
    }

    public void setLatlong(String[] latlong) {
        this.latlong = latlong;
    }
}
