package com.aibot.data.model;

import com.aibot.entity.Answer;
import com.aibot.entity.Clarification;
import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.entity.RecommenderResultsRestaurant;
import com.aibot.qa.LibraryUtil;
import com.aibot.state.State;
import groovy.lang.Tuple2;

import javax.persistence.*;
import java.util.*;

//@Entity
//@Table (name = "qa")
public class Qa {
    @Id
    @Column (name = "qa_id")
    int qaId;
    @ManyToOne(fetch = FetchType.EAGER, cascade={CascadeType.ALL})
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;
    String question;
    @Column (name = "answer")
    String answerMessage;
    String choice;
    String timestamp;
    @Column (name = "$actionword")
    String actionWord;
    @Column (name = "$beverage")
    String beverage;
    @Column (name = "$chef")
    String chef;
    @Column (name = "$cookingmethod")
    String cookingMethod;
    @Column (name = "$country")
    String country;
    @Column (name = "$dish")
    String dish;
    @Column (name = "$establishmenttype")
    String establishmentType;
    @Column (name = "$event")
    String event;
    @Column (name = "$ingredient")
    String ingredient;
    @Column (name = "$location")
    String location;
    @Column (name = "$mealtype")
    String mealType;
    @Column (name = "$nationality")
    String nationality;
    @Column (name = "$refineestablishmenttype")
    String refineEstablishmentType;
    @Column (name = "$refinelocation")
    String refineLocation;
    @Column (name = "$religious")
    String religious;
    @Column (name = "$restaurantFeature")
    String restaurantFeature;
    @Column (name = "$accompany")
    String accompany;
    @Column (name = "$occasion")
    String occasion;
    @Column (name = "$regular")
    String regular;
    @Column (name = "$restaurantname")
    String restaurantName;
    @Column (name = "location")
    String locationWithPreposition;
    String cuisine;
    String restaurantEntity;
    @Column (name = "$pricerange")
    String priceRange;
    @Column (name = "$offer")
    String offer;
    @Column (name = "$accolade")
    String accolade;
    @Column (name = "$regional")
    String regional;
    @Column (name = "$distance")
    String distance;
    String geo;
    @Column (name = "library_id")
    int libraryId;
    String states;
    String suggestion;
    String originalQuestion;
    String city;

    public int getQaId() {
        return qaId;
    }

