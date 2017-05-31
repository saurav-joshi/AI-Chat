package com.aibot.entity;

import com.aibot.recommender.Constants;

import java.util.List;
import java.util.Map;
import java.util.Set;


public class RecommenderQuery {


    public enum QueryType {
        LookingForClassMember, SimilarTo, GeneralQuery, Others
    }

    Constants.RecommenderTarget target;
    List<String> fields;
    Map<String, List<String>> properties;
    Map<Constants.RecommenderTarget, Map<String, List<String>>> similarTo;
    List<Constants.RankCriteria> rankCriteria;
    int pages = Constants.DEFAULT_PAGE_NO;
    int numOfResult = Constants.DEFAULT_PAGE_SIZE;
    Set<String> idsToFilterOut;
    private QueryType queryType;

    public boolean isDistanceRefine() {
        return distanceRefine;
    }

    public void setDistanceRefine(boolean distanceRefine) {
        this.distanceRefine = distanceRefine;
    }

    private boolean distanceRefine;

    public Constants.RecommenderTarget getTarget() {
        return target;
    }

    public void setTarget(Constants.RecommenderTarget target) {
        this.target = target;
    }

    public List<String> getFields() {
        return fields;
    }

    public void setFields(List<String> fields) {
        this.fields = fields;
    }

    public Map<String, List<String>> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, List<String>> properties) {
        this.properties = properties;
    }

    public Map<Constants.RecommenderTarget, Map<String, List<String>>> getSimilarTo() {
        return similarTo;
    }

    public void setSimilarTo(Map<Constants.RecommenderTarget, Map<String, List<String>>> similarTo) {
        this.similarTo = similarTo;
    }

    public List<Constants.RankCriteria> getRankCriteria() {
        return rankCriteria;
    }

    public void setRankCriteria(List<Constants.RankCriteria> rankCriteria) {
        this.rankCriteria = rankCriteria;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int getNumOfResult() {
        return numOfResult;
    }

    public void setNumOfResult(int numOfResult) {
        this.numOfResult = numOfResult;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }

    public Set<String> getIdsToFilterOut() {
        return idsToFilterOut;
    }

    public void setIdsToFilterOut(Set<String> idsToFilterOut) {
        this.idsToFilterOut = idsToFilterOut;
    }

    public boolean isEmpty(){
        if((target == null)
                && (fields == null || fields.size() == 0)
                && (properties == null || properties.size() ==0)
                && (similarTo == null || similarTo.size() == 0)
                && (rankCriteria == null || rankCriteria.size() == 0)){
            return true;
        }
        return false;
    }


    @Override
    public String toString(){
        StringBuilder result = new StringBuilder();
        result.append("================ PrintQuery =================\n");
        result.append("Target:" + target+"\n");

        if(fields != null)
            result.append("Fields:" + String.join(",", fields)+"\n");

        if(properties != null) {
            result.append("Properties:\n");
            properties.forEach((key, val) -> {
                result.append("\t" + key + ":" + val+"\n");
            });
        }
        if(similarTo != null){
            result.append("Similar to:\n");
            similarTo.forEach((k,v) -> {
                result.append("\tSimilar to target:" + k+"\n");
                v.forEach((kk,vv) -> {
                    result.append("\t" + kk + ":" + String.join(",", vv)+"\n");
                });
            });
        }
        if(rankCriteria != null){
            result.append("Ranking criteria:\n");
            rankCriteria.forEach(k->result.append(k+"\n"));
        }
        return result.toString();
    }
}
