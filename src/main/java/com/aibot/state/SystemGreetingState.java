package com.aibot.state;

import com.aibot.entity.Answer;
import com.aibot.entity.Conversation;
import com.aibot.entity.QA;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SystemGreetingState extends State {
    static SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    static Map<String, String> holidays = new HashMap<>();

    @Override
    public void setStateType() {
        this.stateType = StateType.SystemGreetingState;
    }

    @Override
    public void allowedInputStateTypes() {
        //allowedInputStateTypes.add(StateType.ConsumerQueryState);
    }

    @Override
    public StateType process(Conversation conversation) {
        Answer answer = new Answer();
        answer.setMessage("Hi");
        //TODO: refer to user profile to get name
        //assume the first question in qa list is greeting, later need to specify index of the current query
        List<QA> qaList = conversation.getQaList();
        QA qa = conversation.getLatestQA();
        int currentIndex = qaList.size()-1;
        //check current date
        Date currentDate = new Date();
        String moment = getMoment(currentDate);
        String holiday = getHoliday(currentDate);

        if(!holiday.isEmpty()){
            answer.setMessage(holiday);
        }else if(!moment.isEmpty()){
            answer.setMessage(new StringBuilder("Good ").append(moment).append("!").toString());
        }
        System.out.println("Greeting: " + answer.getMessage());
        qa.setAnswer(answer);
        qaList.set(currentIndex,qa);
        conversation.setQaList(qaList);

        //return next state
        return StateType.EndState;
    }

    public StateType getStateType(){
        return this.stateType;
    }

    private String getMoment(Date date){
        String moment = "";
        int hour = date.getHours();
        if(hour<13){
            moment = "morning";
        }else{
            if(hour<19){
                moment = "afternoon";
            }else {
                moment = "evening";
            }
        }
        return moment;
    }

    private String getHoliday(Date date){
        String holiday = "";
        try{
            //get current date time with Date()
            holiday = holidays.get(dateFormat.format(date));
        }catch (Exception e){
            return holiday;
        }
        return holiday;
    }

    public boolean guardCheck(Conversation con){
        return true;
    }
}
