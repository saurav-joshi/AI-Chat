package com.aibot.entity;

public class RecommenderResultsReview {
    String id;
    String restaurantId;
    String reviewText;

    public RecommenderResultsReview(String id, String restaurantId, String reviewText) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.reviewText = reviewText;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }

    public void print(){
        System.out.println("========================");
        System.out.println("-ReviewId:" + id);
        System.out.println("-RestaurantId:" + restaurantId);
        System.out.println("-ReviewText:" + reviewText);
    }
}
