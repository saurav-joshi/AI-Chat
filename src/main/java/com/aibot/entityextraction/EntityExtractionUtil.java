package com.aibot.entityextraction;

import com.aibot.qa.GlobalConstants;
import com.aibot.qa.S3Handler;
import com.aibot.qa.SynonymMappingAndLemmatization;
import com.aibot.utils.StringUtils;
import com.google.common.collect.SetMultimap;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.FilenameUtils;
import scala.Tuple2;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityExtractionUtil {
    boolean removeOverlaps;
    boolean caseInsensitive;
    boolean lemmatizePatterns;

    public static List<EntityExtraction> entityExtractionList;
    public static Set<String> ignoredPatternsForLemmatization;

    public static class EntityExtractionResult{
        private String entityName;
        private String[] entityValue;
        private Integer startIndex;
        private Integer endIndex;
        private String[] source;
        private Double matchScore;

        public EntityExtractionResult(){

        }

        public Double getMatchScore() {
            return matchScore;
        }

        public void setMatchScore(Double matchScore) {
            this.matchScore = matchScore;
        }

        public EntityExtractionResult(String entityName, String entityValue){
            this.entityName = entityName;
            this.entityValue = entityValue.split("\\s+");
        }

        public EntityExtractionResult(String entityName, String[] entityValue, Integer index) {
            this.entityName = entityName;
            this.entityValue = entityValue;
            this.startIndex = index;
            this.endIndex = index + entityValue.length - 1;
        }

        public EntityExtractionResult(String entityName, String[] entityValue, Integer index, String[] source) {
            this.entityName = entityName;
            this.entityValue = entityValue;
            this.startIndex = index;
            this.endIndex = index + source.length - 1;
            this.source = source;
        }

        public EntityExtractionResult(String entityName, String[] entityValue, Integer index, String[] source, double matchScore) {
            this.entityName = entityName;
            this.entityValue = entityValue;
            this.startIndex = index;
            this.endIndex = index + source.length - 1;
            this.source = source;
            this.matchScore = matchScore;
        }

        public String[] getSource() {
            return source;
        }

        public void setSource(String[] source) {
            this.source = source;
        }

    public String getEntityName() {
        return entityName;
    }

    public String[] getEntityValue() {
        return entityValue;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public Integer getEndIndex() {
        return endIndex;
    }

    @Override
    public String toString() {
        return entityName+":"+String.join(" ",entityValue)+",("+startIndex+","+endIndex+")";
    }
}

    public EntityExtraction getEntityExtractionByName(String name){
        return  entityExtractionList.stream().filter(e -> e.getName().equals(name)).findFirst().get();
    }

    public EntityExtractionUtil(Set<String> ignoredPatternsForLemmatization){
        entityExtractionList = new ArrayList<>();
        this.ignoredPatternsForLemmatization = ignoredPatternsForLemmatization;
    }

    public EntityExtractionUtil loadEntityExtractions(){
        try {
            System.out.println("EntityExtractionUtil ==>> removeOverlaps: "+removeOverlaps+" , caseInsensitive: "+caseInsensitive+" , lemmatizePatterns: "+lemmatizePatterns);
            List<String> dictionaries = S3Handler.getFilesInFolder(GlobalConstants.taxonomiesBucketNameS3, GlobalConstants.taxonomiesFolderNameS3);
            for (String fileKey : dictionaries) {
                if (fileKey.split("/").length <= 1)
                    continue;
                String[] nameParts = fileKey.split("/")[1].split("_");
                String name = FilenameUtils.removeExtension(String.join(" ", Arrays.copyOfRange(nameParts, 1, nameParts.length)));
                List<String> dictLines = S3Handler.readLinesFromFile(GlobalConstants.taxonomiesBucketNameS3, fileKey);
                EntityExtraction entityExtraction = buildEntityExtraction(name,dictLines.subList(1, dictLines.size()));
                entityExtractionList.add(entityExtraction);
            }
            /*EntityExtraction personExtraction = NameExtraction.getInstance();
            personExtraction.setEntityName("PersonName");
            entityExtractionList.add(personExtraction);*/

            EntityExtraction dateExtraction = DateExtraction.getInstance();
            dateExtraction.setEntityName("Date");
            entityExtractionList.add(dateExtraction);

            EntityExtraction emailExtraction = EmailExtraction.getInstance();
            emailExtraction.setEntityName("Email");
            entityExtractionList.add(emailExtraction);

            EntityExtraction phoneExtraction = PhoneNumberExtraction.getInstance();
            phoneExtraction.setEntityName("Phone");
            entityExtractionList.add(phoneExtraction);

            EntityExtraction paxExtraction = PaxExtraction.getInstance();
            paxExtraction.setEntityName("Pax");
            entityExtractionList.add(paxExtraction);

            EntityExtraction distanceExtraction = DistanceExtraction.getInstance();
            distanceExtraction.setEntityName("Distance");
            entityExtractionList.add(distanceExtraction);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public EntityExtractionUtil removeOverlaps(){
        removeOverlaps = true;
        return this;
    }

    public EntityExtractionUtil caseInsensitive(){
        caseInsensitive = true;
        return this;
    }

    public EntityExtractionUtil lemmatizePatterns(){
        lemmatizePatterns = true;
        return this;
    }

    public List<EntityExtractionResult> extractEntity(String[] text) {
        //Tuple2<Integer, Integer> scope = NegationDetector.getInstance().negationScope(text);
        List<EntityExtractionResult> results = entityExtractionList.stream().flatMap(entityExtraction ->
                entityExtraction
                        .searchPatternToPosIndex(text)
                        .entries().stream()
                        //.filter(x -> !NegationDetector.getInstance().isNegation(text, scope, x.getKey()))
                        .map(entry -> new EntityExtractionResult(entityExtraction.getName(), entry.getKey(), entry.getValue()))
        ).map(x -> (EntityExtractionResult) x).collect(Collectors.toList());
        return removeOverlaps ? mergeIntervals(results) : results;
    }

    public List<EntityExtractionResult> extractEntityPartial(String[] text){
        //Tuple2<Integer, Integer> scope = NegationDetector.getInstance().negationScope(text);
        List<EntityExtractionResult> results = entityExtractionList.stream()
                .filter(x -> !x.getName().contains("restaurantentity"))
                .flatMap(entityExtraction ->{
                    if(!entityExtraction.isPartialMatch()){
                        return entityExtraction.searchPatternToPosIndex(text)
                                .entries().stream()
                                .map(entry -> new EntityExtractionResult(entityExtraction.getName(), entry.getKey(), entry.getValue(), entry.getKey(),entry.getKey().length));

                    }
                    return entityExtraction.searchPartial(text)
                    .entries().stream()
                    .map(entry -> {
                        if(entry.getKey().equals(entry.getValue()._2())){//exact match, the score is the length of keywords
                            return new EntityExtractionResult(entityExtraction.getName(),
                                    entry.getKey(),
                                    entry.getValue()._1,
                                    entry.getValue()._2(),
                                    entry.getKey().length)
                                    ;
                        }
                        return new EntityExtractionResult(entityExtraction.getName(), //partial match, score is jaccard similarity
                            entry.getKey(),
                            entry.getValue()._1,
                            entry.getValue()._2(),
                            StringUtils.getJaccardSimilarity(entry.getKey(),text))
                            ;}
                    );
        }).map(x-> (EntityExtractionResult)x).collect(Collectors.toList());
        return removeOverlaps ? mergeIntervals(results) : results;
    }

    public static List<EntityExtractionResult> mergeIntervals(List<EntityExtractionResult> intervals) {
        List<EntityExtractionResult> result = new ArrayList<>();
        if(intervals==null||intervals.size()==0) return result;
        // sort starting points in increasing order , sort ending points in decreasing order
        Collections.sort(intervals, (i1, i2) -> i1.startIndex != i2.startIndex ? i1.startIndex - i2.startIndex : i2.endIndex - i1.endIndex);
        EntityExtractionResult pre = intervals.get(0);
        for (EntityExtractionResult curr : intervals) {
            if (pre.endIndex < curr.startIndex) {
                result.add(pre);
                pre = curr;
            }
        }
        result.add(pre);
        return result;
    }

    public static List<EntityExtractionResult> choosePartialMatch(List<EntityExtractionResult> intervals) {
        System.out.println(intervals);
        List<EntityExtractionResult> result = new ArrayList<>();
        if(intervals==null||intervals.size()==0) return result;
        // sort starting points in increasing order , sort match scores in decreasing order
        Collections.sort(intervals, (i1, i2) -> {
            if(i1.getStartIndex() != i2.getStartIndex()){
                return i1.getStartIndex().compareTo(i2.getStartIndex());
            }else {
                return i2.getMatchScore().compareTo(i1.getMatchScore());
            }

        });
        System.out.println(intervals);
        EntityExtractionResult pre = intervals.get(0);
        for (EntityExtractionResult curr : intervals) {
            if ((pre.endIndex < curr.startIndex)){// || (pre.endIndex == curr.startIndex) & !(pre.getEntityName().equals(curr.getEntityName()))) { //keep disambiguated entities
                if(pre.getMatchScore()>0.5){
                    result.add(pre);
                }
                pre = curr;
            }
            /*if(! (curr.getEntityName().contains("regular") || curr.getEntityName().contains("accompany") || curr.getEntityName().contains("occasion"))){
                //those are not restaurant's entities, we keep
                result.add(curr);
            }*/
        }
        if(pre.getMatchScore()>0.5){
            result.add(pre);
        }

        return result;
    }

    public static Map<String, Set<String>> getCandidateMap (List<EntityExtractionResult> entityExtractionResults) {
        Map<String, Set<String>> results = new HashedMap();
        entityExtractionResults.stream().forEach(x -> {
            if(x.getSource()!=null & x.getMatchScore()>0.5){
                String key = String.join(" ",x.getSource());
                String value = String.join(" ",x.getEntityValue());
                Set<String> set = results.getOrDefault(key, new HashSet<>());
                set.add(value);
                results.put(key,set);
            }
        });
        return results;
    }

    public EntityExtraction buildEntityExtraction(String entityName, List<String> lines){
        List<String[]> patterns = new ArrayList<>();
        lines.stream()
            .forEach(line -> {
                String[] splitWords = line.split("\\t");
                List<String> rawPatterns = new ArrayList<>();
                for (String splitWord : splitWords){
                    // each column here may have values separate by ||
                    String [] words = splitWord.split("\\|\\|");
                    for (String word : words){
                        if (word.trim().length() > 0)
                            rawPatterns.add(word.trim());
                    }
                }

                for (int i = 0; i < rawPatterns.size(); i++) {
                    List<String> pattern = null;

                    if(caseInsensitive)
                        pattern = Stream.of(rawPatterns.get(i)).map(String::toLowerCase).collect(Collectors.toList());

                    if(lemmatizePatterns && lines.size() < 1000000 )
                    {
                        pattern = Arrays.asList(SynonymMappingAndLemmatization.run(String.join(" ",pattern)).split("\\s+"));
                    }
                    patterns.add(pattern.stream().toArray(String[]::new));
                }
            });
        EntityExtraction ee = new EntityExtraction() {
            private String name;
            private ACTrie acTrie;
            public EntityExtraction setPatterns(List<String[]> patterns) {
                List<String[]> uniquePatterns = patterns.stream().map(x-> String.join(" ", x)).collect(Collectors.toSet()).stream().map(x -> x.split("\\s+")).collect(Collectors.toList());
                acTrie = new ACTrie<>(uniquePatterns, name);
                return this;
            }

            @Override
            public boolean isPartialMatch() {
               if(entityName.toLowerCase().contains("restaurantname"))
                    return true;
               return false;
            }

            public SetMultimap<String[], Integer> searchPatternToPosIndex(String[] text) {
                return acTrie.searchPatternToPosStartIndex(text);
            }

            public SetMultimap<String[], Tuple2<Integer, String[]>> searchPartial(String[] text) {
                return acTrie.searchPartial(text);
            }

            public EntityExtraction setEntityName(String entityName) {
                this.name = entityName;
                return this;
            }
            public String getName() {
                return name;
            }
            public List<String[]> getPatterns(){
                if(acTrie.getPatterns() == null || acTrie.getPatterns().size() == 0){
                    throw new IllegalArgumentException(" No patterns in the EntityExtraction of "+ name);
                }
                return acTrie.getPatterns();
            }
        }.setEntityName(entityName).setPatterns(patterns);
        return ee;
    }

    public List<EntityExtraction> getEntityExtractionList() {
        return entityExtractionList;
    }

    public void setEntityExtractionList(List<EntityExtraction> entityExtractionList) {
        this.entityExtractionList = entityExtractionList;
    }

    public static String formatOfPax(Tuple2<Integer, Integer> pax) {
        int numOfAdult = pax._1();
        int numOfKid = pax._2();

        if (numOfKid == 0) {
            return new StringBuilder(String.valueOf(numOfAdult)).append(convertPax(numOfAdult,false)).toString();
        } else {
            if (numOfAdult != 0)
                return new StringBuilder(String.valueOf(numOfAdult)).append(convertPax(numOfAdult,false))
                        .append(". ").append(String.valueOf(numOfKid)).append(convertPax(numOfKid,true)).toString();
        }
        return "";
    }

    private static String convertPax(int number, boolean kid){
        String str ="";
        if(kid){
            if (number== 1)
                str =  " child";
            if (number > 1)
                str = " children";
        }else {
            if (number== 1)
                str =  " adult";
            if (number > 1)
                str = " adults";
        }
        return str;
    }

    public static Tuple2<Integer, Integer> getNumberOfPax(String text){
        int numOfAdult = 0;
        int numOfKid = 0;

        Pattern numberPattern = Pattern.compile("(^|\\s+)\\d+");
        //child pattern: \d (child)
        String childRegex = "\\d+\\s*(child)|\\d+\\s*(children)|\\d+\\s*(kid[s]?)";
        Pattern childPattern = Pattern.compile(childRegex);
        try{
            if (text.equalsIgnoreCase("only me") || text.equalsIgnoreCase("me") || text.equalsIgnoreCase("just me")) {
                numOfAdult = 1;//adult
            }
            if (text.equalsIgnoreCase("couple")) {
                numOfAdult = 2;//adult
            }

            //child
            Matcher childMatcher = childPattern.matcher(text);
            if (childMatcher.find()) {
                numOfKid += Integer.parseInt(childMatcher.group().replaceAll("[a-z]+","").trim());
                text = text.replaceAll(childRegex, "");
            }
            //adult
            Matcher matcher = numberPattern.matcher(text);
            while (matcher.find()) {
                numOfAdult += Integer.parseInt(matcher.group().replaceAll("[a-z]+","").trim());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return new Tuple2(numOfAdult,numOfKid);
    }

    public static int getPosition(String[] wordsFromText, String[] words){
        int position = -1;
        for(int i=0; i <= wordsFromText.length-words.length; i++){
            for(int j =0; j < words.length; j++){
                if(!wordsFromText[j+i].equalsIgnoreCase(words[j])){
                    break;
                }
                if(j==words.length-1){
                    return i;
                }
            }
        }
        return position;
    }

    public static int punctBefore(int i, Map<Integer, String> punctTracking){
        int count = 0;
        Iterator<Integer> iter = punctTracking.keySet().iterator();
        while (iter.hasNext()){
            if(iter.next() <= i){
                count++;
                i++;
            }
        }
        return count;
    }
}
