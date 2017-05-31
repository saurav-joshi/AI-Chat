package com.aibot.spelling;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.aibot.qa.GlobalConstants;
import com.aibot.qa.LibraryUtil;
import com.aibot.qa.Parser;
import com.aibot.qa.S3Handler;
import com.aibot.utils.StringUtils;
import org.apache.commons.collections.map.HashedMap;
import org.slf4j.LoggerFactory;
import scala.Tuple2;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpellingCorrection {

    private Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    private int stepThreshold = 1;

    private static Set<String> englishDict = new HashSet<>();

    public SpellingCorrection() {
        root.setLevel(Level.WARN);
        List<String> dictionaries = S3Handler.getFilesInFolder(GlobalConstants.taxonomiesBucketNameS3, GlobalConstants.taxonomiesFolderNameS3);

        for (String fileKey : dictionaries) {
            if (fileKey.split("/").length <= 1
                    //|| fileKey.toLowerCase().contains("dish")
                    )
                continue;
            List<String> dictLines = null;
            try {
                dictLines = S3Handler.readLinesFromFile(GlobalConstants.taxonomiesBucketNameS3, fileKey);
            } catch (IOException e) {
                e.printStackTrace();
            }
            dictLines.stream().map(x -> Parser.stem(x.toLowerCase())).forEach(x -> englishDict.add(x));
        }
        //add our patterns as well
        Pattern p = Pattern.compile("[^a-z]", Pattern.CASE_INSENSITIVE);

        LibraryUtil.patternsForState.stream().forEach(x -> {
            Stream.of(new StringBuilder(x.getUserInputPattern().toLowerCase()).append(" ").append(x.getSystemMessage().toLowerCase())
                    .toString().split("\\s+")).forEach((word) ->{
                Matcher m = p.matcher(word);
                if(!m.find()){
                    englishDict.add(word);
                }
            });
        });
    }

    public Map<String, List<String>> spellingCandidates(String text) {
        Map<String, List<String>> candidateMap = new LinkedHashMap<>();
        List<String> unigrams = Parser.wordTokenize(text.toLowerCase());
        List<String> bigrams = Parser.ngrams(2, unigrams);
        List<String> trigrams = Parser.ngrams(3, unigrams);
        trigrams.addAll(bigrams);
        trigrams.addAll(unigrams);

        for(String s : trigrams){
            if(!isValidCandidate(s)){
                List<Tuple2<String,Double>> candidates = correct(s, englishDict);
                //only consider phrase has candidate
                if(!candidates.isEmpty()){
                    //candidateMap.put(s, candidates.stream().map(x->x._1).collect(Collectors.toSet()));
                    Map<String,Double> scoringMap =  new HashedMap();
                    candidates.stream().forEach(x -> {
                        scoringMap.put(x._1(), x._2());
                    });
                    //sorting and add
                    candidateMap.put(s,sort(scoringMap));
                }
            }
        }
        return candidateMap;
    }

    private boolean isValidCandidate(String gram){//not candidate if every word is a valid one
        return Stream.of(gram.split("\\s+")).allMatch(x -> englishDict.contains(x));
    }

    public static String replaceSpelling(String text, Map<String,List<String>> candidateMap){
        Iterator<Map.Entry<String,List<String>>> it = candidateMap.entrySet().iterator();
        List<String> usedgram = new ArrayList<>();
        while (it.hasNext())
        {
            Map.Entry<String,List<String>> en = it.next();
            if(!SpellingCorrection.isUsed(usedgram,en.getKey())){
                text = text.replace(en.getKey(), en.getValue().iterator().next());//pop up first value, highest score
                usedgram.add(en.getKey());
                it.remove();
            }
        }
        return text;
    }

    public static List<String> sort(Map<String,Double> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .map( x-> x.getKey())
                .collect(Collectors.toList());
    }

    public static boolean isUsed(List<String> usedgram, String s){
        return usedgram.parallelStream().anyMatch(x-> x.contains(s));
    }

    private List<Tuple2<String,Double>> correct(String target, Set<String> dict) {
        List<Tuple2<String,Double>> results = new ArrayList<>();

            Map<String,Set<String>> emptyMap = new HashedMap();
            List<String> lstTarget = wordToList(target);
            dict.stream().forEach(x -> {
                    if(StringUtils.getLevenshteinDistance(target, x) <= stepThreshold){
                        double score = LibraryUtil.SSS(lstTarget, wordToList(x), emptyMap);
                        //System.out.println(lstTarget + "\t" + wordToList(x)+ "=>" + score);
                        if(score >= 0.5){
                            results.add(new Tuple2<>(x, score));
                        }
                    }
            });
                //.collect(Collectors.toList());
            return results;

    }

    private List<String> wordToList(String word){
        List<String> result = new ArrayList<>();
        for(char c: word.toCharArray()){
            result.add(String.valueOf(c));
        }
        return result;
    }
}
