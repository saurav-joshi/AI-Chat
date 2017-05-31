package com.aibot.entityextraction;

import com.aibot.qa.GlobalConstants;
import com.aibot.qa.S3Handler;
import com.aibot.qa.Parser;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.collections.map.HashedMap;
import scala.Tuple2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PaxExtraction implements EntityExtraction {
    String name;
    static List<String[]> patterns = new ArrayList<>();;
    Pattern numberPattern = Pattern.compile("\\d+");
    Pattern punctPattern = Pattern.compile("\\p{Punct}");
    private enum type {pax,adults,head, people,person, guest};
    private static ACTrie acTrie;

    static Map<String, String> numbers = new HashMap<String, String>();

    private static void setNumbers() {
        numbers.put("one", "1");
        numbers.put("two", "2");
        numbers.put("three", "3");
        numbers.put("four", "4");
        numbers.put("five", "5");
        numbers.put("six", "6");
        numbers.put("seven", "7");
        numbers.put("eight", "8");
        numbers.put("nine", "9");
        numbers.put("ten", "10");
        numbers.put("eleven", "11");
        numbers.put("twelve", "12");
    }

    private String formatNumber(String text) {
        Iterator<String> number = numbers.keySet().iterator();
        while (number.hasNext()) {
            String key = number.next();
            text = text.replace(key, numbers.get(key));
        }
        return text;
    }

    static {
        try{
            List<String> lines = S3Handler.readLinesFromFile(GlobalConstants.taxonomiesBucketNameS3, GlobalConstants.paxDictionary);
            lines.stream().forEach(line -> {
                patterns.add(Parser.wordTokenize(line.trim().toLowerCase()).stream().toArray(String[]::new));
                //        patterns.add(line.trim().toLowerCase().split("\\s+"));
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        acTrie = new ACTrie<>(patterns, "pax");
        setNumbers();
    }

    static PaxExtraction phoneExtraction = null;

    public static PaxExtraction getInstance(){
        if(phoneExtraction ==null){
            phoneExtraction = new PaxExtraction();
        }
        return phoneExtraction;
    }

    @Override
    public SetMultimap<String[], Integer> searchPatternToPosIndex(String[] text) {
        SetMultimap<String[], Integer> result = HashMultimap.create();
        try {
            String sentence = String.join(" ",text);//.replaceAll("\\p{Punct}", "");//.replaceAll("\\s+(?=\\p{Punct})", "");
            Map<Integer, String> numberTracking = new HashedMap();
            Map<Integer, String> punctTracking = new LinkedHashMap();
            Matcher matcher;

            for(int i=0; i< text.length; i++){
                matcher  = numberPattern.matcher(text[i]);
                if(matcher.find()){
                    numberTracking.put(i,matcher.group());
                    //continue;
                }

                matcher = punctPattern.matcher(text[i]);
                if(matcher.find() & text[i].length() <2){ //not punctuation in a word like in email...
                    punctTracking.put(i, matcher.group());
                }
            }

            sentence = sentence
                    .replaceAll("(?!/)(?!:)\\p{Punct}", "")
                    .replaceAll("\\d+","@number").replaceAll("( )+", " ");
            SetMultimap<String[], Integer> candidates = acTrie.searchPatternToPosStartIndex(sentence.split(" "));

            candidates.asMap().entrySet().forEach(e -> {
                List<String> found = new ArrayList<String>();
                int i = e.getValue().iterator().next();
                i += EntityExtractionUtil.punctBefore(i,punctTracking);
                int numOfPunct = 0;
                String[] words = e.getKey().clone();
                for (int j = 0; j < words.length; j++) {
                    if (words[j].contains("@number")) {
                        words[j] = words[j].replaceAll("@number",numberTracking.getOrDefault(j+i, words[j]));
                    }
                    found.add(words[j]);
                    //handle punctuation
                    if(j+i+1<text.length & j < words.length-1){
                        Matcher puctMatcher  = punctPattern.matcher(text[j+i+1]);
                        if(puctMatcher.find()){
                            found.add(text[j+i+1]);
                            i++;
                            numOfPunct++;
                        }
                    }
                }
                result.put(found.stream().toArray(String[]::new),i-numOfPunct);
            });
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
