package com.aibot.utils;

import com.aibot.data.model.Qa;
import com.aibot.entity.Clarification;
import com.aibot.qa.GlobalConstants;
import org.apache.commons.collections.map.HashedMap;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class QaUtil {

    public static List<String> getRecommendRestaurantIds(Qa qa) {
        List<String> ids = new ArrayList<>();
        qa.getAnswer().getResultRestaurants().forEach(r -> {
            ids.add(r.getId());
        });
        return ids;
    }
    public static List<String> getTopTargets(int numOfTargets, Qa qa) {
        //return some suggestions with criteria 1) longest source candidate + 2) highest score target compare to source
        List<String> result = new ArrayList<>();
        Map<String, Double> candidateMap = new HashedMap();
        for (Clarification c : qa.getClarification()) {
            int sourceLength = c.getSource().split("\\s+").length;
            for (String s : c.getTargets()) {
                int targetLength = s.split("\\s+").length;
                double score = sourceLength + sourceLength / (float) targetLength;
                candidateMap.put(s, score);
            }

        }
        result = candidateMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .map(x -> x.getKey())
                .collect(Collectors.toList());

        return result.subList(0, result.size() > numOfTargets ? numOfTargets : result.size());
    }
    public static List<String> getTopTargets(int numOfTargets, Map<String, Set<String>> map) {
        List<String> result = new ArrayList<>();
        Map<String, Double> candidateMap = new HashedMap();
        for (Map.Entry<String, Set<String>> c : map.entrySet()) {
            int sourceLength = c.getKey().split("\\s+").length;
            for (String s : c.getValue()) {
                int targetLength = s.split("\\s+").length;
                double score = sourceLength + sourceLength / (float) targetLength;
                candidateMap.put(s, score);
            }

        }
        result = candidateMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .map(x -> x.getKey())
                .collect(Collectors.toList());

        return result.subList(0, result.size() > numOfTargets ? numOfTargets : result.size());
    }

    public static List<Qa> getQACurrentSession(List<Qa> qaList) {
        List<Qa> qaCurrentSession = new ArrayList<>();
        try {
            //get latest session only
            int index = qaList.size()-1;
            for(index= qaList.size()-1; index>0; index=index-1){
                Qa q1 = qaList.get(index);
                Qa q2 = qaList.get(index-1);
                long duration = q1.getTime().getTime() - q2.getTime().getTime();
                long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration);
                if(diffInMinutes< GlobalConstants.sessionInterval) continue;
                else {
                    break;
                }
            }
            //get from the index
            for(int i = index; i< qaList.size(); i++){
                qaCurrentSession.add(qaList.get(i));
            }
        } catch (Exception e) {
            System.out.println("Error getting Q/A: " + e.getMessage());
            e.printStackTrace();
        }
        return qaCurrentSession;
    }
}
