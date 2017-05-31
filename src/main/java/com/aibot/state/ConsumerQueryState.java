package com.aibot.state;


import com.aibot.entity.Conversation;
import com.aibot.entity.QA;
import com.aibot.entity.RecommenderQuery;
import com.aibot.entityextraction.DistanceExtraction;
import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.qa.FlowManagement;
import com.aibot.recommender.Constants;
import com.aibot.entity.*;
import com.aibot.entity.RecommenderResultsRestaurant;
import com.aibot.entityextraction.LocationFromAddress;
import com.aibot.qa.LibraryUtil;
import com.aibot.recommender.Recommender;
import com.aibot.recommender.UserProfiling;
import org.apache.commons.collections.map.HashedMap;

import java.util.*;
import java.util.stream.Collectors;

public class ConsumerQueryState extends State {

    @Override
    public void setStateType() {
        this.stateType = StateType.ConsumerQueryState;
    }

    @Override
    public StateType process(Conversation con) {
        if (FlowManagement.getStateMap().get(StateType.SystemRefineState).guardCheck(con)) {
            return StateType.SystemRefineState;
        }
        if(con.getLatestEntities().size()==0 &&
                !con.getLatestQA().getMatchedPattern().getQuestionType().contains("ConsumerQuery.SurpriseMe") &&
                !con.getLatestQA().getStatePaths().contains(StateType.ShowMoreState) &&
                !con.getLatestQA().getStatePaths().contains(StateType.UserRefineState)){
            con.getLatestQA().getAnswer().setMessage(LibraryUtil.getRandomPatternByQuestionClass("ExpectationManagement.UserGuide").getSystemMessage());
            return StateType.EndState;
        }
        boolean isSystemRefineAnswer = false;
        QA secondLastQA = con.getQaList().size() < 2 ? null : con.getQaList().get(con.getQaList().size() - 2);
        if (secondLastQA != null
                && secondLastQA.getStatePaths().contains(StateType.SystemRefineState)
                && !secondLastQA.getStatePaths().contains(StateType.ConsumerQueryState)
                ) {
            boolean flag1 = con.getLatestEntities().stream().filter(x->x.getEntityName().contains("$changeintentwords")).count() == 0;
            boolean flag2 = con.getLatestEntities().stream().filter(x->x.getEntityName().contains("#location")||x.getEntityName().contains("$location")).count() > 0 ||
                    con.getLatestQA().getGeo() != null;
            if(flag1 && flag2){
                isSystemRefineAnswer = true;
                // distinguish whether the secondLast QA has the correct question Type
                List<EntityExtractionUtil.EntityExtractionResult> mergedEntity = mergeEntity(secondLastQA.getEntities(), con.getLatestEntities());
                if(secondLastQA.getMatchedPattern().getQuestionType().contains("SystemRefine.Location")){
                    QA thirdLastQA = con.getQaList().size() >= 3 ? con.getQaList().get(con.getQaList().size() - 3) : null;
                    boolean hasValidQAToMerge = false;
                    if(thirdLastQA != null && con.isLastQAWithinTolerantTime(con.getLatestQA(), thirdLastQA) &&
                            (thirdLastQA.getStatePaths().contains(StateType.ConsumerQueryState) ||
                                thirdLastQA.getStatePaths().contains(StateType.UserRefineState))){
                        hasValidQAToMerge = true;
                    }
                    if(hasValidQAToMerge){
                        for(EntityExtractionUtil.EntityExtractionResult oldEntity : thirdLastQA.getEntities()){
                            if(mergedEntity.stream().filter(x->x.getEntityName().contains(oldEntity.getEntityName())).count() == 0){
                                mergedEntity.add(oldEntity);
                            }
                        }
                        con.getLatestQA().setEntities(mergedEntity);
                        con.getLatestQA().setMatchedPattern(thirdLastQA.getMatchedPattern());
                    }else{
                        con.getLatestQA().setEntities(mergedEntity);
//                        con.getLatestQA().setMatchedPattern(LibraryUtil.getRandomPatternByQuestionClass("ConsumerQuery.ClassMember"));
                    }
                }else{
                    con.getLatestQA().setEntities(mergedEntity);
                    con.getLatestQA().setMatchedPattern(secondLastQA.getMatchedPattern());
                }
            }
        }

        RecommenderQuery query;
        List<RecommenderResultsRestaurant> result;
        //show more
        if (con.getNumOfShowMore() > 0) {
            LibraryUtil.Pattern pattern = secondLastQA.getMatchedPattern();
            // enter show more when there is no valid query is given previously, such as just login, or new login in a day
            if(pattern == null || secondLastQA.getQuestion() == null){
                return StateType.ExpectationManagementState;
            }
            con.getLatestQA().setEntities(secondLastQA.getEntities());
            con.getLatestQA().setMatchedPattern(secondLastQA.getMatchedPattern());
            Set<String> questionType = pattern.getQuestionType();
            if(!secondLastQA.getStatePaths().contains(StateType.ConsumerQueryState)){
                QA lastConsumerQueryQA = con.getLatestQAWhichIncludeStateTypes(Arrays.asList(StateType.ConsumerQueryState));
                questionType = lastConsumerQueryQA.getMatchedPattern().getQuestionType();
            }

            // TODO: to test. put in the other parameters if available
            Map<String,String> otherParas = new HashedMap();
            if(secondLastQA.getGeo()!= null){
                otherParas.put(Constants.API_PARA_GEO, secondLastQA.getGeo());
            }
            if(con.getLatestQA().getCity() != null && con.getLatestQA().getCity().trim().length() != 0){
                otherParas.put(Constants.API_PARA_CITY, con.getLatestQA().getCity());
            }
            query = Recommender.createRecommenderQuery(questionType, secondLastQA.getEntities(), otherParas, con.getNumOfShowMore() + 1);
            List<String> previousIds = con.getRecommendRestaurantIdInShowMore();
            if(previousIds != null && previousIds.size() != 0){
                query.setIdsToFilterOut(new HashSet<>(previousIds));
            }
            System.out.println("show more --------" + query.getProperties());

            result = Recommender.getInstance().getRecommendationResults(query, UserProfiling.profiling(con));
        } else {//default is normal query
            // TODO: to test. put in the other parameters if available
            Map<String,String> otherParas =  new HashedMap();
            if(con.getLatestQA().getGeo()!= null){
                otherParas.put(Constants.API_PARA_GEO,con.getLatestQA().getGeo());
            }
            if(con.getLatestQA().getCity() != null && con.getLatestQA().getCity().trim().length() != 0){
                otherParas.put(Constants.API_PARA_CITY, con.getLatestQA().getCity());
            }
            query = Recommender.createRecommenderQuery(con.getLatestQA().getMatchedPattern().getQuestionType(), con.getLatestEntities(), otherParas, 1);
            result = Recommender.getInstance().getRecommendationResults(query, UserProfiling.profiling(con));
        }

        if(UserRefineState.isDistanceRequired(con.getLatestEntities(),con,result,isSystemRefineAnswer)){
            con.getLatestQA().getAnswer().setResultRestaurants(result);
            ClarificationState.addDistanceClarification(con);
            return StateType.ResultState;
        }

        //query expansion
        if (result == null) {
            QA lastQuery = con.getLatestQA();
            Map<String,String> otherParas = new HashedMap();
            System.out.println(lastQuery.getQuestion() + "\t" + lastQuery.getStatePaths().toString());
            boolean containsLocation = lastQuery.getEntities().stream().anyMatch(e -> e.getEntityName().contains("location")) || lastQuery.getGeo() != null;
            List<String> locations = lastQuery.getEntities().stream()
                    .filter(e -> e.getEntityName().contains("location"))
                    .map(x -> String.join(" ",x.getEntityValue())).collect(Collectors.toList());
            boolean containsRestaurantName = lastQuery.getEntities().stream().anyMatch(e -> e.getEntityName().contains("restaurantname"));
            if (containsLocation && !isAllLocationAreBuilding(locations)) {
                List<String> previousIds = con.getNumOfShowMore() > 0 ? con.getRecommendRestaurantIdInShowMore() : null;
                double distanceUnit = Constants.DEFAULT_DISTANCE_TO_FILTER;
                if(lastQuery.getEntities().stream().anyMatch(e->e.getEntityName().contains("distance"))){
                    List<EntityExtractionUtil.EntityExtractionResult> distanceEntities = lastQuery.getEntities().stream().filter(x -> x.getEntityName().contains("$distance")).collect(Collectors.toList());
                    if (distanceEntities != null && distanceEntities.size() > 0) {
                        String distanceNum = DistanceExtraction.getInstance().getFormattedDistanceFromText(String.join(" ", distanceEntities.get(0).getEntityValue()));
                        if(distanceNum != null){
                            distanceUnit = Double.valueOf(distanceNum);
                        }
                    }
                }
                double distance = distanceUnit * 2;
                while(distance <= Constants.DEFAULT_DISTANCE_LIMIT_TO_EXTEND){
                    // pass new distance to recomender
                    otherParas.put(Constants.GEO_DISTANCE_TO_FILTER, String.valueOf(distance));
                    if(lastQuery.getGeo() != null){
                        otherParas.put(Constants.API_PARA_GEO, lastQuery.getGeo());
                    }
                    if(con.getLatestQA().getCity() != null && con.getLatestQA().getCity().trim().length() != 0){
                        otherParas.put(Constants.API_PARA_CITY, con.getLatestQA().getCity());
                    }
                    query = Recommender.createRecommenderQuery(con.getLatestQA().getMatchedPattern().getQuestionType(), con.getLatestEntities(), otherParas, 1);
                    System.out.println("expansion --------" + query.getProperties() + " distance:" + distance);
                    // filter out previous results
                    if(previousIds != null && previousIds.size() != 0){
                        query.setIdsToFilterOut(new HashSet<>(previousIds));
                    }
                    result = Recommender.getInstance().getRecommendationResults(query, null);
                    if(result != null && result.size() >= Constants.DEFAULT_PAGE_SIZE){
                        con.getLatestQA().getEntities().add(new EntityExtractionUtil.EntityExtractionResult("$distance",String.valueOf(distance)));
                        con.setLocationExpand(true);
                        break;
                    }else{
                        distance *= 2;
                    }
                }

                // if restaurant name mentioned, search whole city
                if((result == null || result.size() < Constants.DEFAULT_PAGE_SIZE)
                        && containsRestaurantName && !lastQuery.getMatchedPattern().getQuestionType().contains("ConsumerQuery.SimilarRestaurant")){ // Expand to city
                    // remove all locations in the entities
                    List<EntityExtractionUtil.EntityExtractionResult> entities = new ArrayList<>();
                    lastQuery.getEntities().stream().forEach(e -> {
                        if (!(e.getEntityName().contains("location"))) {
                            entities.add(e);
                        }
                    });
                    otherParas.remove(Constants.GEO_DISTANCE_TO_FILTER);
                    otherParas.remove(Constants.API_PARA_GEO);
                    con.getLatestQA().setEntities(entities);
                    con.getLatestQA().setMatchedPattern(lastQuery.getMatchedPattern());
                    //rebuild the query and get result again
                    // do not consider the geo here when expand to global
                    query = Recommender.createRecommenderQuery(con.getLatestQA().getMatchedPattern().getQuestionType(), con.getLatestEntities(), otherParas, 1);
                    System.out.println("expansion --------" + query.getProperties() + " to city");
                    // filter out previous results
                    if(previousIds != null && previousIds.size() != 0){
                        query.setIdsToFilterOut(new HashSet<>(previousIds));
                    }
                    result = Recommender.getInstance().getRecommendationResults(query, null);
                    if(result!=null){
                        con.setLocationExpand(true);
                    }
                }
            }
        }

        con.getLatestQA().getAnswer().setResultRestaurants(result);
        if (con.getLatestQA().getMatchedPattern().getQuestionType().contains("ConsumerQuery.SurpriseMe") && result!=null)
            con.getLatestQA().getAnswer().setMessage(con.getLatestQA().getMatchedPattern().getSystemMessage());
        return StateType.ResultState;
    }

