package com.aibot.entityextraction;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import org.json.JSONArray;
import org.json.JSONObject;
import scala.Tuple2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class LocationExtraction implements EntityExtraction {
    String name;
    List<String[]> patterns;
    static final  String serializedClassifier = "edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz";
    static AbstractSequenceClassifier classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);
    static LocationExtraction locationExtraction = null;

    public static LocationExtraction getInstance(){
        if(locationExtraction ==null){
            locationExtraction = new LocationExtraction();
        }
        return locationExtraction;
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
        List<List<CoreLabel>> out = LocationExtraction.getInstance().classifier.classify(String.join(" ", Arrays.asList(text)).replaceAll("\\p{P}", ""));
        int index = 0;
        for (List<CoreLabel> sentence : out) {
            for(int i=0; i< sentence.size(); i++){
                CoreLabel word = sentence.get(i);
                if(word.get(AnswerAnnotation.class).equalsIgnoreCase("LOCATION")){
                    String person = "";
                    if(i < sentence.size() -1 && sentence.get(i+1).get(AnswerAnnotation.class).equals("LOCATION")){
                        CoreLabel word1 = sentence.get(i+1);
                        if(word1.get(AnswerAnnotation.class).equals("LOCATION")){
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

    public static String getAddress(String latitude, String longitude){
        String query = new StringBuilder("http://maps.googleapis.com/maps/api/geocode/json?latlng=")
                .append(latitude).append(",").append(longitude).append("&sensor=true").toString();
        String address = "";
        try {
            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output = "", full = "";
            while ((output = br.readLine()) != null) {
                full += output;
            }
            conn.disconnect();
            JSONObject json = new JSONObject(full);
            JSONArray jarr = json.getJSONArray("results");
            json = jarr.getJSONObject(0);
            address = json.getString("formatted_address");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return address;
    }
}
