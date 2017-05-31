package com.aibot.data.model;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "customer_query")
public class CustomerQuery {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    @Column(name = "id")
    private long id;
    @Column(name = "application_id")
    private String applicationId;
    @Column(name = "token")
    private String token;
    @Column(name = "queried_date")
    private Timestamp queriedDate;
    @Column(name = "is_success")
    private boolean isSuccess;
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getApplicationId() {
		return applicationId;
	}
	public void setApplicationId(String applicationId) {
		this.applicationId = applicationId;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public Timestamp getQueriedDate() {
		return queriedDate;
	}
	public void setQueriedDate(Timestamp queriedDate) {
		this.queriedDate = queriedDate;
	}
	public boolean getIsSuccess() {
		return isSuccess;
	}
	public void setIsSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}

    
}
