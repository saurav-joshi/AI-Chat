package com.aibot.entity;

public class CustomerAppRep {
	private long id;
	private long customerId;
    private String applicationName;
    private String applicationId;
    private long requestLimited;
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getCustomerId() {
		return customerId;
	}
	public void setCustomerId(long customerId) {
		this.customerId = customerId;
	}
	public String getApplicationName() {
		return applicationName;
	}
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}
	public String getApplicationId() {
		return applicationId;
	}
	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}
	public long getRequestLimited() {
		return requestLimited;
	}
	public void setRequestLimited(long requestLimited) {
		this.requestLimited = requestLimited;
	}
}


