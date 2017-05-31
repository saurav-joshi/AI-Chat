package com.aibot.state;

import com.aibot.entity.*;
import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.entity.*;
import com.aibot.qa.LibraryUtil;
import com.aibot.recommender.Constants;
import com.aibot.recommender.Recommender;
import com.aibot.entity.*;
import org.apache.commons.collections.map.HashedMap;

import java.util.*;

public class ClarificationState extends State {
    @Override
    public void setStateType() {
        this.stateType = StateType.ClarificationState;
    }

    @Override
    public StateType process(Conversation conversation) {
        LibraryUtil.Pattern matchedPattern = conversation.getLatestQA().getMatchedPattern();
        if(matchedPattern == null || matchedPattern.getQuestionType() == null){
            return StateType.ExpectationManagementState;
        }

        // handle distance clarification
        if(matchedPattern.getQuestionType().stream().filter(x->x.contains("Clarify.Distance")).count() > 0){
            // set the correct distance to
            Map<String,String> otherParas = new HashedMap();
            // add the geo if available
            if(conversation.getLatestQA().getGeo() != null && conversation.getLatestQA().getGeo().trim().length() != 0 ){
                otherParas.put(Constants.API_PARA_GEO, conversation.getLatestQA().getGeo());
            }

            QA lastQAWithSuggestion = conversation.getLatestQAWhichIncludeSuggestion("Clarify.Distance");
            String distanceNum = null;
            if(matchedPattern.getQuestionType().contains("Clarify.Distance")){
                // add the distance
                switch(conversation.getLatestQueston().toLowerCase()){
                    case "up to 500m":
                        distanceNum = "0.5";
                        break;
                    case "up to 1km":
                        distanceNum = "1";
                        break;
                    case "up to 5km":
                        distanceNum = "5";
                        break;
                    default:
                        System.out.println("Cannot recognize the distance: " + conversation.getLatestQueston());
                }
                otherParas.put(Constants.GEO_DISTANCE_TO_FILTER, distanceNum);
            }
            if(lastQAWithSuggestion != null && lastQAWithSuggestion.getCity() != null && lastQAWithSuggestion.getCity().trim().length() != 0){
                otherParas.put(Constants.API_PARA_CITY, lastQAWithSuggestion.getCity());
            }
            // update this questions's entities and question type
            List<EntityExtractionUtil.EntityExtractionResult> currentEntities = conversation.getLatestQA().getEntities();
            for (EntityExtractionUtil.EntityExtractionResult oldEntity : lastQAWithSuggestion.getEntities()) {
                String name = oldEntity.getEntityName();
                if(name.equals("$distance")){
                    continue;
                }
                boolean hasEntity = currentEntities.stream().filter(x->x.getEntityName().equals(name)).count() > 0;
                if(!hasEntity){
                    currentEntities.add(oldEntity);
                }
            }
            if(lastQAWithSuggestion.getEntities().stream().filter(x->x.getEntityName().equalsIgnoreCase("$distance")).count() > 0){
                currentEntities.add(new EntityExtractionUtil.EntityExtractionResult("$distance","up to " + distanceNum + "km"));
            }

            // set the question type
            conversation.getLatestQA().setMatchedPattern(lastQAWithSuggestion.getMatchedPattern());
            // get the results based on the given distance
            RecommenderQuery query = Recommender.createRecommenderQuery(lastQAWithSuggestion.getMatchedPattern().getQuestionType(), lastQAWithSuggestion.getEntities(), otherParas, 1);
            List<RecommenderResultsRestaurant> result = Recommender.getInstance().getRecommendationResults(query,null);
            conversation.getLatestQA().getAnswer().setResultRestaurants(result);

            // set the distance-related result message
            if(result != null && result.size() > 0 && distanceNum != null){
                String messagept = LibraryUtil.getRandomPatternByQuestionClass("Result.Refine.Distance").getSystemMessage();
                String value = null;
                try{
                    value = Double.valueOf(distanceNum) >= 1 ? distanceNum + "km." : String.valueOf((int) Math.round(1000.0*Double.valueOf(distanceNum))) + "m.";;
                }catch (Exception e){
                    System.out.println("Wrong distance value.");
                    e.printStackTrace();
                }
                if(value != null && value.length() > 0){
                    conversation.getLatestQA().getAnswer().setMessage(messagept.replaceAll("@Number", value));
                }
            }
            return StateType.ResultState;
        }
        //handle city clarification
        // handle distance clarification
        if(matchedPattern.getQuestionType().stream().filter(x->x.contains("Clarify.MultiCity")).count() > 0){
            Map<String,String> otherParas = new HashedMap();
            // add the geo if available
            if(conversation.getLatestQA().getGeo() != null && conversation.getLatestQA().getGeo().trim().length() != 0 ){
                otherParas.put(Constants.API_PARA_GEO, conversation.getLatestQA().getGeo());
            }

            QA lastQAWithSuggestion = conversation.getLatestQAWhichIncludeSuggestion("Clarify.MultiCity");
            if(lastQAWithSuggestion != null){
                otherParas.put(Constants.API_PARA_CITY, conversation.getLatestQueston().toLowerCase());
                conversation.getLatestQA().setCity(conversation.getLatestQueston().toLowerCase());
            }
            // update this questions's entities
            List<EntityExtractionUtil.EntityExtractionResult> currentEntities = conversation.getLatestQA().getEntities();
            currentEntities.addAll(lastQAWithSuggestion.getEntities());

            // set the question type
            conversation.getLatestQA().setMatchedPattern(lastQAWithSuggestion.getMatchedPattern());
            // get the results based on the given distance
            RecommenderQuery query = Recommender.createRecommenderQuery(lastQAWithSuggestion.getMatchedPattern().getQuestionType(), lastQAWithSuggestion.getEntities(), otherParas, 1);
            List<RecommenderResultsRestaurant> result = Recommender.getInstance().getRecommendationResults(query,null);
            conversation.getLatestQA().getAnswer().setResultRestaurants(result);
            //check if system refine
            boolean isSystemRefineAnswer = false;
            QA thirdLastQA = conversation.getQaList().size() >= 3 ? conversation.getQaList().get(conversation.getQaList().size() - 3) : null;
            if(thirdLastQA!=null && thirdLastQA.getAnswer().getMessage().equalsIgnoreCase("May I know your location?")){
                isSystemRefineAnswer = true;
            }
            //add distance clarification if nearby search
            if(UserRefineState.isDistanceRequired(currentEntities, conversation, result, isSystemRefineAnswer))
                addDistanceClarification(conversation);

            return StateType.ResultState;
        }
        // // TODO: 10/31/2016 add the other clarification types
        return StateType.ExpectationManagementState;
    }

