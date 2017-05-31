package com.aibot.data.model;

import javax.persistence.*;

@Entity
@Table(name = "conversation")
public class Conversation {
	@Id  @GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name = "conversation_id")
	private long conversationId;
	
	@ManyToOne(fetch = FetchType.EAGER, cascade={CascadeType.MERGE})
    @JoinColumn(name = "user_id")
	private UserProfile userProfile;

	public long getConversationId() {
		return conversationId;
	}

	public void setConversationId(long conversationId) {
		this.conversationId = conversationId;
	}

	public UserProfile getUserProfile() {
		return userProfile;
	}

	public void setUserProfile(UserProfile userProfile) {
		this.userProfile = userProfile;
	}

}
