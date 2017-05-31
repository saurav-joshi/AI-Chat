package com.aibot.state;

import com.aibot.entity.Conversation;
import com.aibot.entity.*;
import com.aibot.entity.QA;
import com.aibot.qa.LibraryUtil;
import com.aibot.recommender.Constants;
import com.aibot.recommender.Recommender;
import com.aibot.entity.RecommenderResultsRestaurant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShowMoreState extends State {

    @Override
    public void setStateType() {
        this.stateType = StateType.ShowMoreState;
    }

    @Override
    public State.StateType process(Conversation conversation) {
        int numOfShowMore = 1;
        int numOfShowMoreHasResult = 0;
        QA lastQaBeforeShowMore = null;

        for (int i = conversation.getQaList().size() - 2; i > -1; i--) {
            QA qa = conversation.getQaList().get(i);
            if (qa.getStatePaths().contains(StateType.ShowMoreState)){
                numOfShowMore++;
                if(!qa.getAnswer().getResultRestaurants().isEmpty()){
                    numOfShowMoreHasResult++;
                }
            }
            else {
                lastQaBeforeShowMore = qa;
                break;
            }
        }
        if (lastQaBeforeShowMore == null)
            return StateType.ExpectationManagementState;
        conversation.getLatestQA().setCity(lastQaBeforeShowMore.getCity());
        conversation.setNumOfShowMore(numOfShowMore);
        conversation.setNumOfShowMoreHasResult(numOfShowMoreHasResult);
        if(numOfShowMoreHasResult>=3){
            if(numOfShowMore < 7)
                conversation.getLatestAnswer().setMessage(LibraryUtil.getRandomPatternByQuestionClass("Result.ShowMore.Limit." + numOfShowMore).getSystemMessage());
            else
                conversation.getLatestAnswer().setMessage(LibraryUtil.getRandomPatternByQuestionClass("Result.ShowMore.Limit.7").getSystemMessage());
            return StateType.ResultState;
        }

        if(lastQaBeforeShowMore.getStatePaths().contains(StateType.FixedAnswerState)){
            LibraryUtil.Pattern pattern = lastQaBeforeShowMore.getMatchedPattern();
            if(pattern.getPatternAtt() != null && pattern.getPatternAtt().trim().length() != 0){
                List<String> restIds = new ArrayList<>();
                Arrays.stream(pattern.getPatternAtt().split("\\|\\|")).forEach(x->restIds.add(x));
                List<String> restId2display = new ArrayList<>();
                int start = Constants.DEFAULT_PAGE_SIZE * conversation.getNumOfShowMore();
                int end = Constants.DEFAULT_PAGE_SIZE * (conversation.getNumOfShowMore() + 1);
                if(end <= restIds.size()){
                    restId2display.addAll(restIds.subList(start, end));
                }else if(start >= restIds.size()){
                    start = start % restIds.size();
                    end = end % restIds.size();
                    restId2display.addAll(restIds.subList(start, end));
                }else{
                    restId2display.addAll(restIds.subList(start, restIds.size()));
                    restId2display.addAll(restIds.subList(0, end % restIds.size()));
                }
                List<RecommenderResultsRestaurant>result = Recommender.getInstance().getRestaurantsInfoByIds(restId2display);
                conversation.getLatestQA().getAnswer().setResultRestaurants(result);
                conversation.getLatestQA().getAnswer().setMessage(pattern.getSystemMessage());
                return StateType.EndState;
            }
        }

        return StateType.ConsumerQueryState;
    }

    @Override
    public void allowedInputStateTypes() {
        allowedInputStateTypes.add(State.StateType.StartState);
        allowedInputStateTypes.add(State.StateType.SystemGreetingState);
        allowedInputStateTypes.add(State.StateType.UserGreetingState);
    }

    public boolean guardCheck(Conversation con) {
        return true;
    }

}
