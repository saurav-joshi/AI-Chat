package com.aibot.dao;

import com.aibot.entity.*;
import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.qa.GlobalConstants;
import com.aibot.entity.*;

import com.aibot.entity.*;

import com.aibot.state.State;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.map.HashedMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class QADao {
    static SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    static int totalColumns = 35;//end index of entities column
    static Map<String, Integer> mappingColumn = new HashedMap();
    private static QADao qaDao = null;
    private static final String tableName = "qa";

    /*
    Schema:
    1-qa_id              INT,
	2-conversation_id     INT,
	3-question      TEXT,
	4-answer	TEXT,
	5-choice TEXT,
	6-TIMESTAMP TEXT,
	7-$actionword TEXT, //start index of entities column
    8-$bevarage TEXT,
    9-$chef TEXT,
    10-$cookingmethod TEXT,
    11-$country TEXT,
    12-$dish TEXT,
	13-$establishmentype TEXT,
	14-$EVENT TEXT,
	15-$ingredient TEXT,
    16-$location TEXT,
	17-$mealtype TEXT,
	18-$nationality TEXT,
    19-$refineestablishmentype TEXT,
    20-$refinelocation TEXT,
    21-$religious TEXT,
    22-$restaurantfeature TEXT,
    23-$accompany TEXT,
    24-$occasion TEXT,
    25-$regular TEXT,
    26-$retaurantname TEXT,
	27-location TEXT,
    28-cuisine	TEXT,
	29-restaurantentity TEXT
	30-$pricerange TEXT,
	31-$offer TEXT,
	32-$accolade TEXT,
	33-$regional TEXT,
	34-$distance TEXT,
	35-$city TEXT,
	36-geo TEXT,
	37-library_id int,
	38-states TEXT
	39-suggestion TEXT,
	40-originalQuestion TEXT,
	41-city TEXT
     */

    static {
        int index = 7;
        for (GlobalConstants.Entity entity : GlobalConstants.Entity.values()) {
            String columnName = entity.name();
            mappingColumn.put(columnName, index++);
        }
    }

    public static QADao getInstance() {
        if (qaDao == null) {
            qaDao = new QADao();
        }
        return qaDao;
    }

    public List<QA> getQAByConversation(int conversation_id) {
        List<QA> qaList = new ArrayList<>();
        String query = "SELECT * FROM " + tableName + " WHERE conversation_id =" + conversation_id;
        try {
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                QA qa = new QA();
                int qaId = rs.getInt("qa_id");
                String question = rs.getString("question");
                String originalQuestion = rs.getString("originalQuestion");
                qa.setId(qaId);
                qa.setQuestion(question);
                qa.setOriginalQuestion(originalQuestion);
                qa.setConversation_id(conversation_id);
                //city
                String city = rs.getString("city");
                qa.setCity(city);

                String message = rs.getString("answer");
                List<RecommenderResultsRestaurant> resultsRestaurants = new ArrayList<>();
                String choice = rs.getString("choice").trim();
                if(!choice.isEmpty()){
                    String[] choices = choice.split("\\|\\|");
                    for (String choiceId : choices) {//choice id
                        resultsRestaurants.add(new RecommenderResultsRestaurant(choiceId));
                    }
                }

                // get the suggestion here
                String suggestionStr = rs.getString("suggestion");
                Suggestion suggestion = null;
                if(suggestionStr != null && suggestionStr.trim().length() !=0){
                    Map<String, Object> suggestionMap = new HashMap<>();
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        suggestionMap = mapper.readValue(suggestionStr, Map.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String suggestionMessage = (String) suggestionMap.get("message");
                    List<String> suggestionOption = (List<String>) suggestionMap.get("options");
                    String suggestionType = (String) suggestionMap.get("type");
                    suggestion = new Suggestion(suggestionMessage,suggestionOption,suggestionType);
                }

                Answer answer = new Answer();
                answer.setMessage(message);
                answer.setResultRestaurants(resultsRestaurants);
                answer.setSuggestion(suggestion);
                qa.setAnswer(answer);

                String timestamp = rs.getString("timestamp");
                qa.setTime(dateFormat.parse(timestamp));

                List<EntityExtractionUtil.EntityExtractionResult> entityExtractionResults = new ArrayList<>();

                Map<String, List<String>> mapEntites = new HashedMap();
                for (GlobalConstants.Entity entity : GlobalConstants.Entity.values()) {
                    String value = rs.getString(entity.name());
                    String name = entity.name();
                    if (entity.name().equalsIgnoreCase(GlobalConstants.Entity.location.name())
                            || entity.name().equalsIgnoreCase(GlobalConstants.Entity.cuisine.name())
                            || entity.name().equalsIgnoreCase(GlobalConstants.Entity.restaurantentity.name())) {
                        name = "#" + entity.name();
                    }
                    if(value!=null && !value.isEmpty()){
                        mapEntites.put(name, Arrays.asList(value.split("\\|\\|")));
                    }
                }

                mapEntites.forEach((name,value) -> {
                    if(value!=null && !value.isEmpty()){
                        value.forEach(entity -> {
                            EntityExtractionUtil.EntityExtractionResult entityExtractionResult = new EntityExtractionUtil.EntityExtractionResult(name, entity);
                            entityExtractionResults.add(entityExtractionResult);
                        });
                    }
                });
                qa.setEntities(entityExtractionResults);
                qa.setGeo(rs.getString("geo"));
                qa.setMatchedPatternIdInLibrary(rs.getInt("library_id"));
                //System.out.println("Library: " + rs.getInt("library_id"));
                //state
                List<State.StateType> stateTypes = new ArrayList<>();
                String[] states = rs.getString("states").split(",");
                if(states!=null){
                    for (String state : states) {
                        State.StateType stateType = getState(state);
                        if(stateType!=null){
                            stateTypes.add(stateType);
                        }
                    }
                }
                qa.setStatePaths(stateTypes);
                qaList.add(qa);
            }
            ps.close();
            connection.close();
        } catch (Exception e) {
            System.out.println("Error getting Q/A: " + e.getMessage());
            e.printStackTrace();
        }
        return qaList;
    }

    public List<QA> getQACurrentSession(int conversation_id) {
        List<QA> qaCurrentSession = new ArrayList<>();
        try {
            qaCurrentSession = getQACurrentSession(getQAByConversation(conversation_id));
        } catch (Exception e) {
            System.out.println("Error getting Q/A current session: " + e.getMessage());
            e.printStackTrace();
        }
        return qaCurrentSession;
    }

    public List<QA> getQACurrentSession(List<QA> qaList) {
        List<QA> qaCurrentSession = new ArrayList<>();
        try {
            //get latest session only
            int index = qaList.size()-1;
            for(index= qaList.size()-1; index>0; index=index-1){
                QA q1 = qaList.get(index);
                QA q2 = qaList.get(index-1);
                long duration = q1.getTime().getTime() - q2.getTime().getTime();
                long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration);
                if(diffInMinutes<GlobalConstants.sessionInterval) continue;
                else {
                    break;
                }
            }
            //get from the index
            for(int i = index; i< qaList.size(); i++){
                qaCurrentSession.add(qaList.get(i));
            }
        } catch (Exception e) {
            System.out.println("Error getting Q/A: " + e.getMessage());
            e.printStackTrace();
        }
        return qaCurrentSession;
    }

    public void insertListQA(Conversation conversation) {
        try {
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement("INSERT INTO " + tableName +
                    " VALUES( " + "?, ?, ?,?,?, ?,?,?, ?,?,?, ?,?,?, ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            int conversationId = conversation.getId();

            //insert the newest

            QA qa = conversation.getLatestQA();
            int qaId = qa.getId();
            String question = qa.getQuestion();
            String answer = qa.getAnswer().getMessage();
            List<String> restaurantIds = new ArrayList<>();
            List<RecommenderResultsRestaurant> results = qa.getAnswer().getResultRestaurants();
            if(results != null)
              results.forEach(r -> {
                restaurantIds.add(r.getId());
              });
            String choice = String.join("||", restaurantIds); //choice: list of restaurant id
            String suggestion = qa.getAnswer().getSuggestion() == null ? null : qa.getAnswer().getSuggestion().toString();
            String timestamp = dateFormat.format(qa.getTime());

            ps.setInt(1, qaId);
            ps.setInt(2, conversationId);
            ps.setString(3, question);
            ps.setString(4, answer);
            ps.setString(5, choice);
            ps.setString(6, timestamp);

            int index = 7;
            //Default value
            for (int i = 7; i <= totalColumns; i++) {
                ps.setString(i, "");
            }
            List<EntityExtractionUtil.EntityExtractionResult> entities = qa.getEntities();
            Map<String, List<String>> mapEntites = new HashedMap();
            if (entities != null) {
                for (EntityExtractionUtil.EntityExtractionResult entity : entities) {
                    String name = entity.getEntityName().replace("#", "");
                    String value = String.join(" ", entity.getEntityValue());
                    //ps.setString(mappingColumn.get(name), value);
                    List<String> values = mapEntites.get(name);
                    if (values == null) {
                        values = new ArrayList<>();
                    }
                    values.add(value);
                    mapEntites.put(name, values);
                }
            }
            mapEntites.forEach((name, value) ->{
                try{
                    if(mappingColumn.containsKey(name)){
                        ps.setString(mappingColumn.get(name), String.join("||",value));
                    }
                }catch (SQLException e){
                }
            });
            ps.setString(36, qa.getGeo()); //add geo passed via API
            ps.setInt(37, qa.getMatchedPatternIdInLibrary()); //add matched libary id
            ps.setString(38, "");// add state path
            if(!qa.getStatePaths().isEmpty()){
                StringBuilder sb = new StringBuilder();
                qa.getStatePaths().stream().forEach(stateType -> {
                    sb.append(",").append(stateType.name());
                });
                ps.setString(38, sb.toString().substring(1));
            }

            ps.setString(39, suggestion);
            ps.setString(40, qa.getOriginalQuestion());
            ps.setString(41, qa.getCity());
            ps.execute();
            ps.close();
            connection.close();

        } catch (Exception e) {
            System.out.println("Error updating qa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private State.StateType getState(String state){
        for (State.StateType stateType : State.StateType.values()) {
            if(stateType.name().equalsIgnoreCase(state)){
                return stateType;
            }
        }
        return null;
    }

}
