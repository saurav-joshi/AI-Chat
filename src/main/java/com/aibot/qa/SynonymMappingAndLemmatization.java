package com.aibot.qa;

import com.google.common.io.Resources;

import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class SynonymMappingAndLemmatization {

    public static Map<String, String> synMapping = new HashMap<>();

    public static void init(String path) {
        try {
            List<String> lines = Resources.readLines(Resources.getResource(path), Charset.defaultCharset());
            //country mapping
            //lines.addAll(S3Handler.readLinesFromFile(GlobalConstants.taxonomiesBucketNameS3, GlobalConstants.countryMapping));
            lines.remove(0);
            lines.forEach(line -> {
                String[] columns = line.split("\\t");
                for(String key: columns[0].split("\\||\\|")){
                    synMapping.put(key.toLowerCase(), columns[1]);
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String run(String text){
        text = text.toLowerCase();
        List<String> unigrams = Parser.wordTokenize(text);
        List<String> allGrams = new LinkedList<>();
        allGrams.addAll(Parser.ngrams(3,unigrams));
        allGrams.addAll(Parser.bigrams(unigrams,false));
        allGrams.addAll(unigrams);
        if(allGrams.size() > 200) return  text;

        //mark * end each special keywords
        // get the longest match for replacement prior to the short ones in case there are multiple matches
        Iterator<String> iter = allGrams.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            // remove it after finishing processing
            iter.remove();

            if(synMapping.containsKey(key)){
                String t = Parser.wordTokenize(synMapping.get(key)).stream().map(x->x+"*").collect(Collectors.joining(" "));
                String regex = "\\b" + key + "\\b";
                if(key.equals("@") || key.equals("&")){
                    t = Parser.wordTokenize(synMapping.get(key)).stream().map(x->" "+x+"*").collect(Collectors.joining(" "));
                    regex = "\\b\\s*" + key + "\\s*\\b";
                }
                text = text.replaceAll(regex,t);
                List<String> subUnigrams = Parser.wordTokenize(key);
                if(subUnigrams.size() == 3){
                    List<String> subBigrams = Parser.bigrams(subUnigrams,false);
                    allGrams.removeIf(x->subBigrams.contains(x));
                    allGrams.removeIf(x->subUnigrams.contains(x));
                }else if(subUnigrams.size() == 2){
                    allGrams.removeIf(x->subUnigrams.contains(x));
                }
            }
            iter = allGrams.iterator();
        }

        List<String> refinedTokens = Parser.wordTokenize(text); //with * end
        // combine the words with the star again since Parser.wordTokenize separate them with space
        while(refinedTokens.contains("*")){
            int idx = refinedTokens.indexOf("*");
            if(idx != -1 && idx != 0){
                refinedTokens.set(idx-1, refinedTokens.get(idx-1)+"*");
                refinedTokens.remove(idx);
            }
        }

        return refinedTokens.stream().map( x-> {
            if(x.length() > 1 && x.endsWith("*")) return String.join(" " , Parser.wordTokenize(x.substring(0, x.length() - 1)));
            else return Parser.stem(x);
        }).filter(x-> !x.equals("*")).collect(Collectors.joining(" "));//.replaceAll("\\s+(?=\\p{Punct}\\s+)", "");
    }
}
