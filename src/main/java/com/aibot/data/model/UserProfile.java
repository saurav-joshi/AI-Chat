package com.aibot.data.model;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "userprofile")
public class UserProfile {
	@Id
	@Column(name = "user_id")
	private String userId;
	private String name;
	private String likedRests;
	private String dislikedRests;
	private String likedDishes;
	private String dislikedDishes;
	private String likedCuisines;
	private String dislikedCuisines;
	private String likedLocations;
	private String dislikedLocations;
	private String contextPreference;
	private String likedRestAssociations;
	@OneToMany(fetch = FetchType.EAGER, mappedBy = "userProfile",cascade={CascadeType.ALL})
	private Set<Conversation> conversations;
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLikedRests() {
		return likedRests;
	}
	public void setLikedRests(String likedRests) {
		this.likedRests = likedRests;
	}
	public String getDislikedRests() {
		return dislikedRests;
	}
	public void setDislikedRests(String dislikedRests) {
		this.dislikedRests = dislikedRests;
	}
	public String getLikedDishes() {
		return likedDishes;
	}
	public void setLikedDishes(String likedDishes) {
		this.likedDishes = likedDishes;
	}
	public String getDislikedDishes() {
		return dislikedDishes;
	}
	public void setDislikedDishes(String dislikedDishes) {
		this.dislikedDishes = dislikedDishes;
	}
	public String getLikedCuisines() {
		return likedCuisines;
	}
	public void setLikedCuisines(String likedCuisines) {
		this.likedCuisines = likedCuisines;
	}
	public String getDislikedCuisines() {
		return dislikedCuisines;
	}
	public void setDislikedCuisines(String dislikedCuisines) {
		this.dislikedCuisines = dislikedCuisines;
	}
	public String getLikedLocations() {
		return likedLocations;
	}
	public void setLikedLocations(String likedLocations) {
		this.likedLocations = likedLocations;
	}
	public String getDislikedLocations() {
		return dislikedLocations;
	}
	public void setDislikedLocations(String dislikedLocations) {
		this.dislikedLocations = dislikedLocations;
	}
	public String getContextPreference() {
		return contextPreference;
	}
	public void setContextPreference(String contextPreference) {
		this.contextPreference = contextPreference;
	}
	public String getLikedRestAssociations() {
		return likedRestAssociations;
	}
	public void setLikedRestAssociations(String likedRestAssociations) {
		this.likedRestAssociations = likedRestAssociations;
	}
	public Set<Conversation> getConversations() {
		return conversations;
	}
	public void setConversations(Set<Conversation> conversations) {
		this.conversations = conversations;
	}
	

}