    public static void addDistanceClarification(Conversation conversation){
        //Set the suggestion, distanceClarification has the highest priority now
        Suggestion currentSuggestion = conversation.getLatestQA().getAnswer().getSuggestion();
        LibraryUtil.Pattern disPattern = LibraryUtil.getRandomPatternByQuestionClass("Clarify.Distance");
        if(currentSuggestion == null){
            conversation.getLatestQA().getAnswer().setSuggestion(new Suggestion(disPattern.getSystemMessage(), Arrays.asList(disPattern.getPatternAtt().trim().split("\\|\\|")), "Clarify.Distance"));
        }else{
            // !currentSuggestion.getType().toLowerCase().equalsIgnoreCase("clarifyPartialMatch")
            conversation.getLatestQA().getAnswer().getSuggestion().setMessage(disPattern.getSystemMessage());
            conversation.getLatestQA().getAnswer().getSuggestion().setOptions(Arrays.asList(disPattern.getPatternAtt().trim().split("\\|\\|")));
            conversation.getLatestQA().getAnswer().getSuggestion().setType("Clarify.Distance");
        }
    }
    @Override
    public void allowedInputStateTypes(){
        allowedInputStateTypes.add(StateType.StartState);
    }

    public boolean guardCheck(Conversation con) {
        return true;
    }
}
