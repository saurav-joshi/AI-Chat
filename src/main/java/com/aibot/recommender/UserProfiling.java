package com.aibot.recommender;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.aibot.data.repo.UserProfileRepoImpl;
import com.aibot.entity.Conversation;
import com.aibot.entity.QA;
import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.entity.UserProfile;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.collections.map.LinkedMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class UserProfiling {

    private static UserProfileRepoImpl userProfileRepo;
    @Autowired
    UserProfileRepoImpl aUserProfileRepo;

    @PostConstruct
    public void init() {
        this.userProfileRepo = aUserProfileRepo;
    }

    private static int defaultNumberOfDaysForShortTermPreference = 7;
    public UserProfile updateProfile(UserProfile userProfile, Conversation conversation){
        Map<String, Map<String, Map<String,Long>>> cPreference = getPreferenceFromConversation(conversation);
        if(cPreference == null || cPreference.size() == 0){
            // return without saving, as the userprofile is not updated.
            return userProfile;
        }

        Map<String, Map<String, Map<String,Long>>> contextPreference = userProfile.getContextPreference();
        if(contextPreference == null || contextPreference.size() == 0){
            userProfile.setContextPreference(cPreference);
        }else{
            // merge the preferences
            cPreference.forEach((cx, ap)->{
                if(contextPreference.get(cx) == null){
                    contextPreference.put(cx, ap);
                }else{
                    contextPreference.put(cx, mergeMap(ap, contextPreference.get(cx)));
                }
            });
            userProfile.setContextPreference(contextPreference);
        }
        //save to db as the userprofile is updated.
        userProfileRepo.saveUserProfile(userProfile);
        return userProfile;
    }

    public static UserProfile profiling(Conversation conversation){
        List<Conversation> convs = new ArrayList<>();
        convs.add(conversation);
        return profiling(convs);
    }

    public static UserProfile profiling(List<Conversation> conversations){
        UserProfile userProfile = null;
        Map<String, Map<String, Map<String,Long>>>contextPreference = new LinkedMap();
        String userId = null;

        for(Conversation conv : conversations){
            if(userId == null){
                userId = conv.getUserId();
                userProfile = userProfileRepo.findByUserIdCustom(userId);
            }

            Map<String, Map<String, Map<String,Long>>> cPreference = getPreferenceFromConversation(conv);
            if(cPreference != null && cPreference.size() != 0){
                if(contextPreference.size() == 0){
                    contextPreference.putAll(cPreference);
                }else{
                    cPreference.forEach((cx, ap)->{
                        if(contextPreference.get(cx) == null){
                            contextPreference.put(cx, ap);
                        }else{
                            Map<String, Map<String,Long>> mergedAp = mergeMap(ap, contextPreference.get(cx));
                            contextPreference.put(cx, mergedAp);
                        }
                    });
                }
            }
        }
        if (userProfile == null) {
        	userProfile = new UserProfile();
        	userProfile.setUserId(userId);
        }
        userProfile.setContextPreference(contextPreference);
        return userProfile;
    }

    public static  Map<String, Map<String, Map<String,Long>>> getPreferenceFromConversation(Conversation conversation){
        return derivePreferenceFromConversation(conversation,-1);
    }

    public static Map<String, Map<String, Map<String,Long>>> getShortTermPreferenceFromConversation(Conversation conversation) {
        return derivePreferenceFromConversation(conversation, defaultNumberOfDaysForShortTermPreference);
    }

    public static  Map<String, Map<String, Map<String,Long>>> derivePreferenceFromConversation(Conversation conversation, int latestNumberOfDays){
        List<QA> qaList = conversation.getQaList();
        if(qaList == null || qaList.size() == 0){
            return null;
        }

        List<QA> selectedQAList = null;
        if(latestNumberOfDays <= 0) {
            selectedQAList = qaList;
        }else{
            // select the qas which is within this time period
            selectedQAList = new ArrayList<>();
            for(int i = qaList.size()-1; i > -1; i--){
                QA qa = qaList.get(i);
                if(TimeUnit.MICROSECONDS.toDays(System.currentTimeMillis() - qa.getTime().getTime()) < latestNumberOfDays){
                    selectedQAList.add(qa);
                }else{
                    break;
                }
            }
        }

        Map<String, Map<String, Map<String,Long>>>contextPreference = new LinkedMap();
        // process each qa to get preference
        for(QA qa : selectedQAList){
            List<EntityExtractionUtil.EntityExtractionResult> entities = qa.getEntities();
            Map<String, List<String>> solrProperty = Recommender.mapEntityList2SolrProperty(entities, true);

            // For each qa, we need to get the context and its preference if available
            List<String> contextValues = new ArrayList<>();
            if(solrProperty.get(Constants.CONTEXT_ACCOMPANY) != null) {
                contextValues.addAll(solrProperty.remove(Constants.CONTEXT_ACCOMPANY));
            }
            if(solrProperty.get(Constants.CONTEXT_OCASION) != null){
                contextValues.addAll(solrProperty.remove(Constants.CONTEXT_OCASION));
            }
            if(solrProperty.get(Constants.CONTEXT_REGULAR) != null){
                contextValues.addAll(solrProperty.remove(Constants.CONTEXT_REGULAR));
            }
            if(contextValues.size() == 0){
                contextValues.add(Constants.CONTEXT_GENERAL);
            }

            // add the remaining properties into the
            if(solrProperty.size() != 0){
                Map<String, Map<String,Long>>attributePreference = new LinkedMap();
                solrProperty.forEach((k,v)->{
                    Map<String, Long> entityCount = v.stream()
                            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
                    attributePreference.put(k,entityCount);
                });

                // save the preference to context
                for(String context : contextValues){
                    if(contextPreference.containsKey(context)){
                        contextPreference.put(context,mergeMap(attributePreference, contextPreference.get(context)));
                    }else{
                        contextPreference.put(context,attributePreference);
                    }
                }
            }
        }
        return contextPreference;
    }

    public static Map<String, Map<String,Long>> mergeMap(Map<String, Map<String,Long>> map1, Map<String, Map<String,Long>> map2){
        if(map1 == null || map1.size() == 0){
            return map2;
        }
        if(map2 == null || map2.size() ==0){
            return map1;
        }
        map2.forEach((k,v)->{
            if(map1.get(k) == null){
                map1.put(k,v);
            }else{
                Map<String,Long> m1 = map1.get(k);
                v.forEach((kk, vv) -> m1.merge(kk, vv, (v1, v2) -> v1 + v2));
                map1.put(k, m1);
            }
        });
        return map1;
    }

    public static Map<String,Map<String,List<String>>> updateLikedRestAssociations(UserProfile userProfile, List<EntityExtractionUtil.EntityExtractionResult> entities, List<String> newLikedRestIds) {
        if (entities == null || entities.size() == 0 || newLikedRestIds == null || newLikedRestIds.size() == 0)
            return userProfile == null ? null : userProfile.getLikedRestAssociations();
        Map<String, Map<String, List<String>>> likedRestAssociations = (userProfile == null || userProfile.getLikedRestAssociations() == null)? new HashedMap() : userProfile.getLikedRestAssociations();

        //using solr properties to represent
        Map<String, List<String>> solrProperty = Recommender.mapEntityList2SolrProperty(entities, true);

        // filter out the unwanted properties
        List<String> unwantedProperties = Arrays.asList(Constants.GEO_DISTANCE_TO_FILTER, Constants.RESTAURANT_GEO_FIELD, Constants.RESTAURANT_CS_NAME_FIELD);
        unwantedProperties.forEach(x->{
            if(solrProperty.get(x) != null) {
                solrProperty.remove(x);
            }
        });

        solrProperty.forEach((outerKey,vlist)->{
            Map<String, List<String>> innerMap = likedRestAssociations.containsKey(outerKey) ? likedRestAssociations.get(outerKey) : new HashedMap();
            vlist.forEach(innerKey->{
                if (innerMap.containsKey(innerKey)) {
                    List<String> alist = innerMap.get(innerKey);
                    alist.addAll(newLikedRestIds);
                    innerMap.put(innerKey, alist);
                } else {
                    innerMap.put(innerKey, newLikedRestIds);
                }
            });
            likedRestAssociations.put(outerKey, innerMap);
        });

        // using entity extraction results to represent
//
//        for (EntityExtractionUtil.EntityExtractionResult x : entities) {
//            String outerKey = x.getEntityName();
//            Map<String, List<String>> innerMap = likedRestAssociations.containsKey(outerKey) ? likedRestAssociations.get(outerKey) : new HashedMap();
//            String innerKey = String.join(" ", x.getEntityValue());
//            if (innerMap.containsKey(innerKey)) {
//                List<String> alist = innerMap.get(innerKey);
//                alist.addAll(newLikedRestIds);
//                innerMap.put(innerKey, alist);
//            } else {
//                innerMap.put(innerKey, newLikedRestIds);
//            }
//            likedRestAssociations.put(outerKey, innerMap);
//        }
        return likedRestAssociations;
    }

    public static Map<String, Map<String,Long>> derivePreferenceFromUserLikedRests(UserProfile userProfile){
        if(userProfile == null || userProfile.getLikedRestAssociations() == null || userProfile.getLikedRestAssociations().size() == 0)
            return null;
        Map<String, Map<String, Long>> results = new HashedMap();
        userProfile.getLikedRestAssociations().forEach((k,vmap)->{
            Map<String, Long> tempMap = vmap.entrySet().stream().
                    collect(Collectors.toMap(Map.Entry::getKey,e -> new Long(e.getValue().size())));
            results.put(k, tempMap);
        });
        return results;
    }
}
