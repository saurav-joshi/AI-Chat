package com.aibot.state;

import com.aibot.entity.Conversation;

import java.util.HashSet;
import java.util.Set;

public abstract class State {

     public StateType stateType;

     public static enum StateType {
          StartState, ClarificationState, SystemRefineState, FixedAnswerState, UserThanksState, ShowMoreState, ResultState, HelpState, MotivationState,
          SystemGreetingState, UserGreetingState, ConsumerQueryState, ExpectationManagementState, UserRefineState, BookingState, EndState, ChangeLocationState
     }

     public State(){

     }

     public Set<StateType> allowedInputStateTypes = new HashSet<>();

     public abstract void setStateType();

     public abstract void allowedInputStateTypes();

     public abstract boolean guardCheck(Conversation con);

     /**
      * @param conversation
      * @return next state to jump and accumulated Conversation with inferred/extracted information
      */
     public abstract StateType process(Conversation conversation);

     public StateType getStateType(){
          return this.stateType;
     }

     public Set<StateType> getAllowedInputStateTypes(){
          return allowedInputStateTypes;
     }

     public boolean checkAllowedInputStateType(StateType stateType){
          return allowedInputStateTypes.contains(stateType);
     }
}
