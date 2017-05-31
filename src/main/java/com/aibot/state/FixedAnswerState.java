package com.aibot.state;

import com.aibot.entity.Conversation;
import com.aibot.entity.QA;
import com.aibot.entity.RecommenderResultsRestaurant;
import com.aibot.qa.LibraryUtil;
import com.aibot.recommender.Recommender;

import java.util.*;

public class FixedAnswerState extends State {

    @Override
    public void setStateType() {
        this.stateType = StateType.FixedAnswerState;
    }

    @Override
    public State.StateType process(Conversation conversation){
        LibraryUtil.Pattern currentPattern = conversation.getLatestQA().getMatchedPattern();
        // if has manually-prepared results, display them directly
        if(currentPattern.getPatternAtt() != null && currentPattern.getPatternAtt().trim().length() != 0){
            Set<String> restIds = new TreeSet<>((s1, s2) -> s2.toLowerCase().compareTo(s1.toLowerCase()));
            java.util.Arrays.stream(currentPattern.getPatternAtt().split("\\|\\|")).forEach(x->restIds.add(x));
            List<String> restId2display = new ArrayList<>(restIds).subList(0,6);
            List<RecommenderResultsRestaurant> result = Recommender.getInstance().getRestaurantsInfoByIds(new ArrayList<>(restId2display));
            // set the result restaurants
            conversation.getLatestQA().getAnswer().setResultRestaurants(result);
        }
        String answerMessage = conversation.getLatestQA().getMatchedPattern().getSystemMessage();
        QA secondLastQA = conversation.getQaList().size() < 2 ? null : conversation.getQaList().get(conversation.getQaList().size() - 2);
        if (secondLastQA != null && secondLastQA.getStatePaths().contains(StateType.BookingState)) {
            answerMessage = LibraryUtil.getRandomPatternByQuestionClass("ExpectationManagement.IntentChange.FixedAnswer").getSystemMessage()
                            + "<br>" + answerMessage;
        }
        conversation.getLatestQA().getAnswer().setMessage(answerMessage);
        return StateType.EndState;
    }

    @Override
    public void allowedInputStateTypes(){
        allowedInputStateTypes.add(StateType.ConsumerQueryState);
        allowedInputStateTypes.add(StateType.UserRefineState);
    }


    public boolean guardCheck(Conversation con){
        return true;
    }
}
