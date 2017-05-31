package com.aibot.entity;

import java.util.Collections;
import java.util.Set;


public class Clarification {
    Type type;
    String source;
    Set<String> targets;
    Boolean doneFlag = false;

    public static enum Type {
        SpellingCorrection, FuzzyMatch, WordDisambiguation
    }

    public Clarification(Type type, String source, Set<String> targets, Boolean doneFlag) {
        this.type = type;
        this.source = source;
        this.targets = targets;
        this.doneFlag = doneFlag;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Set<String> getTargets() {
        return targets;
    }

    public void setTargets(Set<String> targets) {
        this.targets = targets;
    }

    public Boolean getDoneFlag() {
        return doneFlag;
    }

    public void setDoneFlag(Boolean doneFlag) {
        this.doneFlag = doneFlag;
    }
}

