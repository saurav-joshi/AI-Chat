package com.aibot.entityextraction;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import scala.Tuple2;

import java.util.Arrays;
import java.util.List;

public class NameExtraction implements EntityExtraction {
    String name;
    List<String[]> patterns;
    static final  String serializedClassifier = "edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz";
    static AbstractSequenceClassifier classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
    static NameExtraction nameExtraction = null;

    public static NameExtraction getInstance(){
        if(nameExtraction ==null){
            nameExtraction = new NameExtraction();
        }
        return nameExtraction;
    }
    //assume one name in a sentence
    public String getPersonName(String text){
        String person = "";
        List<List<CoreLabel>> out = NameExtraction.getInstance().classifier.classify(text);
        for (List<CoreLabel> sentence : out) {
            for(int i=0; i< sentence.size(); i++){
                CoreLabel word = sentence.get(i);
                if(word.get(AnswerAnnotation.class).equalsIgnoreCase("PERSON")){
                    person = word.word();
                    return person;
                }
            }
        }
        return person;
    }

    //building's number, location in lowercase, location is two/three... words
    public String getLocation(String text){
        String location = "";
        List<List<CoreLabel>> out = NameExtraction.getInstance().classifier.classify(text);
        for (List<CoreLabel> sentence : out) {
            for(int i=0; i< sentence.size(); i++){
                CoreLabel word = sentence.get(i);
                if(word.get(AnswerAnnotation.class).equalsIgnoreCase("LOCATION")){
                    location = word.word();
                    return location;
                }
            }
        }
        return location;
    }

    public String getRestaurant(String text){
        String restaurant = "";
        List<List<CoreLabel>> out = NameExtraction.getInstance().classifier.classify(text);
        for (List<CoreLabel> sentence : out) {
            for(int i=0; i< sentence.size(); i++){
                CoreLabel word = sentence.get(i);
                if(word.get(AnswerAnnotation.class).equalsIgnoreCase("Organization")){
                    restaurant = word.word();
                    return restaurant;
                }
            }
        }
        return restaurant;
    }

    public String getPrice(String text){
        String price = "";
        List<List<CoreLabel>> out = NameExtraction.getInstance().classifier.classify(text);
        for (List<CoreLabel> sentence : out) {
            for(int i=0; i< sentence.size(); i++){
                CoreLabel word = sentence.get(i);
                if(word.get(AnswerAnnotation.class).equalsIgnoreCase("Money")){
                    price = word.word();
                    return price;
                }
            }
        }
        return price;
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

    @Override
    public SetMultimap<String[], Integer> searchPatternToPosIndex(String[] text) {
        SetMultimap<String[], Integer> result = HashMultimap.create();
        List<List<CoreLabel>> out = NameExtraction.getInstance().classifier.classify(String.join(" ", Arrays.asList(text)).replaceAll("\\p{P}", ""));
        int index = 0;
        for (List<CoreLabel> sentence : out) {
            for(int i=0; i< sentence.size(); i++){
                CoreLabel word = sentence.get(i);
                if(word.get(AnswerAnnotation.class).equalsIgnoreCase("PERSON")){
                    String person = "";
                    if(i < sentence.size() -1 && sentence.get(i+1).get(AnswerAnnotation.class).equals("PERSON")){
                        CoreLabel word1 = sentence.get(i+1);
                        if(word1.get(AnswerAnnotation.class).equals("PERSON")){
                            person = word.word() + " " + word1.word();
                            index = i;
                        }
                        i++;
                    }else{
                        person = word.word();
                        index = i;
                    }
                    result.put(person.split(" "),index);
                }
            }
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
}
