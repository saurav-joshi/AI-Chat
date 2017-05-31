package com.aibot.controller;

import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;

import com.aibot.dao.ConversationDao;
import com.aibot.data.model.Customer;
import com.aibot.data.model.CustomerApp;
import com.aibot.data.repo.CustomerAppRepo;
import com.aibot.entity.*;
import com.aibot.utils.EncryptUtil;
import com.aibot.data.repo.UserProfileRepoImpl;
import com.aibot.entity.*;
import com.aibot.entity.*;
import com.aibot.qa.LibraryUtil;
import com.aibot.recommender.Recommender;
import com.aibot.recommender.UserProfiling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController extends BaseController {
    static ConversationDao convdao =  ConversationDao.getInstance();
    @Autowired
    CustomerAppRepo customerRepoCustom;
    @Autowired
    UserProfileRepoImpl userProfileRepoCustom;

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public Object receive(@RequestBody UserResp user, HttpServletResponse res) {
    	CustomerApp cusApp = customerRepoCustom.findByApplicationId(user.getApplicationId());
        Customer cus = null;
        if(cusApp!=null){
            cus = cusApp.getCustomer();
        }
        //CustomerDao.getInstance().getCustomer(user.getApplicationId());
    	if (cus == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return getRespErr("Invalid application id.");
        }
    	if (user.getUserName() == null || user.getUserName().trim().equals("")) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return getRespErr("Please provide username.");
        }

        String token = EncryptUtil.generateToken(user.getApplicationId());
        UserProfile uProfile = new UserProfile();
        uProfile.setUserId(token);
        uProfile.setName(user.getUserName());
        userProfileRepoCustom.saveUserProfile(uProfile);
        user.setToken(token);
        res.setStatus(HttpServletResponse.SC_OK);
        return user;
    }

    @RequestMapping(value = "/like", method = RequestMethod.POST)
    public Object like(@RequestBody LikeReq like, HttpServletResponse res) {
        if (like.getRestIds() == null || like.getRestIds().isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return getRespErr("Invalid restaurant id.");
        }
        UserProfile up = userProfileRepoCustom.findByUserIdCustom(like.getToken());
        if (up == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return getRespErr("Invalid token key.");
        }

        Collection<String> newLikes = new HashSet<>(up.getLikedRests());
        newLikes.addAll(like.getRestIds());
        up.setLikedRests(new ArrayList<>(newLikes));
        // add the associations of the liked restaurants to user profile
        QA previousQa = convdao.selectConversation(up.getUserId()) == null? null :convdao.selectConversation(up.getUserId()).getLatestQA();
        if(previousQa != null && previousQa.getEntities() != null && previousQa.getEntities().size() != 0){
            up.setLikedRestAssociations(UserProfiling.updateLikedRestAssociations(up, previousQa.getEntities(), like.getRestIds()));
        }
        Collection<String> newDislikes = new HashSet<>(up.getDislikedRests());
        newDislikes.removeAll(newLikes);
        up.setDislikedRests(new ArrayList<>(newDislikes));
        userProfileRepoCustom.saveUserProfile(up);

        Answer answer = new Answer();
        List<RecommenderResultsRestaurant> likedRestInfo = Recommender.getInstance().getRestaurantsInfoByIds(like.getRestIds());
        String restName = String.join(",", likedRestInfo.stream().map(e -> e.getName()).collect(Collectors.toList()));
        answer.setMessage("Oh, you like " + restName + "! Cool! Let me save this for you. This is how I get to know you better :)");
        LibraryUtil.Pattern disPattern = LibraryUtil.getRandomPatternByQuestionClass("Clarify.Like");
        // add suggestions when user click on like
        //answer.setSuggestion(new Suggestion(disPattern.getSystemMessage(), Arrays.asList(disPattern.getPatternAtt().trim().split("\\|\\|")), "Clarify.Like"));
        res.setStatus(HttpServletResponse.SC_OK);
        return answer;
    }

    @RequestMapping(value = "/dislike", method = RequestMethod.POST)
    public Object dislike(@RequestBody LikeReq dislike, HttpServletResponse res) {
        if (dislike.getRestIds() == null || dislike.getRestIds().isEmpty()) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return getRespErr("Invalid restaurant id.");
        }
        UserProfile up = userProfileRepoCustom.findByUserIdCustom(dislike.getToken());
        if (up == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return getRespErr("Invalid token key.");
        }

        Collection<String> newDislikes = new HashSet<>(up.getDislikedRests());
        newDislikes.addAll(dislike.getRestIds());
        Collection<String> newLikes = new HashSet<>(up.getLikedRests());
        newLikes.removeAll(newDislikes);
        up.setDislikedRests(new ArrayList<>(newDislikes));
        up.setLikedRests(new ArrayList<>(newLikes));
        userProfileRepoCustom.saveUserProfile(up);

        Answer answer = new Answer();
        List<RecommenderResultsRestaurant> dislikedRestInfo = Recommender.getInstance().getRestaurantsInfoByIds(dislike.getRestIds());
        String restName = String.join(",", dislikedRestInfo.stream().map(e -> e.getName().trim()).collect(Collectors.toList()));
        answer.setMessage("Noted. I won't show " + restName + " again in the future.");
        res.setStatus(HttpServletResponse.SC_OK);
        return answer;
    }
}

