package com.aibot.controller;

import com.aibot.dao.QADao;
import com.aibot.data.model.Booking;
import com.aibot.data.repo.BookingRepo;
import com.aibot.data.repo.ConversationRepoImpl;
import com.aibot.data.repo.UserProfileRepoImpl;
import com.aibot.entity.*;
import com.aibot.entity.*;
import com.aibot.qa.LibraryUtil;
import com.aibot.recommender.Recommender;
import com.aibot.recommender.UserProfiling;
import com.aibot.state.BookingState;
import com.aibot.entity.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Date;

@RestController
public class BookingController extends BaseController {
    @Autowired
    ConversationRepoImpl conversationCustomRepo;
    @Autowired
    BookingRepo bookingRepo;
    @Autowired
    UserProfileRepoImpl userProfileRepoCustom;

    private enum TYPE {ACTIVE, CONFIRM, CANCEL}
    @RequestMapping(value = "/booking", method = RequestMethod.POST)
    public Object receive(@RequestBody BookingReq bookingInfo, HttpServletResponse res) {
    	if (bookingInfo.getRestId() == null || bookingInfo.getRestId().isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return getRespErr("Invalid restaurant id.");
        }

        UserProfile up = userProfileRepoCustom.findByUserIdCustom(bookingInfo.getToken());
        if (up == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return getRespErr("Invalid token key.");
        }
        if (!contains(bookingInfo.getType())) {
    		res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    		return getRespErr("Invalid type key.");
    	}

        System.out.println("---------------->>     Active booking     <<----------------");
        Conversation con = conversationCustomRepo.findCustomConByUserId(bookingInfo.getToken());

        QA qa = new QA();
        qa.setTime(new Date());
        qa.setConversation_id(con.getId());
        qa.setId(con.getQaList().size()+1);
        con.getQaList().add(qa);
        con.setBookingRestaurantId(Integer.parseInt(bookingInfo.getRestId()));

        Answer answer = null ;
        if(bookingInfo.getType().equalsIgnoreCase(TYPE.ACTIVE.toString())){
            qa.setOriginalQuestion(BookingState.bookingActivate);
            qa.setQuestion(BookingState.bookingActivate); //start using query api
            con.setActivateBooking(true);
            answer = query(con);
        }
        else if(bookingInfo.getType().equalsIgnoreCase(TYPE.CONFIRM.toString())){
            qa.setOriginalQuestion(BookingState.bookingConfirm);
            qa.setQuestion(BookingState.bookingConfirm);
            answer = confirmBooking(con);
        }
        else if(bookingInfo.getType().equalsIgnoreCase(TYPE.CANCEL.toString())){
            qa.setOriginalQuestion(BookingState.bookingConfirm);
            qa.setQuestion(BookingState.bookingCancel);
            answer = cancelBooking(con);
        }

        QADao.getInstance().insertListQA(con);
        userProfileRepoCustom.saveUserProfile(UserProfiling.profiling(Collections.singletonList(con)));
        res.setStatus(HttpServletResponse.SC_OK);

        return answer;
    }

    private Answer query(Conversation con){
        return ConversationController.ma.process(con);
    }

    private Answer confirmBooking(Conversation con){
        if(BookingState.isBookingExpired(con)){
            con.getLatestAnswer().setWarningMessage(LibraryUtil.getRandomPatternByQuestionClass("Warning.BookingTermination").getSystemMessage());
            System.out.println("Previous booking is terminated: " + con.getLatestAnswer().getWarningMessage());
        }else {
            String confirmMessage = LibraryUtil.getRandomPatternByQuestionClass("Booking.ConfirmQuestion").getSystemMessage();

            RecommenderResultsRestaurant restaurant = Recommender.getInstance().getRestaurantInfoById(String.valueOf(con.getBookingRestaurantId()));
            if(restaurant!=null) confirmMessage = confirmMessage.replace("$RestaurantName",restaurant.getName());

            //BookingDao.getInstance().confirmBooking(BookingState.getQaWithBooking(con), con.getUserId(),con.getBookingRestaurantId());
            Booking booking = bookingRepo.findByQaIdAndUserId(BookingState.getQaWithBooking(con),con.getUserId());
            booking.setConfirm(1);
            bookingRepo.save(booking);

            con.getLatestQA().getAnswer().setMessage(confirmMessage);
        }
        //TODO: 3rd party booking
        return con.getLatestAnswer();
    }

    private Answer cancelBooking(Conversation con){
        if(BookingState.isBookingExpired(con)){
            con.getLatestAnswer().setWarningMessage(LibraryUtil.getRandomPatternByQuestionClass("Warning.BookingTermination").getSystemMessage());
            System.out.println("Previous booking is terminated: " + con.getLatestAnswer().getWarningMessage());
        }else {
            String cancelMessage = LibraryUtil.getRandomPatternByQuestionClass("Booking.CancelQuestion").getSystemMessage();
            //TODO; cancel and disable cancel of a while
            con.getLatestQA().getAnswer().setMessage(cancelMessage);
        }
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
}


