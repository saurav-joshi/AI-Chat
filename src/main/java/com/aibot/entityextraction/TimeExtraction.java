package com.aibot.entityextraction;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import scala.Tuple2;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeExtraction implements EntityExtraction {
    String name;
    List<String[]> patterns;
    //Pattern timePattern = Pattern.compile("(^|\\s+)(([0-9]|1[0-2]):?\\s*([0-5][0-9])?\\s*([ap]m))|((1[3-9]|2[0-4]):?\\s*([0-5][0-9])?\\s*(\\^[ap]m))");
    Pattern timePattern = Pattern.compile("(^|\\s+)(([0-9]|1[0-2]):?\\s*([0-5][0-9])?\\s*([ap]m))" +
            "|((1[3-9]|2[0-4]):?\\s*([0-5][0-9])?\\s*)");
    static TimeExtraction phoneExtraction = null;

    public static TimeExtraction getInstance(){
        if(phoneExtraction ==null){
            phoneExtraction = new TimeExtraction();
        }
        return phoneExtraction;
    }

    @Override
    public SetMultimap<String[], Integer> searchPatternToPosIndex(String[] text) {
        SetMultimap<String[], Integer> result = HashMultimap.create();
        try {
            String sentence = String.join(" ",text);
            Matcher matcher = timePattern.matcher(sentence);
            if (matcher.find()) {
                String word = matcher.group().trim();
                if(word.length()>=2){
                    String[] words = word.split("\\s+");
                    int index = EntityExtractionUtil.getPosition(text, words);
                    if (index > -1)
                        result.put(new String[]{word},index);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        /*String sentence = String.join(" ", Arrays.asList(text)).replaceAll("\\s+(?=\\p{Punct})", "");

        List<ParsedResult> dates = Chrono.casual.parse(sentence);

        for(ParsedResult date: dates){
            System.out.println(DateExtraction.getTime(date.text));
            String[] words = com.crayon.qa.Parser.wordTokenize(date.text).stream().toArray(String[]::new); //date.text.split(" ");
            if(words!=null && words.length>0){
                //result.put(words,getPosition(sentence,words[0]));
                int index = EntityExtractionUtil.getPosition(text, words);
                if (index > -1)
                    result.put(words,index);
            }
        }*/
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
