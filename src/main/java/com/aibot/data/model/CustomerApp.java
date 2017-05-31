package com.aibot.data.model;

import javax.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "customer_app")
public class CustomerApp {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    @Column(name = "id")
    private long id;
    //relationship
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id")
    private Customer customer;
    //@Column(name = "customer_id")
    //private long customerId;
    @Column(name = "application_name")
    private String applicationName;
    @Column(name = "application_id")
    private String applicationId;
    @Column(name = "created_date")
    private Timestamp createdDate;
    @Column(name = "expired_date")
    private Timestamp expireddDate;
    @Column(name = "request_limited")
    private long requestLimited;
    @Column(name = "request_counted")
    private long requestCounted;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
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

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public Timestamp getExpireddDate() {
        return expireddDate;
    }

    public void setExpireddDate(Timestamp expireddDate) {
        this.expireddDate = expireddDate;
    }

    public long getRequestLimited() {
        return requestLimited;
    }

    public void setRequestLimited(long requestLimited) {
        this.requestLimited = requestLimited;
    }

	public long getRequestCounted() {
		return requestCounted;
	}

	public void setRequestCounted(long requestCounted) {
		this.requestCounted = requestCounted;
	}
}
