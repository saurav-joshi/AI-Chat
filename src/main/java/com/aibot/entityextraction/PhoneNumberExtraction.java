package com.aibot.entityextraction;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import scala.Tuple2;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumberExtraction implements EntityExtraction {
    String name;
    List<String[]> patterns;
    Pattern phonePattern = Pattern.compile("\\d+.\\d+");

    static PhoneNumberExtraction phoneExtraction = null;

    public static PhoneNumberExtraction getInstance(){
        if(phoneExtraction ==null){
            phoneExtraction = new PhoneNumberExtraction();
        }
        return phoneExtraction;
    }

    @Override
    public SetMultimap<String[], Integer> searchPatternToPosIndex(String[] text) {
        SetMultimap<String[], Integer> result = HashMultimap.create();
        try {
            String sentence = String.join(" ",text);
            Matcher matcher = phonePattern.matcher(sentence);
            if (matcher.find()) {
                String word = matcher.group();
                if(word.length()>=4){
                    String[] words = word.split("\\s+");
                    int index = EntityExtractionUtil.getPosition(text, words);
                    if (index > -1)
                        result.put(words,index);
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
