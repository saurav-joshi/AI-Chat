package com.aibot.state;

import com.aibot.entity.Conversation;
import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.qa.LibraryUtil;

public class ChangeLocationState extends State {
    @Override
    public void setStateType() {
        this.stateType = StateType.UserThanksState;
    }

    @Override
    public void allowedInputStateTypes() {

    }

    @Override
    public StateType process(Conversation conversation) {
        //city entities
        EntityExtractionUtil.EntityExtractionResult ee = conversation.getLatestEntities().stream().filter(x -> x.getEntityName().contains("city")).findFirst().get();
        if(ee!=null){
            String city = String.join(" ",ee.getEntityValue());
            String greeting = LibraryUtil.getRandomPatternByQuestionClass("SystemAcknowledgement." + city).getSystemMessage() + ". "
                    + LibraryUtil.getRandomPatternByQuestionClass("SystemTip").getSystemMessage() + ". "
                    + LibraryUtil.getRandomPatternByQuestionClass("SystemStarted." + city).getSystemMessage();
            conversation.getLatestQA().getAnswer().setMessage(greeting);
        }

        return StateType.EndState;
    }

    public boolean guardCheck(Conversation con){
        return true;
    }
}
