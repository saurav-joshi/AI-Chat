package com.aibot.entity;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Suggestion {
    private String message;
    private List<String> options;
    String type; // the type in the pattern library

    public Suggestion(String message, List<String> options, String type) {
        this.message = message;
        this.options = options;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String toString(){
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> suggestionMap = new HashMap<>();
        suggestionMap.put("message", this.message);
        suggestionMap.put("options", this.getOptions());
        suggestionMap.put("type", this.getType());
        String resultString = null;
        try {
            resultString = mapper.writeValueAsString(suggestionMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultString;
    }
}
