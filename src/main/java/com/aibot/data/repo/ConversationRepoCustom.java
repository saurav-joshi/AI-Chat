package com.aibot.data.repo;

import com.aibot.entity.Conversation;

public interface ConversationRepoCustom {
    public Conversation findCustomConByUserId(String userId);
}
