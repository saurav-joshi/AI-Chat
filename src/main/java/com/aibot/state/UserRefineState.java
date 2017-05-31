package com.aibot.state;

import com.aibot.entity.Conversation;
import com.aibot.entity.QA;
import com.aibot.entity.RecommenderQuery;
import com.aibot.entity.RecommenderResultsRestaurant;
import com.aibot.entityextraction.DistanceExtraction;
import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.entityextraction.LocationFromAddress;
import com.aibot.qa.FlowManagement;
import com.aibot.qa.LibraryUtil;
import com.aibot.recommender.Constants;
import com.aibot.recommender.Recommender;
import com.aibot.entity.*;
import org.apache.commons.collections.map.HashedMap;

import java.util.*;
import java.util.stream.Collectors;

public class UserRefineState extends State {

    @Override
    public void setStateType() {
        this.stateType = State.StateType.UserRefineState;
    }

    @Override
        public State.StateType process(Conversation conversation){

        List<StateType> stateTypes = Arrays.asList(StateType.ConsumerQueryState, stateType.UserRefineState);
        QA lastQA = conversation.getLatestQAWhichIncludeStateTypes(stateTypes,true,true,Arrays.asList(StateType.ShowMoreState,StateType.ClarificationState));

        if(lastQA == null) {
         // consumer query but classify into user refine query wrongly.
            if(conversation.getLatestQA().getEntities() != null && conversation.getLatestQA().getEntities().size() != 0){
                conversation.getLatestQA().setMatchedPatternIdInLibrary(20);
                return StateType.ConsumerQueryState;
            }else{
                return StateType.ExpectationManagementState;
            }
        }

        LibraryUtil.Pattern lastPattern = lastQA.getMatchedPattern();
        LibraryUtil.Pattern currentPattern = conversation.getLatestQA().getMatchedPattern();
        List<EntityExtractionUtil.EntityExtractionResult> currentEntities = conversation.getLatestQA().getEntities();

        //if refine offer
        if(currentPattern.getQuestionType().contains("UserRefine.Offer") || currentPattern.getQuestionType().contains("UserRefine.Attribute.SameClass")) {
            currentEntities.addAll(lastQA.getEntities());
        }

        //if refine price
        if(currentPattern.getQuestionType().contains("UserRefine.Price")) {
            Arrays.stream(currentPattern.getPatternAtt().split(",")).forEach(x->{
                EntityExtractionUtil.EntityExtractionResult e = new EntityExtractionUtil.EntityExtractionResult("$pricerange",x.split("\\s+"), -1);
                currentEntities.add(e);
            });
            // merge the entities instead of add all directly, add all the ones which is not in the currentQA's entity list
            List<EntityExtractionUtil.EntityExtractionResult> newEntitiesToAdd = new ArrayList<>();
            for(EntityExtractionUtil.EntityExtractionResult oldEntity : lastQA.getEntities()){
                if(currentEntities.stream().filter(x->x.getEntityName().equals(oldEntity.getEntityName())).count() <= 0){
                    newEntitiesToAdd.add(oldEntity);
                }
            }
            currentEntities.addAll(newEntitiesToAdd);
        }

        //refine attribute
        if(currentPattern.getQuestionType().contains("UserRefine.Attribute")){
            List<EntityExtractionUtil.EntityExtractionResult> newEntitiesToAdd = new ArrayList<>();
            for (EntityExtractionUtil.EntityExtractionResult oldEntity : lastQA.getEntities()) {
                String name = oldEntity.getEntityName();
                if(name.equals("$location") || name.equals("#location")){
                    boolean flag1 = currentEntities.stream().filter(x->x.getEntityName().equals("$location")).count() > 0;
                    boolean flag2 = currentEntities.stream().filter(x->x.getEntityName().equals("#location")).count() > 0;
                    if(flag1 || flag2) continue;
                }
                if(name.equals("$nationality") || name.equals("#cuisine") || name.equalsIgnoreCase("$dish")){
                    boolean flag1 = currentEntities.stream().filter(x->x.getEntityName().equals("$nationality")).count() > 0;
                    boolean flag2 = currentEntities.stream().filter(x->x.getEntityName().equals("#cuisine")).count() > 0;
                    boolean flag3 = currentEntities.stream().filter(x->x.getEntityName().equals("$dish")).count() > 0;
                    if(flag1 || flag2 || flag3) continue;
                }
                boolean flag = currentEntities.stream().filter(x->x.getEntityName().equals(name)).count() > 0;
                if(!flag){
                    newEntitiesToAdd.add(oldEntity);
                }
            }
            currentEntities.addAll(newEntitiesToAdd);
        }

        // refine location: nearer, too far
        Map<String,String> otherParas = new HashedMap();
        if(currentPattern.getQuestionType().contains("UserRefine.Location")){
            //otherParas = new HashedMap();

            // find the last (not including the current one) refine.Location's distance if available, and calculate the new distance
            QA targetQA = null;
            List<QA> qalist = conversation.getQaList();
            ListIterator li = qalist.listIterator(qalist.size()-1);
            while(li.hasPrevious()) {// Iterate in reverse.
                QA preQa = (QA) li.previous();

                if(preQa.getEntities().stream().filter(x->x.getEntityName().contains("$distance")).count() > 0){
                    targetQA = preQa;
                    break;
                }
                if(preQa == lastQA){
                    break;
                }
            }

            if(targetQA == null){ // this is the first refine.location, use the current distance
                otherParas.put(Constants.GEO_DISTANCE_TO_FILTER, currentPattern.getPatternAtt());
                currentEntities.add(new EntityExtractionUtil.EntityExtractionResult("$distance", "up to " + currentPattern.getPatternAtt() + "km"));
            }else{ // this is not the first refine location, so use half of the last refine.location's distance
                EntityExtractionUtil.EntityExtractionResult distanceEntity = targetQA.getEntities().stream().filter(x->x.getEntityName().contains("$distance")).collect(Collectors.toList()).get(0);
                // default distance
                float newDistance = Float.valueOf(currentPattern.getPatternAtt());
                // halve the previous distance
                try{
                    newDistance = Float.valueOf(String.join(" ", distanceEntity.getEntityValue())) * 0.5f;
                }catch (Exception e){
                    String temp = DistanceExtraction.getInstance().getFormattedDistanceFromText(String.join(" ", distanceEntity.getEntityValue()));
                    if(temp != null){
                        newDistance = Float.valueOf(temp) * 0.5f;
                    }
                }
                currentEntities.add(new EntityExtractionUtil.EntityExtractionResult("$distance", "up to " + String.valueOf(newDistance) + "km"));
                otherParas.put(Constants.GEO_DISTANCE_TO_FILTER, String.valueOf(newDistance));
            }

            // merge the entities
            List<EntityExtractionUtil.EntityExtractionResult> newEntitiesToAdd = new ArrayList<>();
            for (EntityExtractionUtil.EntityExtractionResult oldEntity : lastQA.getEntities()) {
                String name = oldEntity.getEntityName();
                if(name.equals("$location") || name.equals("#location")){
                    boolean flag1 = currentEntities.stream().filter(x->x.getEntityName().equals("$location")).count() > 0;
                    boolean flag2 = currentEntities.stream().filter(x->x.getEntityName().equals("#location")).count() > 0;
                    if(flag1 || flag2) continue;
                }
                boolean hasEntity = currentEntities.stream().filter(x->x.getEntityName().equals(name)).count() > 0;
                if(!hasEntity){
                    newEntitiesToAdd.add(oldEntity);
                }
            }
            currentEntities.addAll(newEntitiesToAdd);
        }

        if(currentPattern.getQuestionType().contains("UserRefine.Distance")){
            System.out.println("--Refine distance");
            //otherParas = new HashedMap();
            String distanceNum = null;
            // get the distance of the indicated in the query sentence
            List<EntityExtractionUtil.EntityExtractionResult> distanceEntities = conversation.getLatestEntities().stream().filter(x -> x.getEntityName().contains("$distance")).collect(Collectors.toList());
            if (distanceEntities.size() > 0) {
                distanceNum = DistanceExtraction.getInstance().getFormattedDistanceFromText(String.join(" ", distanceEntities.get(0).getEntityValue()));
                if(distanceNum != null){
                    otherParas.put(Constants.GEO_DISTANCE_TO_FILTER, distanceNum);
                }
            }
            System.out.println(">> refine distance to filter to:" + otherParas.get(Constants.GEO_DISTANCE_TO_FILTER));
            // merge the entities
            List<EntityExtractionUtil.EntityExtractionResult> newEntitiesToAdd = new ArrayList<>();
            for (EntityExtractionUtil.EntityExtractionResult oldEntity : lastQA.getEntities()) {
                boolean hasEntity = currentEntities.stream().filter(x->x.getEntityName().equals(oldEntity.getEntityName())).count() > 0;
                if(!hasEntity){
                    newEntitiesToAdd.add(oldEntity);
                }
            }
            currentEntities.addAll(newEntitiesToAdd);
        }

        conversation.getLatestQA().setEntities(currentEntities);
        conversation.getLatestQA().setMatchedPattern(lastPattern);

        if(FlowManagement.getStateMap().get(StateType.SystemRefineState).guardCheck(conversation)){
            return StateType.SystemRefineState;
        }

        if(conversation.getLatestQA().getGeo() != null && conversation.getLatestQA().getGeo().trim().length() != 0 ){
            /*if(otherParas == null){
                otherParas = new HashedMap();
            }*/
            otherParas.put(Constants.API_PARA_GEO, conversation.getLatestQA().getGeo());
        }
        // double check the city information after merging the entities to give higher priority to the city derived from the query
        String newCity = conversation.getCurrentCityOfUser();

        if(!newCity.isEmpty()){
            if(!LocationFromAddress.getCitiesCovered().contains(newCity)){
                conversation.getLatestQA().getAnswer().setMessage("Sorry, the bot haven't been there yet. Stay tuned!");
                return StateType.EndState;
            }
            conversation.getLatestQA().setCity(newCity);
        }
        if(conversation.getLatestQA().getCity() != null && conversation.getLatestQA().getCity().trim().length() != 0){
            otherParas.put(Constants.API_PARA_CITY, conversation.getLatestQA().getCity());
        }
        RecommenderQuery query = Recommender.createRecommenderQuery(lastPattern.getQuestionType(), currentEntities, otherParas, 1);
        List<RecommenderResultsRestaurant> result = Recommender.getInstance().getRecommendationResults(query,null);

        if(isDistanceRequired(currentEntities, conversation, result, false))
            ClarificationState.addDistanceClarification(conversation);

        conversation.getLatestQA().getAnswer().setResultRestaurants(result);
        return StateType.ResultState;
    }

