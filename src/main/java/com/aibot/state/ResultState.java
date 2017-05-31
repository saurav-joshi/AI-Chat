package com.aibot.state;

import com.aibot.entity.Conversation;
import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.qa.FlowManagement;
import com.aibot.entity.QA;
import com.aibot.entityextraction.DistanceExtraction;
import com.aibot.qa.LibraryUtil;
import com.aibot.recommender.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ResultState extends State {

    @Override
    public void setStateType() {
        this.stateType = StateType.ResultState;
    }

    @Override
    public State.StateType process(Conversation conversation) {
        if (conversation.getLatestAnswer().getMessage() != null) {
            return StateType.EndState;
        }
        if (conversation.getLatestAnswer().getResultRestaurants() == null) {
            if (conversation.getNumOfShowMore() > 0) {
                String messagePattern = LibraryUtil.getRandomPatternByQuestionClass("Result.ShowMore.NoMoreResult.4").getSystemMessage();
                int numOfShowMoreNoResult = conversation.getNumOfShowMore() - conversation.getNumOfShowMoreHasResult();
                System.out.println("#show more no result" + numOfShowMoreNoResult);
                if (1 <= numOfShowMoreNoResult & numOfShowMoreNoResult <= 3) {
                    messagePattern = LibraryUtil.getRandomPatternByQuestionClass("Result.ShowMore.NoMoreResult." + numOfShowMoreNoResult).getSystemMessage();
                }
                conversation.getLatestAnswer().setMessage(messagePattern);
            } else return StateType.ExpectationManagementState;
        } else if (conversation.getLatestAnswer().getResultRestaurants().size() < Constants.DEFAULT_PAGE_SIZE) {
            String numOfRestaurant = String.valueOf(conversation.getLatestAnswer().getResultRestaurants().size());
            String message = LibraryUtil.getRandomPatternByQuestionClass("Result.Single").getSystemMessage().replace("%Number", numOfRestaurant);
            if (conversation.getLatestAnswer().getResultRestaurants().size() > 1) {
                message = message.replace("restaurant", "restaurants");
                message = message.replace("option", "options");
            }
            conversation.getLatestAnswer().setMessage(message);
        } else if (conversation.getLatestStatePaths().contains(StateType.UserRefineState)) {
            String messagePattern = LibraryUtil.getRandomPatternByQuestionClass("Result.Refine").getSystemMessage();
            String filledMessage = createMessageForUserRefine(messagePattern, conversation);
            conversation.getLatestAnswer().setMessage(String.join(" ", filledMessage));
        } else {
            //change from booking to consumer
            String preMess = "";
            QA secondLastQA = conversation.getQaList().size() < 2 ? null : conversation.getQaList().get(conversation.getQaList().size() - 2);
            if (secondLastQA != null && BookingState.isBookingLoop(conversation)) {
                preMess = LibraryUtil.getRandomPatternByQuestionClass("ExpectationManagement.IntentChange.ConsumerQuery").getSystemMessage() + "<br>";
            }

            String messagePattern = LibraryUtil.getRandomPatternByQuestionClass("Result.General").getSystemMessage();
            if (conversation.getLocationExpand()) {
                messagePattern = LibraryUtil.getRandomPatternByQuestionClass("Result.ShowMore.LocationBased").getSystemMessage();
                conversation.setLocationExpand(false);
            }
            conversation.getLatestQA().getAnswer().setMessage(preMess + messagePattern);
        }
        return StateType.EndState;
    }

    public String createMessageForUserRefine(String messagePattern, Conversation con) {
        List<String> result = new ArrayList<>();
        System.out.println(messagePattern);
        List<EntityExtractionUtil.EntityExtractionResult> entities = con.getLatestEntities();
        System.out.println(entities);
        Arrays.stream(messagePattern.split("\\s+")).forEach(w -> {
            if (w.equals("#RestaurantEntity")) {
                String e = entities.stream().filter(x -> !x.getEntityName().equals("$location") &&
                        !x.getEntityName().equals("#location") &&
                        !x.getEntityName().equals("$refinelocation") &&
                        !x.getEntityName().equals("$actionword") &&
                        !x.getEntityName().equals("$offer") &&
                        !x.getEntityName().equals("$distance"))
                        .map(x -> {
                            if(x.getEntityName().equals("#cuisine") || x.getEntityName().equals("$cuisine")){
                                return String.join(" ", x.getEntityValue()).substring(0, 1).toUpperCase() + String.join(" ", x.getEntityValue()).substring(1);
                            }
                           return String.join(" ", x.getEntityValue());
                        }).collect(Collectors.joining(",")).replace("restaurant", "");
                if (e.indexOf("low") != -1) e = "cheaper";
                if (e.indexOf("high") != -1) e = "nicer";
                //if similar
                if (con.getLatestQA().getMatchedPattern().getQuestionType().contains("ConsumerQuery.SimilarRestaurant")) {
                    e = "similar to " + e;
                }
                result.add(e);
            } else if (w.toLowerCase().contains("#location") || w.toLowerCase().contains("$location")) {
                String location = "";
                if (entities.stream().anyMatch(x -> x.getEntityName().equals("#location"))) {
                    location = entities.stream().filter(x -> x.getEntityName().equals("#location"))
                            .map(x -> {
                                String locationValue = FlowManagement.contextToEntityMapping.getOrDefault(String.join(" ", x.getEntityValue()),String.join(" ", x.getEntityValue()));
                                locationValue = locationValue.substring(0,1).toUpperCase() + locationValue.substring(1);
                                return String.join(" ", x.getEntityValue()).replaceAll(locationValue.toLowerCase(), locationValue);
                            }).collect(Collectors.joining(","));
                } else {
                    if (entities.stream().anyMatch(x -> x.getEntityName().equals("$location"))) {
                        location = "in " + entities.stream().filter(x -> x.getEntityName().equals("$location"))
                                .map(x -> String.join(" ", x.getEntityValue()).substring(0, 1).toUpperCase() + String.join(" ", x.getEntityValue()).substring(1))
                                .collect(Collectors.joining(","));
                    }
                }
                if (location.length() > 1) {
                    result.add(location);
                }

            } else if (w.toLowerCase().contains("$distance")) {
                if (entities.stream().anyMatch(x -> x.getEntityName().equals("$distance"))) {
                    String distanceText = entities.stream().filter(x -> x.getEntityName().equals("$distance"))
                            .map(x -> String.join(" ", x.getEntityValue())).collect(Collectors.joining(","));
                    String distanceNum = DistanceExtraction.getInstance().getFormattedDistanceFromText(distanceText);
                    String distanceValue = null;
                    try {
                        distanceValue = Double.valueOf(distanceNum) >= 1 ? distanceNum + "km." : String.valueOf((int) Math.round(1000.0 * Double.valueOf(distanceNum))) + "m.";
                    } catch (Exception e) {
                        System.out.println("Wrong distance value.");
                        e.printStackTrace();
                    }
                    if (distanceValue != null && distanceValue.length() > 1) {
                        result.add("within " + distanceValue);
                    }
                }else{
                    result.add(".");
                }
            } else {
                result.add(w);
                if (w.contains("restaurant")) {
                    if (entities.stream().anyMatch(x -> x.getEntityName().equals("$offer"))) {
                        result.add(" with offer ");
                    }
                }
            }

        });
        System.out.println(result);
        return result.stream().collect(Collectors.joining(" ")).replaceAll("\\s+\\.",".");
    }

    @Override
    public void allowedInputStateTypes() {
        allowedInputStateTypes.add(StateType.StartState);
        allowedInputStateTypes.add(StateType.SystemGreetingState);
        allowedInputStateTypes.add(StateType.UserGreetingState);
    }

    public boolean guardCheck(Conversation con) {
        return true;
    }
}
