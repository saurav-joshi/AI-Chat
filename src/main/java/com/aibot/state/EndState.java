package com.aibot.state;

import com.aibot.entity.Conversation;

public class EndState extends State {


    public void setStateType() {
        this.stateType = StateType.EndState;
    }

    public StateType process(Conversation conversation){
        return StateType.EndState ;
    }

    public void allowedInputStateTypes(){
        allowedInputStateTypes.add(StateType.ExpectationManagementState);
        allowedInputStateTypes.add(StateType.ConsumerQueryState);
        allowedInputStateTypes.add(StateType.SystemGreetingState);
        allowedInputStateTypes.add(StateType.UserRefineState);
        allowedInputStateTypes.add(StateType.MotivationState);
        allowedInputStateTypes.add(StateType.UserGreetingState);
    }

    public boolean guardCheck(Conversation con){
        return true;
    }
}
