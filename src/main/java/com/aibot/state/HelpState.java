package com.aibot.state;

import com.aibot.entity.Conversation;
import com.aibot.entity.QA;
import com.aibot.qa.LibraryUtil;

public class HelpState extends State{
    @Override
    public void setStateType() {
        this.stateType = StateType.HelpState;
    }

    @Override
    public void allowedInputStateTypes() {
        allowedInputStateTypes.add(StateType.ConsumerQueryState);
    }

    @Override
    public StateType process(Conversation conversation) {
        LibraryUtil.Pattern currentPattern = conversation.getLatestQA().getMatchedPattern();
        if(currentPattern != null){
            String answerMessage = currentPattern.getSystemMessage();
            QA secondLastQA = conversation.getQaList().size() < 2 ? null : conversation.getQaList().get(conversation.getQaList().size() - 2);
            if (secondLastQA != null) {
                if(secondLastQA.getStatePaths().contains(StateType.BookingState)){
                    if(currentPattern.getQuestionType().contains("Help.Help"))
                        answerMessage = LibraryUtil.getRandomPatternByQuestionClass("ExpectationManagement.IntentChange.Help.Help").getSystemMessage() + "<br>" + answerMessage;
                    else
                        answerMessage = LibraryUtil.getRandomPatternByQuestionClass("ExpectationManagement.IntentChange.Help.NegativeExpression").getSystemMessage();
                }
            }
            conversation.getLatestQA().getAnswer().setMessage(answerMessage);
            return StateType.EndState;
        }else{//do noting, route to expectation management
            return StateType.ExpectationManagementState;
        }
    }

    public boolean guardCheck(Conversation con){
        return true;
    }
}
