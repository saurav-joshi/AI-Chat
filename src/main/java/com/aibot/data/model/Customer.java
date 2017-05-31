package com.aibot.data.model;

import javax.persistence.*;

import java.sql.Timestamp;

@Entity
@Table(name = "customer")
public class Customer {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private long id;
    private String name;
    private String password;
    private String phone;
    private String email;
    @Column(name = "company_name")
    private String companyName;
    @Column(name = "created_date")
    private Timestamp createdDate;
    private int status;
    @Column(name = "is_admin")
    private boolean isAdmin;
    @Column(name = "is_system_admin")
    private boolean isSystemAdmin;
    @Column(name = "parent_id")
    private long parentId;
    @Column(name = "contract_started")
    private Timestamp contractStarted;
    @Column(name = "contract_ended")
    private Timestamp contractEnded;
    @Column(name = "request_limited")
    private long requestLimited;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(boolean admin) {
        isAdmin = admin;
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

	public Timestamp getContractStarted() {
		return contractStarted;
	}

	public void setContractStarted(Timestamp contractStarted) {
		this.contractStarted = contractStarted;
	}

	public Timestamp getContractEnded() {
		return contractEnded;
	}

	public void setContractEnded(Timestamp contractEnded) {
		this.contractEnded = contractEnded;
	}

	public long getRequestLimited() {
		return requestLimited;
	}

	public void setRequestLimited(long requestLimited) {
		this.requestLimited = requestLimited;
	}
}
