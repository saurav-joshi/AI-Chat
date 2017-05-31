package com.aibot.entityextraction;

import com.aibot.qa.GlobalConstants;
import com.aibot.qa.Parser;
import com.aibot.qa.S3Handler;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import org.apache.commons.collections.map.HashedMap;
import scala.Tuple2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DistanceExtraction implements EntityExtraction {
    String name;
    static List<String[]> patterns = new ArrayList<>();;
//    Pattern numberPattern = Pattern.compile("\\d+");
    Pattern numberPattern = Pattern.compile("([0-9]*[.])?[0-9]+");
    Pattern punctPattern = Pattern.compile("\\p{Punct}");
    private static ACTrie acTrie;

    static Map<String, String> numbers = new HashMap<String, String>();

    static {
        try{
            List<String> lines = S3Handler.readLinesFromFile(GlobalConstants.taxonomiesBucketNameS3, GlobalConstants.distanceDictionary);
            lines.stream().forEach(line -> {
                patterns.add(Parser.wordTokenize(line.trim().toLowerCase()).stream().toArray(String[]::new));
                //        patterns.add(line.trim().toLowerCase().split("\\s+"));
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        acTrie = new ACTrie<>(patterns, "distance");

    }

    static DistanceExtraction distanceExtraction = null;

    public static DistanceExtraction getInstance(){
        if(distanceExtraction ==null){
            distanceExtraction = new DistanceExtraction();
        }
        return distanceExtraction;
    }

    @Override
    public SetMultimap<String[], Integer> searchPatternToPosIndex(String[] text) {
        SetMultimap<String[], Integer> result = HashMultimap.create();
        try {
            String sentence = String.join(" ",text);//.replaceAll("\\s+(?=\\p{Punct})", "");
            Map<Integer, String> numberTracking = new HashedMap();
            Map<Integer, String> punctTracking = new HashedMap();
            Matcher matcher;
            for(int i=0; i< text.length; i++){
                matcher  = numberPattern.matcher(text[i]);
                if(matcher.find()){
                    numberTracking.put(i,matcher.group());
                    continue;
                }
                matcher  = punctPattern.matcher(text[i]);
                if(matcher.find()){
                    punctTracking.put(i,matcher.group());
                    continue;
                }
            }

            sentence = sentence.replaceAll("(?!/)(?!:)\\p{Punct}", "").replaceAll("\\d+","@number").replaceAll("( )+", " ");
            SetMultimap<String[], Integer> candidates = acTrie.searchPatternToPosStartIndex(sentence.split(" "));
            //convert back to the original text then replace pattern
            candidates.asMap().entrySet().forEach(e -> {
                List<String> found = new ArrayList<String>();
                int i = e.getValue().iterator().next();
                i += EntityExtractionUtil.punctBefore(i,punctTracking);
                int numOfPunct = 0;
                String[] words = e.getKey().clone();
                for (int j = 0; j < words.length; j++) {
                    if (words[j].contains("@number")) {
                        words[j] = words[j].replace("@number",numberTracking.getOrDefault(j+i, words[j]));
                    }
                    found.add(words[j]);
                    if(j+i+1<text.length & j < words.length-1){
                        Matcher puctMatcher  = punctPattern.matcher(text[j+i+1]);
                        if(puctMatcher.find()){
                            found.add(text[j+i+1]);
                            i++;
                            numOfPunct++;
                        }
                    }
                }
                int index = i-numOfPunct;
                if (index < text.length)
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

    // return formatted distance (in km) from text
    public String getFormattedDistanceFromText(String distanceText){
        if(distanceText == null || distanceText.trim().length() == 0)
            return null;
        String distanceNum = null;
        Matcher matcher = numberPattern.matcher(distanceText);
        if(matcher.find()){
            distanceNum =  matcher.group(0);
            String reminingString = distanceText.substring(distanceText.indexOf(distanceNum)+distanceNum.length(), distanceText.length());

            if(reminingString.contains("km") ||
                    reminingString.contains("kilometer") ||
                    reminingString.contains("kilometre") ||
                    reminingString.contains("k")){
                return distanceNum;
            }else if(reminingString.contains("meter") ||
                    reminingString.contains("metre") ||
                    reminingString.contains("m")){
                return String.valueOf(Double.valueOf(distanceNum)/1000.0);
            }else{
                if(Double.valueOf(distanceNum)<100)
                    return distanceNum;
                else
                    return String.valueOf(Double.valueOf(distanceNum)/1000.0);
            }
        }
        return distanceNum;
    }
}
