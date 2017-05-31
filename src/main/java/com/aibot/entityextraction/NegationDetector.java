package com.aibot.entityextraction;

import com.google.common.collect.SetMultimap;
import com.google.common.io.Resources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import scala.Tuple2;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class NegationDetector implements Serializable {
    private static ACTrie<String> preNegPhrases;
    private static ACTrie<String> postNegPhrases;
    private static ACTrie<String> conjunctions;

    private final int windowForPreNeg;
    private final int windowForPostNeg;
    private String negexType;


    /*@Value("${negativePhrasesPath}")
    private String negativePhrasesPath;// = "sentiment/negative-phrases.txt";
    @Value("${postNegPhrasesPath}")
    private String postNegPhrasesPath;// = "sentiment/negative-phrases.txt";
    @Value("${conjunctionsFilePath}")
    private String conjunctionsFilePath;//= "sentiment/negative-phrases.txt";
    @Value("${preNegativePhraseWindow}")
    private String preNegativePhraseWindow;// = 0;
    @Value("${postNegativePhraseWindow}")
    private String postNegativePhraseWindow;// = 2;
*/
    @Inject
    public NegationDetector (@Value("${negativePhrasesPath}") String negativePhrasesPath, @Value("${postNegPhrasesPath}") String postNegPhrasesPath,
                             @Value("${conjunctionsFilePath}") String conjunctionsFilePath,@Value("${preNegativePhraseWindow}") String preNegativePhraseWindow,
                             @Value("${postNegativePhraseWindow}") String postNegativePhraseWindow) {
        preNegPhrases = buildTrie(negativePhrasesPath);
        postNegPhrases = buildTrie(postNegPhrasesPath);
        conjunctions = buildTrie(conjunctionsFilePath);

        this.windowForPreNeg = Integer.parseInt(preNegativePhraseWindow);
        this.windowForPostNeg = Integer.parseInt(postNegativePhraseWindow);
    }

    /**
     * Returns the negation scope for a given sentence
     * @param



     * @return A string with -1 if no negation phrase is found
     *        else "a - b" where a = start of phrase, b = end
     */
    public Tuple2<Integer, Integer> negationScope(String[] words) {
        SetMultimap<Integer, Integer> preNegResults = preNegPhrases.search(words, false);
        if (preNegResults.size() > 0) {
            negexType = "preNegPhrases";
            Map.Entry<Integer, Integer> foundNeg = preNegResults.entries().iterator().next();
            int startIndex = foundNeg.getKey();
            int endIndex = foundNeg.getKey() + preNegPhrases.getPatternFromIndex(foundNeg.getValue()).length;
            List<Map.Entry<Integer, Integer>> conjResults =
                conjunctions.search(words, false).entries().stream().filter(x -> x.getKey() > endIndex).collect(Collectors.toList());
            if (conjResults.size() == 0)
                return new Tuple2<>(startIndex, endIndex);
            else {
                Map.Entry<Integer, Integer> foundConj = conjResults.get(0);
                return new Tuple2<>(startIndex,foundConj.getKey());
            }
        }

        SetMultimap<Integer, Integer> postNegResults = postNegPhrases.search(words, false);
        if (postNegResults.size() > 0) {
            negexType = "postNegPhrases";
            Map.Entry<Integer, Integer> foundPostNeg = postNegResults.entries().iterator().next();
            return new Tuple2<>(0,foundPostNeg.getKey());
        }
        return new Tuple2<>(0, 0);
    }

    public boolean isNegation(String[] tokenArray, Tuple2<Integer, Integer> scope, String[] keywordArray){
        boolean isNeg = false;
        if(scope._1()==scope._2()){
            return isNeg;
        }
        int index = Collections.indexOfSubList(Arrays.asList(tokenArray), Arrays.asList(keywordArray));
        if (index < scope._1() || index > scope._2())
            isNeg = false;
        if (negexType.equals("preNegPhrases"))
            isNeg = Math.abs(index - scope._1()) <= windowForPreNeg;
        if (negexType.equals("postNegPhrases"))
            isNeg = Math.abs(index - scope._2()) <= windowForPostNeg;
        return isNeg;
    }

    private static ACTrie<String> buildTrie(String path) {
        try {
            System.out.println(path+"---------------------");
            return new ACTrie<>(
                Resources.readLines(
                    Thread.currentThread().getContextClassLoader().getResource(path),
//                    Resources.getResource(path),
                    Charset.defaultCharset()
                ).stream().map(s -> s.split("\\s+")).collect(Collectors.toList())
            ,"");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