    @Override
    public void allowedInputStateTypes() {
        allowedInputStateTypes.add(StateType.StartState);
        allowedInputStateTypes.add(StateType.ConsumerQueryState);
        allowedInputStateTypes.add(StateType.UserRefineState);
    }

    List<EntityExtractionUtil.EntityExtractionResult> mergeEntity(List<EntityExtractionUtil.EntityExtractionResult> oldEntity, List<EntityExtractionUtil.EntityExtractionResult> newEntity) {
        List<EntityExtractionUtil.EntityExtractionResult> mergedEntity = new ArrayList<>();
        mergedEntity.addAll(newEntity);
        oldEntity.forEach(x -> {
            if (!newEntity.stream().anyMatch(y -> y.getEntityName().equals(x.getEntityName())))
                mergedEntity.add(x);
        });
        return mergedEntity;
    }

    public boolean guardCheck(Conversation con) {
        return true;
    }

    private boolean isAllLocationAreBuilding(List<String> locations){
        if(locations==null || locations.isEmpty()) return false;
        for (String location : locations) {
            String locationWithoutPres = FlowManagement.contextToEntityMapping.getOrDefault(location,location);
            if(!LocationFromAddress.isBuildingOrStreet(locationWithoutPres)){
                return false;
            }
        }
        return true;
    }

}
