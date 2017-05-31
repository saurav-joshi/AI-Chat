package com.aibot.state;

import com.aibot.entity.Conversation;
import com.aibot.entity.Suggestion;
import com.aibot.qa.LibraryUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SystemRefineState extends State {

    List<LibraryUtil.Pattern> keywords = LibraryUtil.allPatterns.stream().filter(x -> x.getLibraryName().equals("SystemRefineState")).collect(Collectors.toList());

    @Override
    public void setStateType() {
        this.stateType = StateType.SystemRefineState;
    }

    @Override
    public StateType process(Conversation conversation) {
        conversation.getLatestQA().getAnswer().setMessage(keywords.get(0).getSystemMessage());
        LibraryUtil.Pattern disPattern = LibraryUtil.getRandomPatternByQuestionClass("Clarify.ShareLocation");
        conversation.getLatestQA().getAnswer().setSuggestion(new Suggestion(disPattern.getSystemMessage(), Arrays.asList(disPattern.getPatternAtt().trim().split("\\|\\|")), "Clarify.ShareLocation"));
        // add suggestions when user click on like
        return StateType.EndState;
    }

    @Override
    public void allowedInputStateTypes() {
        allowedInputStateTypes.add(StateType.StartState);
        allowedInputStateTypes.add(StateType.SystemGreetingState);
        allowedInputStateTypes.add(StateType.UserGreetingState);
    }

    public boolean guardCheck(Conversation con) {
        return con.getLatestQA().getGeo() == null
                && con.getLatestEntities().stream().filter(x->x.getEntityName().equalsIgnoreCase("$location")|| x.getEntityName().equalsIgnoreCase("#location")).count() == 0
              && (con.getLatestEntities().stream().anyMatch(x -> x.getEntityName().equalsIgnoreCase("$RefineLocation")) ||
                con.getLatestEntities().stream().anyMatch(x -> x.getEntityName().equalsIgnoreCase("$distance")));
    }

}