    public void setQaId(int qaId) {
        this.qaId = qaId;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswerMessage() {
        return answerMessage;
    }

    public void setAnswerMessage(String answerMessage) {
        this.answerMessage = answerMessage;
    }

    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getActionWord() {
        return actionWord;
    }

    public void setActionWord(String actionWord) {
        this.actionWord = actionWord;
    }

    public String getBeverage() {
        return beverage;
    }

    public void setBeverage(String beverage) {
        this.beverage = beverage;
    }

    public String getChef() {
        return chef;
    }

    public void setChef(String chef) {
        this.chef = chef;
    }

    public String getCookingMethod() {
        return cookingMethod;
    }

    public void setCookingMethod(String cookingMethod) {
        this.cookingMethod = cookingMethod;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDish() {
        return dish;
    }

    public void setDish(String dish) {
        this.dish = dish;
    }

    public String getEstablishmentType() {
        return establishmentType;
    }

    public void setEstablishmentType(String establishmentType) {
        this.establishmentType = establishmentType;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getIngredient() {
        return ingredient;
    }

    public void setIngredient(String ingredient) {
        this.ingredient = ingredient;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getRefineEstablishmentType() {
        return refineEstablishmentType;
    }

    public void setRefineEstablishmentType(String refineEstablishmentType) {
        this.refineEstablishmentType = refineEstablishmentType;
    }

    public String getRefineLocation() {
        return refineLocation;
    }

    public void setRefineLocation(String refineLocation) {
        this.refineLocation = refineLocation;
    }

    public String getReligious() {
        return religious;
    }

    public void setReligious(String religious) {
        this.religious = religious;
    }

    public String getRestaurantFeature() {
        return restaurantFeature;
    }

    public void setRestaurantFeature(String restaurantFeature) {
        this.restaurantFeature = restaurantFeature;
    }

    public String getAccompany() {
        return accompany;
    }

    public void setAccompany(String accompany) {
        this.accompany = accompany;
    }

    public String getOccasion() {
        return occasion;
    }

    public void setOccasion(String occasion) {
        this.occasion = occasion;
    }

    public String getRegular() {
        return regular;
    }

    public void setRegular(String regular) {
        this.regular = regular;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getLocationWithPreposition() {
        return locationWithPreposition;
    }

    public void setLocationWithPreposition(String locationWithPreposition) {
        this.locationWithPreposition = locationWithPreposition;
    }

    public String getCuisine() {
        return cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public String getRestaurantEntity() {
        return restaurantEntity;
    }

    public void setRestaurantEntity(String restaurantEntity) {
        this.restaurantEntity = restaurantEntity;
    }

    public String getPriceRange() {
        return priceRange;
    }

    public void setPriceRange(String priceRange) {
        this.priceRange = priceRange;
    }

    public String getOffer() {
        return offer;
    }

    public void setOffer(String offer) {
        this.offer = offer;
    }

    public String getAccolade() {
        return accolade;
    }

    public void setAccolade(String accolade) {
        this.accolade = accolade;
    }

    public String getRegional() {
        return regional;
    }

    public void setRegional(String regional) {
        this.regional = regional;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public int getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(int libraryId) {
        this.libraryId = libraryId;
    }

    public String getStates() {
        return states;
    }

    public void setStates(String states) {
        this.states = states;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    public String getOriginalQuestion() {
        return originalQuestion;
    }

    public void setOriginalQuestion(String originalQuestion) {
        this.originalQuestion = originalQuestion;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }






    @Transient
    Set<Clarification> clarification;
    @Transient
    public Set<Clarification> getClarification() {
        return clarification;
    }
    @Transient
    public void setClarification(Set<Clarification> clarification) {
        this.clarification = clarification;
    }
    @Transient
    List<String> cleanedQuestionPatternWords;
    @Transient
    Answer answer = new Answer();
    @Transient
    List<EntityExtractionUtil.EntityExtractionResult> entities;
    @Transient
    int matchedPatternIdInLibrary;
    @Transient
    Date time;
    @Transient
    List<State.StateType> statePaths = new ArrayList<>();
    @Transient
    public LibraryUtil.Pattern getMatchedPattern() {
        return LibraryUtil.allPatternsMap.get(this.getMatchedPatternIdInLibrary());
    }
    @Transient
    public void setMatchedPattern(LibraryUtil.Pattern pattern) {
        this.matchedPatternIdInLibrary = pattern.getId();
    }

    @Transient
    public List<String> getCleanedQuestionPatternWords() {
        return cleanedQuestionPatternWords;
    }
    @Transient
    public List<EntityExtractionUtil.EntityExtractionResult> getEntities() {
        return entities;
    }
    @Transient
    public void setEntities(List<EntityExtractionUtil.EntityExtractionResult> entities) {
        this.entities = entities;

    }
    @Transient
    public void setCleanedQuestionPatternWords(List<String> cleanedQuestionPatternWords) {
        this.cleanedQuestionPatternWords = cleanedQuestionPatternWords;
    }
    @Transient
    public void setEntities(Tuple2<String, String> entities) {
        this.entities = entities;
    }
    @Transient
    public int getMatchedPatternIdInLibrary() {
        return matchedPatternIdInLibrary;
    }
    @Transient
    public void setMatchedPatternIdInLibrary(int matchedPatternIdInLibrary) {
        this.matchedPatternIdInLibrary = matchedPatternIdInLibrary;
    }
    @Transient
    public Date getTime() {
        return time;
    }
    @Transient
    public void setTime(Date time) {
        this.time = time;
        this.timestamp = time.toString();
    }
    @Transient
    public List<State.StateType> getStatePaths() {
        return statePaths;
    }
    @Transient
    public void setStatePaths(List<State.StateType> statePaths) {
        this.statePaths = statePaths;

        //state
        if(!statePaths.isEmpty()){
            StringBuilder sb = new StringBuilder();
            statePaths.stream().forEach(stateType -> {
                sb.append(",").append(stateType.name());
            });
            this.setStates(sb.toString().substring(1));
        }
    }
    @Transient
    public Answer getAnswer() {
        return this.answer;
    }
    @Transient
    public void setAnswer(Answer answer) {
        //message
        setAnswerMessage(answer.getMessage());
        //choice
        List<String> restaurantIds = new ArrayList<>();
        List<RecommenderResultsRestaurant> results = answer.getResultRestaurants();
        if(results != null)
            results.forEach(r -> {
                restaurantIds.add(r.getId());
            });
        String choice = String.join("||", restaurantIds);
        setChoice(choice);
        //suggestion
        String suggestion = answer.getSuggestion() == null ? null : answer.getSuggestion().toString();
        setSuggestion(suggestion);
        this.answer = answer;
    }
}
