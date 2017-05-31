package com.aibot.data.repo;

import com.aibot.data.model.UserProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class UserProfileRepoImpl implements UserProfileRepoCustom {
    @Autowired
    UserProfileRepo userProfileRepo;

    @Override
    public com.aibot.entity.UserProfile findByUserIdCustom(String userId){
        com.aibot.entity.UserProfile userProfile = new com.aibot.entity.UserProfile();
        try{
            UserProfile originalUserProfile = userProfileRepo.findByUserId(userId);
            userProfile.setUserId(originalUserProfile.getUserId());
            userProfile.setName(originalUserProfile.getName());
            userProfile.setLikedRests(Arrays.stream(originalUserProfile.getLikedRests().split(","))
                    .filter(x->x.trim().length() != 0).collect(Collectors.toList()));
            userProfile.setDislikedRests(Arrays.stream(originalUserProfile.getDislikedRests().split(","))
                    .filter(x->x.trim().length() != 0).collect(Collectors.toList()));
            userProfile.setLikedDishes(Arrays.stream(originalUserProfile.getLikedDishes().split(","))
                    .filter(x->x.trim().length() != 0).collect(Collectors.toList()));
            userProfile.setDislikedDishes(Arrays.stream(originalUserProfile.getDislikedDishes().split(","))
                    .filter(x->x.trim().length() != 0).collect(Collectors.toList()));
            userProfile.setLikedCuisines(Arrays.stream(originalUserProfile.getLikedCuisines().split(","))
                    .filter(x->x.trim().length() != 0).collect(Collectors.toList()));
            userProfile.setDislikedCuisines(Arrays.stream(originalUserProfile.getDislikedCuisines().split(","))
                    .filter(x->x.trim().length() != 0).collect(Collectors.toList()));
            userProfile.setLikedLocations(Arrays.stream(originalUserProfile.getLikedLocations().split(","))
                    .filter(x->x.trim().length() != 0).collect(Collectors.toList()));
            userProfile.setDislikedLocations(Arrays.stream(originalUserProfile.getDislikedLocations().split(","))
                    .filter(x->x.trim().length() != 0).collect(Collectors.toList()));
            Map<String, Map<String, Map<String, Long>>> contextPreference = (Map<String, Map<String, Map<String, Long>>>) stringToMap(originalUserProfile.getContextPreference());
            Map<String,Map<String,List<String>>> likedRestAssociations = (Map<String,Map<String,List<String>>>) stringToMap(originalUserProfile.getLikedRestAssociations());
            userProfile.setContextPreference(contextPreference);
            userProfile.setLikedRestAssociations(likedRestAssociations);
        }catch (Exception e){
            return null;
        }
        return userProfile;
    }

    @Override
    public void saveUserProfile(com.aibot.entity.UserProfile userProfile){
        UserProfile userProfileFinal = new UserProfile();
        userProfileFinal.setUserId(userProfile.getUserId());
        userProfileFinal.setName(userProfile.getName());
        userProfileFinal.setLikedRests(listToString(userProfile.getLikedRests()));
        userProfileFinal.setDislikedRests(listToString(userProfile.getDislikedRests()));
        userProfileFinal.setLikedDishes(listToString(userProfile.getLikedDishes()));
        userProfileFinal.setDislikedDishes(listToString(userProfile.getDislikedDishes()));
        userProfileFinal.setLikedCuisines(listToString(userProfile.getLikedCuisines()));
        userProfileFinal.setDislikedCuisines(listToString(userProfile.getDislikedCuisines()));
        userProfileFinal.setLikedLocations(listToString(userProfile.getLikedLocations()));
        userProfileFinal.setDislikedLocations(listToString(userProfile.getDislikedLocations()));
        userProfileFinal.setContextPreference(mapToString(userProfile.getContextPreference()));
        userProfileFinal.setLikedRestAssociations(mapToString(userProfile.getLikedRestAssociations()));
        userProfileRepo.save(userProfileFinal);
    }

    @Override
    public String getUserName(String userId){
        UserProfile originalUserProfile = userProfileRepo.findByUserId(userId);
        return originalUserProfile.getName();
    }

    private String listToString(List<String> alist){
        return String.join(",", alist);
    }

    private String mapToString(Object map2Convert){
        String resultStr = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            resultStr = mapper.writeValueAsString(map2Convert);
        } catch (IOException e) {
            System.out.println("Exception when converting map to string");
            e.printStackTrace();
        }
        return resultStr;
    }

    private Object stringToMap(String strToConvert){
        Object resultObj = null;
        if(strToConvert != null && strToConvert.trim().length() != 0){
            ObjectMapper mapper1 = new ObjectMapper();
            try {
                resultObj = mapper1.readValue(strToConvert, Map.class);
            } catch (Exception e) {
                System.out.println("Exception when converting string to map");
                e.printStackTrace();
            }
        }
        return resultObj;
    }


}
