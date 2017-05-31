package com.aibot.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Random;

import javax.servlet.http.HttpServletResponse;

import com.aibot.data.repo.UserProfileRepo;
import com.aibot.entity.Answer;
import com.aibot.entity.Suggestion;
import com.aibot.entity.UserMessage;
import com.aibot.entityextraction.NegationDetector;
import com.aibot.qa.FlowManagement;
import com.aibot.qa.GeoCalculator;
import com.aibot.state.BookingState;
import com.aibot.data.model.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.aibot.dao.QADao;
import com.aibot.data.repo.ConversationRepo;
import com.aibot.data.repo.ConversationRepoImpl;
import com.aibot.data.repo.UserProfileRepoImpl;
import com.aibot.entity.QA;
import com.aibot.entityextraction.LocationFromAddress;
import com.aibot.qa.LibraryUtil;
import com.aibot.recommender.UserProfiling;

@RestController
public class ConversationController extends BaseController {

    static FlowManagement ma = FlowManagement.init();

    private enum TYPE {QUERY, NEW, BEGIN}
    @Autowired
    UserProfileRepo userProfileRepo;
    @Autowired
    ConversationRepo conversationRepo;
    @Autowired
    ConversationRepoImpl conversationCustomRepo;
    @Autowired
    NegationDetector negationDetector;

    @Autowired
    UserProfileRepoImpl userProfileRepoCustom;

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public String test() {
        //com.oracle.iaasimov.data.model.Conversation conversation=conversationRepo.findByUserProfileUserId("14f329be-bcb4-4f61-868a-bb53474c6795");
        com.aibot.data.model.Conversation conversation_nw = new com.aibot.data.model.Conversation();
        conversation_nw.setUserProfile(userProfileRepo.findOne("14f329be-bcb4-4f61-868a-bb53474c6795"));
        conversationRepo.save(conversation_nw);
    	//com.crayon.data.model.UserProfile crayonBotProfile = userProfileRepo.findOne("14f329be-bcb4-4f61-868a-bb53474c6795");
        //crayonBotProfile.setName("test");
        String test = "I don't like chicken rice";
        String keyword = "chicken rice";
        System.out.println(negationDetector.isNegation(test.split("\\s+"),negationDetector.negationScope(test.split("\\s+")),keyword.split("\\s+")));
    	return conversation_nw.getConversationId()+"";
    }
    @RequestMapping(value = "/query", method = RequestMethod.POST)
    public Object receive(@RequestBody UserMessage um, HttpServletResponse res) {
        System.out.println("---------------->>Collecting user profile<<----------------");
        com.aibot.entity.UserProfile up = userProfileRepoCustom.findByUserIdCustom(um.getToken());
    	if (up == null) {
    		res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);//User are bot created by the developer
    		return getRespErr("Invalid token key.");
    	}
    	if (!contains(um.getType())) {
    		res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    		return getRespErr("Invalid type key.");
    	}
    	if (TYPE.QUERY.toString().equalsIgnoreCase(um.getType()) && (um.getQuestion() == null || um.getQuestion().trim().equals(""))
                && (um.getLatitude() == null || um.getLatitude().trim().equals("") || um.getLongitude() == null || um.getLongitude().trim().equals(""))) {
    		res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    		return getRespErr("Please ask any questions.");
    	}

    	// check number of request limited
    	if (isRequestLimited(um.getToken())) {
    		res.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
    		return getRespErr("Your application is expired. Please contact your admin.");
    	}
    	
        System.out.println("---------------->>     Question: "+ um.getQuestion()+ "     <<----------------");
        com.aibot.entity.Conversation con = conversationCustomRepo.findCustomConByUserId(um.getToken());

        Random rand = new Random();
        if (con == null) {
            com.aibot.data.model.Conversation conversationModel = new com.aibot.data.model.Conversation();
            UserProfile userProfileModel = new UserProfile();
            userProfileModel.setName(up.getName());
            userProfileModel.setUserId(um.getToken());

            conversationModel.setUserProfile(userProfileModel);
            conversationRepo.save(conversationModel);

            con = new com.aibot.entity.Conversation();
            con.setId((int)conversationRepo.findByUserProfileUserId(um.getToken()).getConversationId());
            con.setUserId(um.getToken());
            con.setUserName(up.getName());
            con.setQaList(new ArrayList<>());
        }
        QA qa = new QA();
            qa.setQuestion(um.getQuestion() == null ? "" : um.getQuestion().trim());
            qa.setOriginalQuestion(um.getQuestion() == null ? "" : um.getQuestion().trim());
            qa.setTime(new Date());
            qa.setConversation_id(con.getId());
            qa.setId(con.getQaList().size()+1);
        if(um.getLatitude() != null && org.apache.commons.lang3.math.NumberUtils.isParsable(um.getLatitude())
                && um.getLongitude() != null && org.apache.commons.lang3.math.NumberUtils.isParsable(um.getLongitude())){
            qa.setGeo(String.join(",", Arrays.asList(um.getLatitude(), um.getLongitude())));
            //TODO: move city and location from start state to here
        }
        con.getQaList().add(qa);

