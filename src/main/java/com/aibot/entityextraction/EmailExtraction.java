package com.aibot.entityextraction;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import scala.Tuple2;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailExtraction implements EntityExtraction{
    String name;
    List<String[]> patterns;

    private static final String EMAIL_PATTERN =
            ".*[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);

    static EmailExtraction emailExtraction = null;

    public static EmailExtraction getInstance(){
        if(emailExtraction ==null){
            emailExtraction = new EmailExtraction();
        }
        return emailExtraction;
    }

    @Override
    public SetMultimap<String[], Integer> searchPatternToPosIndex(String[] text) {
        SetMultimap<String[], Integer> result = HashMultimap.create();
        try {
            for(int i= 0; i < text.length; i++){
                String word = text[i];
                Matcher matcher = emailPattern.matcher(word);
                if (matcher.find()) {
                    result.put(new String[]{word},i);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public SetMultimap<String[], Tuple2<Integer, String[]>> searchPartial(String[] text) {
        return null;
    }

    @Override
    public EntityExtraction setEntityName(String entityName) {
        this.name = entityName;
        return this;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<String[]> getPatterns() {
        return this.patterns;
    }

    @Override
    public EntityExtraction setPatterns(List<String[]> patterns) {
        this.patterns = patterns;
        return this;
    }

    @Override
    public boolean isPartialMatch() {
        return false;
    }
}