    @Override
    public void allowedInputStateTypes(){
        allowedInputStateTypes.add(StateType.ConsumerQueryState);
        allowedInputStateTypes.add(StateType.UserRefineState);
    }

    public boolean guardCheck(Conversation con){
        return true;
    }

    public static boolean isDistanceRequired(List<EntityExtractionUtil.EntityExtractionResult> currentEntities, Conversation conversation, List<RecommenderResultsRestaurant> result, boolean isSystemRefineAnswer){
        // add distance clarification for near by search
        // near by search case 1: #location: near/around location
        boolean flag1 = currentEntities != null && currentEntities.stream().filter(x->x.getEntityName().contains("#location") && String.join("",x.getEntityValue()).contains("near")||String.join("",x.getEntityValue()).contains("around")).count() > 0;
        // near by search case 2: $refinelocation location: near by location, close by location
        boolean flag2 = currentEntities != null && currentEntities.stream().filter(x->x.getEntityName().contains("$refinelocation")).count()> 0 &&
                (currentEntities.stream().filter(x-> x.getEntityName().contains("$location")).count() > 0 || conversation.getLatestQA().getGeo() != null);
        // it does not have distance given
        boolean flag3 = currentEntities.stream().filter(x->x.getEntityName().contains("$distance")).count() > 0;

        return (result!= null && (isSystemRefineAnswer || flag1 || flag2) && !flag3);
    }
}
