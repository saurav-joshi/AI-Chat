package com.aibot.entity;

public class CustomerRep {
	private long id;
	private String name;
    private String password;
    private String phone;
    private String email;
    private String companyName;
    private boolean isAdmin;
    private boolean isSystemAdmin;
    private long parentId;
    private long contractStarted;
    private long contractEnded;
    private long requestLimited;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getCompanyName() {
		return companyName;
	}
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}
	public boolean getIsAdmin() {
		return isAdmin;
	}
	public void setIsAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	public boolean getIsSystemAdmin() {
		return isSystemAdmin;
	}
	public void setIsSystemAdmin(boolean isSystemAdmin) {
		this.isSystemAdmin = isSystemAdmin;
	}
	public long getParentId() {
		return parentId;
	}
	public void setParentId(long parentId) {
		this.parentId = parentId;
	}
	public long getContractStarted() {
		return contractStarted;
	}
	public void setContractStarted(long contractStarted) {
		this.contractStarted = contractStarted;
	}
	public long getContractEnded() {
		return contractEnded;
	}
	public void setContractEnded(long contractEnded) {
		this.contractEnded = contractEnded;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public long getRequestLimited() {
		return requestLimited;
	}
	public void setRequestLimited(long requestLimited) {
		this.requestLimited = requestLimited;
	}
}


