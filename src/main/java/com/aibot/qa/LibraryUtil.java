package com.aibot.qa;

import com.google.common.io.Resources;
import scala.Tuple2;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public class LibraryUtil {

    public static class Pattern {
        int id;
        String libraryName;
        String userInputPattern;
        List<String> questionPatternWords;
        String systemMessage;
        String patternAtt;
        Set<String> questionType;
        String owner;

        public String getUserInputPattern() {
            return userInputPattern;
        }

        public void setUserInputPattern(String userInputPattern) {
            this.userInputPattern = userInputPattern;
        }

        public Set<String> getQuestionType() {
            return questionType;
        }

        public void setQuestionType(Set<String> questionType) {
            this.questionType = questionType;
        }

        public List<String> getQuestionPatternWords() {
            return questionPatternWords;
        }

        public void setQuestionPatternWords(List<String> questionPatternWords) {
            this.questionPatternWords = questionPatternWords;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getLibraryName() {
            return libraryName;
        }

        public void setLibraryName(String libraryName) {
            this.libraryName = libraryName;
        }

        public String getPatternAtt() {
            return patternAtt;
        }

        public void setPatternAtt(String patternAtt) {
            this.patternAtt = patternAtt;
        }

        public String getSystemMessage() {
            return systemMessage;
        }

        public void setSystemMessage(String systemMessage) {
            this.systemMessage = systemMessage;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }
    }

    static class Context {

        String contextName;

        public String getRule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }

        String rule;

        int id;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getContextName() {
            return contextName;
        }

        public void setContextName(String contextName) {
            this.contextName = contextName;
        }

        @Override
        public boolean equals(Object arg0) {
            return (this.getContextName()+this.rule)
                .equals(((Context)arg0).getContextName()+((Context)arg0).getRule());
        }

        @Override
        public int hashCode() {
            return contextName.length()+rule.length();
        }
    }

    public static List<Pattern> patternsForState = new ArrayList<>();
    public static List<Pattern> allPatterns = new ArrayList<>();
    public static Map<Integer, Pattern> allPatternsMap = new HashMap<>();
    public static Map<String, Set<String>> embeddedContextsMap = new HashMap<>();
    public static Map<String, Set<String>> flatContextsMap = new HashMap<>();
    private static String punctRegex = "\\p{Punct}";
    static Random rand = new Random();
    private static List<String> changingBookingPatterns = new ArrayList<>();

    public static void init(){
        loadPattern(GlobalConstants.patternFilePath);
        loadContext(GlobalConstants.contextFilePath);
    }

    private static void loadPattern(String path) {
        try {
            List<String> lines = Resources.readLines(Resources.getResource(path), Charset.defaultCharset());
            lines.remove(0);
            lines.forEach(line -> {
                String[] columns = line.split("\\t");
                Pattern pattern = new Pattern();
                pattern.setId(Integer.parseInt(columns[0]));
                pattern.setLibraryName(columns[1]);
                pattern.setUserInputPattern(columns[2]);
                pattern.setQuestionPatternWords(Parser.lemmatizeAndLowercaseText(columns[2]));
                pattern.setSystemMessage(columns[3]);
                pattern.setPatternAtt(columns[4]);
                Set<String> questionTypes = new TreeSet<>((s1, s2) -> s2.toLowerCase().compareTo(s1.toLowerCase()));
                Arrays.stream(columns[5].split("\\|\\|")).forEach( x-> questionTypes.add(x));
                pattern.setQuestionType(questionTypes);
                pattern.setOwner(columns[6]);
                if(pattern.getLibraryName().endsWith("State") & pattern.getOwner().equalsIgnoreCase("user"))
                   patternsForState.add(pattern);
                allPatterns.add(pattern);
                allPatternsMap.put(pattern.getId(),pattern);

                //changing booking patterns
                if(questionTypes.contains("Booking.ChangeBooking.SingleDate")
                        || questionTypes.contains("Booking.ChangeBooking.SinglePax")
                        || questionTypes.contains("Booking.ChangeBooking.Double")){
                    changingBookingPatterns.add(columns[2].toLowerCase());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Pattern getRandomPatternByQuestionClass(String className){
        List<Pattern> patterns = LibraryUtil.allPatterns.stream()
            .filter(x -> x.getQuestionType().contains(className))
            .collect(Collectors.toList());
        if(patterns.size() == 0)
            throw new IllegalArgumentException("No Patterns Found By " + className);
        return patterns.get(rand.nextInt(patterns.size()));
    }

    private static void loadContext(String path) {
        try {
            // create embeddedContextsMap
            List<String> lines = Resources.readLines(Resources.getResource(path), Charset.defaultCharset());
            lines.remove(0);
            for(String line : lines){
                String[] columns = line.toLowerCase().split("\\t");
                embeddedContextsMap.computeIfAbsent(columns[1], v-> new HashSet<>()).add(columns[2]);
            }
            // create flatContextsMap
            embeddedContextsMap.entrySet().forEach(e -> {
                flatContextsMap.computeIfAbsent(e.getKey(), v -> new HashSet<>()).addAll(e.getValue());
                Set<String> children = e.getValue().stream().flatMap(x -> embeddedContextsMap.getOrDefault(x, new HashSet<>()).stream() ).collect(Collectors.toSet());
                while (!children.isEmpty()) {
                    flatContextsMap.computeIfAbsent(e.getKey(), v -> new HashSet<>()).addAll(children);
                    children =  children.stream().flatMap(x ->
                        embeddedContextsMap.getOrDefault(x, new HashSet<>()).stream()
                    ).collect(Collectors.toSet());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Tuple2<Pattern,Double> patternClassification(List<String> words , Map<String,Set<String>> flatContextsMap){
        Pattern pattern = patternsForState.stream().reduce((a, b) -> SSS(words, a.getQuestionPatternWords(),flatContextsMap) - SSS(words, b.getQuestionPatternWords(),flatContextsMap) > 0 ? a : b).get();
        double score = SSS(words, pattern.getQuestionPatternWords(),flatContextsMap);
        return new Tuple2<>(pattern, score);
    }

    /**
     * Semantic Sequence Similarity based on the rules specified in context
     * Time complexity is 2^N
     * @param message
     * @param questionPattern
     * @param flatContextsMap
     * @return
     */
    public static double SSS(List<String> message, List<String> questionPattern, Map<String,Set<String>> flatContextsMap) {
        // opt[i][j] denote the length of the SSS of x[i..M] and y[j..N], so no need to reverse during the recover SSS

        message.removeIf(x -> x.matches(punctRegex));
        questionPattern.removeIf(x -> x.matches(punctRegex));

        int[][] opt = new int[message.size() + 1][questionPattern.size() + 1];
        for (int i = message.size() - 1; i >= 0; i--) {
            for (int j = questionPattern.size() - 1; j >= 0; j--) {
                if (message.get(i).equals(questionPattern.get(j))
                    || (flatContextsMap.containsKey(questionPattern.get(j)) && flatContextsMap.get(questionPattern.get(j)).contains(message.get(i))))
                    opt[i][j] = opt[i + 1][j + 1] + 1;
                else
                    opt[i][j] = Math.max(opt[i + 1][j], opt[i][j + 1]);
            }
        }
        List<String> result = new ArrayList();
        int i = 0, j = 0;
        while (i < message.size() && j < questionPattern.size()) {
            if (message.get(i).equals(questionPattern.get(j))
                || (flatContextsMap.containsKey(questionPattern.get(j)) && flatContextsMap.get(questionPattern.get(j)).contains(message.get(i)))) {
                result.add(questionPattern.get(j));
                i++;
                j++;
            } else if (opt[i + 1][j] >= opt[i][j + 1]) i++;
            else j++;
        }
//        System.out.println(result.size());
        return (double)result.size()/ (double)Math.max(message.size(),questionPattern.size());
    }

    public static List<Tuple2<Pattern,Double>> patternClassificationMultiple(List<String> words , Map<String,Set<String>> flatContextsMap){
        List<Tuple2<Pattern,Double>> results = new ArrayList<>();
        // calculate and maintain the scores for all the patterns
        List<Tuple2<Pattern,Double>>patternScoreList = patternsForState.stream().map(x->new Tuple2<>(x, SSS(words, x.getQuestionPatternWords(),flatContextsMap))).collect(Collectors.toList());
        // sort the list by score in descending order
        Collections.sort(patternScoreList, (p1, p2) -> p2._2.compareTo(p1._2));
        // add the first matched pattern (with highest score)
        results.add(patternScoreList.get(0));
        double maxScore = patternScoreList.get(0)._2;
        // add all the patterns with the same highest score
        int i = 1;
        int limit = 20;
        while(i < patternScoreList.size() && patternScoreList.get(i)._2 >= maxScore){
            results.add(patternScoreList.get(i));
            i++;
            if(i >= limit){
                break;
            }
        }
        return results;
    }

    public static String replacePattern(String s){
        for(String pattern: changingBookingPatterns){
            if(s.contains(pattern)){
                return s.replace(pattern,"").trim();
            }
        }
        return s;
    }

}
