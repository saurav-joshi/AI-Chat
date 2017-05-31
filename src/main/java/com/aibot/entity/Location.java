package com.aibot.entity;

public class Location {
    String name;
    String city;
    String country;
    Double lat;
    Double lon;
    String type;

    public Location(String name, String city, String country, Double lat, Double lon, String type) {
        this.name = name;
        this.city = city;
        this.country = country;
        this.lat = lat;
        this.lon = lon;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
