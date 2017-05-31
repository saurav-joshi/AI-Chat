package com.aibot.entity;

import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.qa.GeoCalculator;
import com.aibot.dao.QADao;
import com.aibot.entityextraction.LocationFromAddress;
import com.aibot.qa.GlobalConstants;
import com.aibot.state.State;
import org.apache.commons.collections.CollectionUtils;
import scala.Tuple2;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Conversation {

    int id;
    List<QA> qaList = new ArrayList<>();
    String userId;
    int numOfShowMore;
    int numOfShowMoreHasResult;
    int bookingRestaurantId =0;
    boolean isActivateBooking = false;

    public boolean isPreProcess() {
        return isPreProcess;
    }

    public void setPreProcess(boolean preProcess) {
        isPreProcess = preProcess;
    }

    boolean isPreProcess = false;

    public boolean isActivateBooking() {
        return isActivateBooking;
    }

    public void setActivateBooking(boolean activateBooking) {
        isActivateBooking = activateBooking;
    }

    public int getNumOfShowMoreHasResult() {
        return numOfShowMoreHasResult;
    }

    public void setNumOfShowMoreHasResult(int numOfShowMoreHasResult) {
        this.numOfShowMoreHasResult = numOfShowMoreHasResult;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    String userName;

    boolean isLocationExpand;

    boolean isBookingExpired;

    public Conversation() {

    }

    public String getLatestQueston() {
        return getLatestQA().getQuestion();
    }

    public Answer getLatestAnswer() {
        return getLatestQA().getAnswer();
    }

    public List<State.StateType> getLatestStatePaths() {
        return getLatestQA().getStatePaths();
    }

    public List<EntityExtractionUtil.EntityExtractionResult> getLatestEntities() {
        return getLatestQA().getEntities();
    }

    public QA getLatestQAWhichIncludeStateTypes(List<State.StateType> statetypes) {
        return getLatestQAWhichIncludeStateTypes(statetypes, false, false, null);
    }

    public QA getLatestQAWhichIncludeStateTypes(List<State.StateType> statetypes, boolean withinCurrentSession, boolean continous, List<State.StateType> skipStates) {
        QA lastQA = null;
        List<QA> qaListToSearchFor = this.getQaList();
        if(withinCurrentSession){
            qaListToSearchFor = QADao.getInstance().getQACurrentSession(this.getQaList());
        }
        for (int i = qaListToSearchFor.size() - 2; i > -1; i--) {
            if (CollectionUtils.intersection(qaListToSearchFor.get(i).getStatePaths(), statetypes).size() > 0) {
                lastQA = qaListToSearchFor.get(i);
                break;
            }else if(continous){
                if(CollectionUtils.intersection(qaListToSearchFor.get(i).getStatePaths(), skipStates).size() <= 0){
                    break;
                }
            }
        }
        return lastQA;
    }

    public QA getLatestQAWhichIncludeSuggestion(String suggestionType) {
        QA lastQA = null;
        for (int i = this.getQaList().size() - 2; i > -1; i--) {
            Suggestion suggestioni = getQaList().get(i).getAnswer().getSuggestion();
            if(suggestioni != null && suggestioni.getType() != null && suggestioni.getType().equalsIgnoreCase(suggestionType)){
                lastQA = getQaList().get(i);
                break;
            }
        }
        return lastQA;
    }

    public QA getLatestQAByStateTypes(List<State.StateType> statetypes) {
        QA lastQA = null;
        for (int i = this.getQaList().size() - 2; i > -1; i--) {
            if(getQaList().get(i).getStatePaths().toString().equalsIgnoreCase(statetypes.toString())){
                lastQA = getQaList().get(i);
                break;
            }
        }
        return lastQA;
    }

    public boolean isLastQAWithinTolerantTime(){
        QA latestQA = this.getQaList().size() >= 1 ? this.getLatestQA() : null;
        QA secondLastQA = this.getQaList().size() >= 2 ? this.getQaList().get(this.getQaList().size()-2) : null;
        return isLastQAWithinTolerantTime(latestQA,secondLastQA);
    }

    public boolean isLastQAWithinTolerantTime(QA qa1, QA qa2){
        if(qa1 == null || qa2 == null){
            return false;
        }
        long duration = qa1.getTime().getTime() - qa2.getTime().getTime();
        long diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        if(diffInMinutes < GlobalConstants.refineQuestionToleranceInterval)
            return true;
        else
            return false;
    }

    public List<String> getRecommendRestaurantIdInShowMore(){
        List<String> ids = new ArrayList<>();
        for (int i = this.getQaList().size() - 2; i > -1; i--) {
            QA qa = this.getQaList().get(i);
            if (qa.getStatePaths().contains(State.StateType.ShowMoreState)){
                ids.addAll(qa.getRecommendRestaurantIds());
            }
            else {
                ids.addAll(qa.getRecommendRestaurantIds());
                break;
            }
        }
        return ids;
    }

    public Conversation(String question) {
        QA qa = new QA();
        qa.setQuestion(question);
        qa.setTime(new Date());
        addQA(qa);
    }

    public List<QA> getQaList() {
        return qaList;
    }

    public void setQaList(List<QA> qaList) {
        this.qaList = qaList;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public QA getLatestQA() {
        return qaList.get(qaList.size() - 1);
    }

    public String getLastKnownCityByGeo() {
        String city = "";
        for(int i= qaList.size()-1 ; i >= 0; i--){
            QA qa = qaList.get(i);
            if(qa.getGeo()!=null){
                city = GeoCalculator.getCityFromLatLongOpenMap(qa.getGeo().split(",")[0], qa.getGeo().split(",")[1]);
                break;
            }
        }
        return city;
    }

    public Conversation addQA(QA qa) {
        this.getQaList().add(qa);
        return this;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNumOfShowMore() {
        return numOfShowMore;
    }

    public void setNumOfShowMore(int numOfShowMore) {
        this.numOfShowMore = numOfShowMore;
    }

    public boolean getLocationExpand() {
        return isLocationExpand;
    }

    public void setLocationExpand(boolean locationExpand) {
        isLocationExpand = locationExpand;
    }

    public int getBookingRestaurantId() {
        return bookingRestaurantId;
    }

    public void setBookingRestaurantId(int bookingRestaurantId) {
        this.bookingRestaurantId = bookingRestaurantId;
    }

    public boolean isLocationExpand() {
        return isLocationExpand;
    }

    public boolean isBookingExpired() {
        return isBookingExpired;
    }

    public void setBookingExpired(boolean bookingExpired) {
        isBookingExpired = bookingExpired;
    }

    public String getCurrentCityOfUser(){
        String city = "";
        try{
            List<EntityExtractionUtil.EntityExtractionResult> latestEntities = getLatestEntities();
            //1st priority is city was mentioned explicitly, even more than one, choose the first one, open for disambiguous optimization
            if(latestEntities.stream().filter(x -> x.getEntityName().contains("city")).count()>0){
                EntityExtractionUtil.EntityExtractionResult cityEntity = latestEntities.stream().filter(x -> x.getEntityName().contains("city")).findFirst().get();
                return String.join(" ", cityEntity.getEntityValue());
            }
            String geo = getLatestQA().getGeo();
            boolean validGeo = (geo != null && geo.split(",").length == 2);
            if (validGeo) {
                city = GeoCalculator.getCityFromLatLongOpenMap(getLatestQA().getGeo().split(",")[0], getLatestQA().getGeo().split(",")[1]);
            }

            //2nd priority is to infer from mentioned location, even more than one, choose the first one, open for disambiguous optimization
            List<EntityExtractionUtil.EntityExtractionResult> locationEntities = latestEntities.stream().filter(x -> (x.getEntityName().equals("#location")
                    || x.getEntityName().equals("$location"))).collect(Collectors.toList());//ignore $refinelocation entities like "closer", "nearer"...
            if (locationEntities != null && locationEntities.size() > 0) {
                EntityExtractionUtil.EntityExtractionResult locationEntity = locationEntities.stream().findFirst().get();
                //remove preposition like in/near/at...
                String mentionedLocation = String.join(" ", locationEntity.getEntityValue()).replaceAll("^\\b(in|on|near|within|along|close to|at|around|next to|across|to)\\b", "").trim();
                List<Tuple2<String, String>> cityCountries = LocationFromAddress.getCityAndCountryByLocation(mentionedLocation);
                if (cityCountries != null ) {
                    if(cityCountries.size()==1 || cityCountries.stream().allMatch(x -> x._1().equalsIgnoreCase(cityCountries.get(0)._1())))
                        return cityCountries.get(0)._1();

                    if(cityCountries.size()>1){
                        if (validGeo) {
                            String geoCity = city;
                            if(cityCountries.stream().anyMatch(x -> x._1().equalsIgnoreCase(geoCity))){
                                return cityCountries.stream().filter(x -> x._1().equalsIgnoreCase(geoCity)).findFirst().get()._1();
                            }else{
                                return String.join("||",cityCountries.stream().map(x -> x._1()).distinct().collect(Collectors.toList()));
                            }
                        }else{
                            return String.join("||",cityCountries.stream().map(x -> x._1()).distinct().collect(Collectors.toList()));
                        }
                    }
                }
            }
            if(city.isEmpty()){//try last known geo
                return getLastKnownCityByGeo();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return city.toLowerCase();
    }
}


