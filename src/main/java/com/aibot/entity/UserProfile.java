package com.aibot.entity;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

public class UserProfile {
    String name;
    String userId;

    List<String> likedRests = new ArrayList<>();
    List<String> dislikedRests = new ArrayList<>();
    List<String> likedDishes = new ArrayList<>();
    List<String> dislikedDishes = new ArrayList<>();
    List<String> likedCuisines = new ArrayList<>();
    List<String> dislikedCuisines = new ArrayList<>();
    List<String> likedLocations = new ArrayList<>();
    List<String> dislikedLocations = new ArrayList<>();

    Map<String, Map<String, Map<String, Long>>> contextPreference;
    // example: {context1: {{Chinese:2} {italian:10}}, <dish,<<>,<>>>}

    Map<String,Map<String,List<String>>> likedRestAssociations;

    public UserProfile(){}

    public UserProfile(String userId){
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getLikedRests() {
        return likedRests;
    }

    public void setLikedRests(List<String> likedRests) {
        this.likedRests = likedRests;
    }

    public List<String> getDislikedRests() {
        return dislikedRests;
    }

    public void setDislikedRests(List<String> dislikedRests) {
        this.dislikedRests = dislikedRests;
    }

    public List<String> getLikedDishes() {
        return likedDishes;
    }

    public void setLikedDishes(List<String> likedDishes) {
        this.likedDishes = likedDishes;
    }

    public List<String> getDislikedDishes() {
        return dislikedDishes;
    }

    public void setDislikedDishes(List<String> dislikedDishes) {
        this.dislikedDishes = dislikedDishes;
    }

    public List<String> getLikedCuisines() {
        return likedCuisines;
    }

    public void setLikedCuisines(List<String> likedCuisines) {
        this.likedCuisines = likedCuisines;
    }

    public List<String> getDislikedCuisines() {
        return dislikedCuisines;
    }

    public void setDislikedCuisines(List<String> dislikedCuisines) {
        this.dislikedCuisines = dislikedCuisines;
    }

    public List<String> getLikedLocations() {
        return likedLocations;
    }

    public void setLikedLocations(List<String> likedLocations) {
        this.likedLocations = likedLocations;
    }

    public List<String> getDislikedLocations() {
        return dislikedLocations;
    }

    public void setDislikedLocations(List<String> dislikedLocations) {
        this.dislikedLocations = dislikedLocations;
    }

    public Map<String, Map<String, Map<String, Long>>> getContextPreference() {
        return contextPreference;
    }

    public void setContextPreference(Map<String, Map<String, Map<String, Long>>> contextPreference) {
        this.contextPreference = contextPreference;
    }

    public Map<String, Map<String, List<String>>> getLikedRestAssociations() {
        return likedRestAssociations;
    }

    public void setLikedRestAssociations(Map<String, Map<String, List<String>>> likedRestAssociations) {
        this.likedRestAssociations = likedRestAssociations;
    }

    public boolean hasContextPreference(){
        if(contextPreference == null || contextPreference.size() == 0)
            return false;
        return true;
    }

    public String listToString(List<String> alist){
        return String.join(",", alist);
    }

    public String mapToString(Object map2Convert){
        ObjectMapper mapper = new ObjectMapper();
        String preferenceJsonStr = null;
        try {
            preferenceJsonStr = mapper.writeValueAsString(map2Convert);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return preferenceJsonStr;
    }

}


