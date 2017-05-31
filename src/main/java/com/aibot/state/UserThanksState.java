package com.aibot.state;

import com.aibot.entity.Conversation;
import com.aibot.entity.QA;

import java.util.List;

public class UserThanksState extends State {
    @Override
    public void setStateType() {
        this.stateType = StateType.UserThanksState;
    }

    @Override
    public void allowedInputStateTypes() {
        allowedInputStateTypes.add(StateType.StartState);
        allowedInputStateTypes.add(StateType.ConsumerQueryState);//user can thank to the suggestion
        allowedInputStateTypes.add(StateType.SystemGreetingState);//user can thank after greeting
        allowedInputStateTypes.add(StateType.EndState);//user can thank at the end conversation
    }

    @Override
    public StateType process(Conversation conversation) {
        String answer = "";
        List<QA> qaList = conversation.getQaList();
        QA qa = conversation.getLatestQA();
        int currentIndex = qaList.size()-1;

        //Check previous qa
        int previousIndex = currentIndex - 2;
        if(previousIndex>0){
            QA previousQA = qaList.get(previousIndex);
            List<StateType> states = previousQA.getStatePaths();
            StateType stateType = states.get(states.size()-1);//closest state
            if (stateType.equals(StateType.ConsumerQueryState)){
                //send to endstate
                return StateType.EndState;
            }
        }
        //default: motivation state so that user can continue conversation
        return StateType.MotivationState;
    }

    public boolean guardCheck(Conversation con){
        return true;
    }
}
