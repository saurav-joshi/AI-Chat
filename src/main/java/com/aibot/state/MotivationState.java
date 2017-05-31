package com.aibot.state;

import com.aibot.entity.Conversation;
import com.aibot.qa.LibraryUtil;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class MotivationState extends State{
    Random rand = new Random();

    @Override
    public void setStateType() {
        this.stateType = StateType.MotivationState;
    }

    @Override
    public State.StateType process(Conversation conversation){
        List<String> resultMessage = LibraryUtil.allPatterns.stream()
            .filter(x -> x.getLibraryName().equals(StateType.MotivationState.toString()))
            .map(x -> x.getSystemMessage()).collect(Collectors.toList());
        conversation.getLatestAnswer().setMessage(resultMessage.get(rand.nextInt(resultMessage.size())));
        return StateType.EndState;
    }

    @Override
    public void allowedInputStateTypes(){
        allowedInputStateTypes.add(StateType.StartState);
        allowedInputStateTypes.add(StateType.SystemGreetingState);
        allowedInputStateTypes.add(StateType.UserGreetingState);
    }

    public boolean guardCheck(Conversation con){
        return true;
    }
}