        Answer answer = new Answer() ;
        if(um.getType().equalsIgnoreCase(TYPE.QUERY.toString())){
            //if geo share and question is empty, set question is the city
            if(qa.getQuestion().isEmpty() && !qa.getGeo().isEmpty()){
                String city = GeoCalculator.getCityFromLatLongOpenMap(qa.getGeo().split(",")[0], qa.getGeo().split(",")[1]);
                if(con.getQaList().size()<3){
                    if(!LocationFromAddress.getCitiesCovered().contains(city)){
                        con.getLatestQA().getAnswer().setMessage(LibraryUtil.getRandomPatternByQuestionClass("Warning.CoveredCity").getSystemMessage());
                    }else{
                        //new user share location => show acknowledgement and tip
                        String greeting = getDialogByCity(city, dialogTypeByCity.SystemAcknowledgement.name()) + "\n"
                                + LibraryUtil.getRandomPatternByQuestionClass("SystemTip").getSystemMessage() + "\n"
                                + getDialogByCity(city, dialogTypeByCity.SystemStarted.name());
                        con.getLatestQA().getAnswer().setMessage(greeting);
                    }
                    answer = con.getLatestQA().getAnswer();
                }
            }else{
                answer = query(con);
            }
        } else if(um.getType().equalsIgnoreCase(TYPE.NEW.toString()))
            answer = newUserGreeting(con);
        else if(um.getType().equalsIgnoreCase(TYPE.BEGIN.toString()))
            answer = userBeginGreeting(con);
        if(BookingState.isBookingExpired(con)){
            answer.setWarningMessage(LibraryUtil.getRandomPatternByQuestionClass("Warning.BookingTermination").getSystemMessage());
            System.out.println("Previous booking is terminated: " + answer.getWarningMessage());
        }
        QADao.getInstance().insertListQA(con);
        userProfileRepoCustom.saveUserProfile(UserProfiling.profiling(Collections.singletonList(con)));
        //tracking query from user success or fail
        if (answer.getMessage() == null) {
        	trackingRequest(um.getToken(), false);
        } else {
        	trackingRequest(um.getToken(), true);
        }
        
        res.setStatus(HttpServletResponse.SC_OK);
        return answer;
    }
    
    private Answer query(com.aibot.entity.Conversation con){
        try{
            ma.process(con);
        }catch (Exception e){
            e.printStackTrace();
            con.getLatestQA().getAnswer().setMessage(LibraryUtil.getRandomPatternByQuestionClass("ExpectationManagement.UserGuide").getSystemMessage());
        }
        return con.getLatestAnswer();
    }

    private Answer newUserGreeting(com.aibot.entity.Conversation con){
    	String greeting = LibraryUtil.getRandomPatternByQuestionClass("SystemGreeting.FirstTimeUser").getSystemMessage()
                .replace("#UserName",con.getUserName());
        greeting = greeting + " " + LibraryUtil.getRandomPatternByQuestionClass("SystemRequestLocation").getSystemMessage();
        LibraryUtil.Pattern disPattern = LibraryUtil.getRandomPatternByQuestionClass("Clarify.ShareLocation");
        con.getLatestQA().getAnswer().setSuggestion(new Suggestion(disPattern.getSystemMessage(), Arrays.asList(disPattern.getPatternAtt().trim().split("\\|\\|")), "Clarify.ShareLocation"));
        con.getLatestQA().getAnswer().setMessage(greeting);
       return con.getLatestAnswer();
    }

    private Answer userBeginGreeting(com.aibot.entity.Conversation con){
        String greeting =LibraryUtil.getRandomPatternByQuestionClass("SystemGreeting.General").getSystemMessage()
                .replace("#UserName",con.getUserName()) + "\n"
                + LibraryUtil.getRandomPatternByQuestionClass("TipsOfTheDay").getSystemMessage(); //tips

        //TODO: use last known geo (return user)
        con.getLatestQA().getAnswer().setMessage(greeting);
        return con.getLatestAnswer();
    }
    
    private boolean contains(String type) {
        for (TYPE t : TYPE.values()) {
            if (t.name().equalsIgnoreCase(type)) {
                return true;
            }
        }
        return false;
    }

    private enum dialogTypeByCity {SystemStarted, SystemAcknowledgement}
    private String getDialogByCity(String city, String dialog){
        try{
            return LibraryUtil.getRandomPatternByQuestionClass(dialog + "." + city).getSystemMessage();
        }catch (Exception e){
            return "";
        }
    }

}

