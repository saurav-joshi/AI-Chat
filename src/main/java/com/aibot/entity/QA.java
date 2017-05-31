package com.aibot.entity;

import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.qa.LibraryUtil;
import com.aibot.state.State;
import groovy.lang.Tuple2;
import org.apache.commons.collections.map.HashedMap;

import java.util.*;
import java.util.stream.Collectors;

public class QA {

    int id;
    int conversation_id;
    String question;
    String originalQuestion;

    public Set<Clarification> getClarification() {
        return clarification;
    }

    public void setClarification(Set<Clarification> clarification) {
        this.clarification = clarification;
    }

    Set<Clarification> clarification;

    List<String> cleanedQuestionPatternWords;
    Integer userId;
    Answer answer = new Answer();
    List<EntityExtractionUtil.EntityExtractionResult> entities;
    int matchedPatternIdInLibrary;
    Date time;
    List<State.StateType> statePaths = new ArrayList<>();
    String geo;
    String city;

    public LibraryUtil.Pattern getMatchedPattern(){
        return LibraryUtil.allPatternsMap.get(this.getMatchedPatternIdInLibrary());
    }

    public void setMatchedPattern(LibraryUtil.Pattern pattern){
        this.matchedPatternIdInLibrary = pattern.getId();
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public List<String> getCleanedQuestionPatternWords() {
        return cleanedQuestionPatternWords;
    }

    public List<EntityExtractionUtil.EntityExtractionResult> getEntities() {
        return entities;
    }

    public void setEntities(List<EntityExtractionUtil.EntityExtractionResult> entities) {
        this.entities = entities;
    }

    public void setCleanedQuestionPatternWords(List<String> cleanedQuestionPatternWords) {
        this.cleanedQuestionPatternWords = cleanedQuestionPatternWords;
    }

    public void setEntities(Tuple2<String, String> entities) {
        this.entities = entities;
    }

    public int getMatchedPatternIdInLibrary() {
        return matchedPatternIdInLibrary;
    }

    public void setMatchedPatternIdInLibrary(int matchedPatternIdInLibrary) {
        this.matchedPatternIdInLibrary = matchedPatternIdInLibrary;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public List<State.StateType> getStatePaths() {
        return statePaths;
    }

    public void setStatePaths(List<State.StateType> statePaths) {
        this.statePaths = statePaths;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getQuestion() {
        return question;
    }
    public QA setQuestion(String question) {
        this.question = question;
        return this;
    }

    public String getOriginalQuestion(){
        return originalQuestion;
    }
    public void setOriginalQuestion(String originalQuestion){
        this.originalQuestion = originalQuestion;
    }
    public Answer getAnswer() {
        return answer;
    }
    public void setAnswer(Answer answer) {
        this.answer = answer;
    }

    public int getConversation_id() {
        return conversation_id;
    }

    public void setConversation_id(int conversation_id) {
        this.conversation_id = conversation_id;
    }

    public List<String> getRecommendRestaurantIds(){
        List<String> ids = new ArrayList<>();
        this.getAnswer().getResultRestaurants().forEach(r -> {
            ids.add(r.getId());
        });
        return ids;
    }

    public List<String> getTopTargets(int numOfTargets) {
        //return some suggestions with criteria 1) longest source candidate + 2) highest score target compare to source
        List<String> result = new ArrayList<>();
        Map<String,Double> candidateMap = new HashedMap();
        for (Clarification c: clarification){
            int sourceLength = c.getSource().split("\\s+").length;
            for(String s: c.getTargets()){
                int targetLength = s.split("\\s+").length;
                double score = sourceLength + sourceLength/(float)targetLength;
                candidateMap.put(s, score);
            }

        }
        result = candidateMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .map( x-> x.getKey())
                .collect(Collectors.toList());

        return result.subList(0, result.size()>numOfTargets ? numOfTargets : result.size());
    }

    public static List<String> getTopTargets(int numOfTargets, Map<String,Set<String>> map) {
        List<String> result = new ArrayList<>();
        Map<String,Double> candidateMap = new HashedMap();
        for (Map.Entry<String, Set<String>> c: map.entrySet()){
            int sourceLength = c.getKey().split("\\s+").length;
            for(String s: c.getValue()){
                int targetLength = s.split("\\s+").length;
                double score = sourceLength + sourceLength/(float)targetLength;
                candidateMap.put(s, score);
            }

        }
        result = candidateMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .map( x-> x.getKey())
                .collect(Collectors.toList());

        return result.subList(0, result.size()>numOfTargets ? numOfTargets : result.size());
    }
}