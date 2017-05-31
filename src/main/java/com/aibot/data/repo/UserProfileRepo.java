package com.aibot.data.repo;

import com.aibot.data.model.UserProfile;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface UserProfileRepo extends PagingAndSortingRepository<UserProfile, String> {
    public UserProfile findByUserId(String userId);
}
