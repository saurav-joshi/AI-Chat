package com.aibot.data.repo;

import com.aibot.data.model.Conversation;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ConversationRepo extends PagingAndSortingRepository<Conversation, Long> {
    public Conversation findByUserProfileUserId(String userId);
}
