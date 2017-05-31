package com.aibot.entity;

import com.aibot.data.model.Booking;

import java.util.List;

public class Answer {
    private String message;
    private List<RecommenderResultsRestaurant> resultRestaurants;
    private String explainerMessage;
    private boolean confirmBooking = false;
    private Booking bookingInfo;
    private Suggestion suggestion;
    private String warningMessage;

    public Answer(){

    }

    public Answer(String message,
            List<RecommenderResultsRestaurant> resultRestaurants,
            String explainerMessage){
        this.message = message;
        this.resultRestaurants = resultRestaurants;
        this.explainerMessage = explainerMessage;
    }

    public Answer(String message,
                  List<RecommenderResultsRestaurant> resultRestaurants,
                  String explainerMessage,
                  Suggestion suggestion){
        this.message = message;
        this.resultRestaurants = resultRestaurants;
        this.explainerMessage = explainerMessage;
        this.suggestion = suggestion;
    }

    public String getExtractedEntity() {
        return extractedEntity;
    }

    public void setExtractedEntity(String extractedEntity) {
        this.extractedEntity = extractedEntity;
    }

    public String extractedEntity;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<RecommenderResultsRestaurant> getResultRestaurants() {
        return resultRestaurants;
    }

    public void setResultRestaurants(List<RecommenderResultsRestaurant> resultRestaurants) {
        this.resultRestaurants = resultRestaurants;
    }

    public String getExplainerMessage() {
        return explainerMessage;
    }

    public void setExplainerMessage(String explainerMessage) {
        this.explainerMessage = explainerMessage;
    }

	public boolean getConfirmBooking() {
		return confirmBooking;
	}

	public void setConfirmBooking(boolean confirmBooking) {
		this.confirmBooking = confirmBooking;
	}

    public Booking getBookingInfo() {
        return bookingInfo;
    }

    public void setBookingInfo(Booking bookingInfo) {
        this.bookingInfo = bookingInfo;
    }

    public boolean isConfirmBooking() {
        return confirmBooking;
    }

    public Suggestion getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(Suggestion suggestion) {
        this.suggestion = suggestion;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    public void setWarningMessage(String warningMessage) {
        this.warningMessage = warningMessage;
    }
}
