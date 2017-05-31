package com.aibot.data.repo;

import com.aibot.entity.UserProfile;

public interface UserProfileRepoCustom {
    public UserProfile findByUserIdCustom(String userId);
    public void saveUserProfile(UserProfile userProfile);
    public String getUserName(String userId);
}
