package com.aibot.state;


import com.aibot.entity.Conversation;

public class UserGreetingState extends State {

    @Override
    public void setStateType() {
        this.stateType = StateType.UserGreetingState;
    }

    @Override
    public void allowedInputStateTypes()
    {
        allowedInputStateTypes.add(StateType.StartState);
    }

    @Override
    public StateType process(Conversation conversation) {
        return StateType.MotivationState;
    }

    public boolean guardCheck(Conversation con){
        return true;
    }
}
