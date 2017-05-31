package com.aibot.state;

import com.aibot.entity.Conversation;
import com.aibot.qa.FlowManagement;
import com.aibot.qa.LibraryUtil;

import java.util.List;

public class ExpectationManagementState extends State {

    @Override
    public void setStateType() {
        this.stateType = StateType.ExpectationManagementState;
    }

    @Override
    public State.StateType process(Conversation conversation){
        if(conversation.getLatestQA().getQuestion().length()>1
                && conversation.getLatestEntities().isEmpty() && !conversation.isPreProcess()){
            conversation = FlowManagement.analyzeWithPartialMatchStep(conversation);
            return StateType.EndState;
        }
        List<StateType> paths = conversation.getLatestStatePaths();
        int numOfEM = 0;
        for(int i = conversation.getQaList().size()-2; i> -1; i--){
            if(conversation.getQaList().get(i).getStatePaths().contains(StateType.ExpectationManagementState)){
                numOfEM++;
            }
            else break;
        }
        if(numOfEM > 2){
            conversation.getLatestAnswer().setMessage(LibraryUtil.getRandomPatternByQuestionClass("ExpectationManagement.UserGuide").getSystemMessage());
        }
        else if(paths.stream().anyMatch(x->x.equals(StateType.ConsumerQueryState) || x.equals(StateType.UserRefineState) || x.equals(StateType.ClarificationState)))
            conversation.getLatestAnswer().setMessage(LibraryUtil.getRandomPatternByQuestionClass("ExpectationManagement.NoResult").getSystemMessage());
        else if (paths.stream().anyMatch(x->x.equals(StateType.ShowMoreState)))
            conversation.getLatestAnswer().setMessage(LibraryUtil.getRandomPatternByQuestionClass("ExpectationManagement.NoMoreResult").getSystemMessage());
        else
            conversation.getLatestAnswer().setMessage(LibraryUtil.getRandomPatternByQuestionClass("ExpectationManagement.Unknown").getSystemMessage());
        return State.StateType.EndState;
    }

    @Override
    public void allowedInputStateTypes(){
        allowedInputStateTypes.add(StateType.StartState);
        allowedInputStateTypes.add(StateType.SystemGreetingState);
        allowedInputStateTypes.add(StateType.ConsumerQueryState);
        allowedInputStateTypes.add(StateType.UserRefineState);
    }

    public boolean guardCheck(Conversation con){
        return true;
    }
}
