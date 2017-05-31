package com.aibot.entity;

import java.util.List;

public class LikeReq {
    private String token;
    private List<String> restIds;
    
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public List<String> getRestIds() {
		return restIds;
	}
	public void setRestIds(List<String> restIds) {
		this.restIds = restIds;
	}
}


