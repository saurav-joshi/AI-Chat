package com.aibot.recommender;

import com.aibot.dao.RestaurantDao;
import com.aibot.entity.RecommenderQuery;
import com.aibot.entity.RecommenderResultsRestaurant;
import com.aibot.entityextraction.DistanceExtraction;
import com.aibot.entityextraction.EntityExtractionUtil;
import com.aibot.qa.FlowManagement;
import com.aibot.qa.GeoCalculator;
import com.aibot.qa.GlobalConstants;
import com.aibot.entity.UserProfile;
import com.aibot.entityextraction.LocationFromAddress;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import scala.Tuple2;

import java.util.*;
import java.util.stream.Collectors;

public class Recommender<T> {
    private static Recommender recommender;

    String solrRestaurantULR;
    RestaurantDao restaurantDao;
    String tagFilePath = Constants.TAG_EXTEND_FILE_PATH;
    OfferHandler offerHander = new OfferHandler(Constants.OFFER_FILE_PATH);
    ResultWrapper resultWrapper = new ResultWrapper();

    Map<String,String> attribute2SolrField = new HashMap<>();
    int preferenceFrequencyLimit = 5;
    String rankByDistancePoint;



    public static Recommender getInstance() {
        return recommender == null ? new Recommender() : recommender;
    }

    public Recommender(){
        solrRestaurantULR = GlobalConstants.solrUrl_V2;
        restaurantDao = new RestaurantDao(solrRestaurantULR, tagFilePath);
        attribute2SolrField.put("$dish",Constants.RESTAURANT_CS_DISH_FIELD);
        attribute2SolrField.put("#location",Constants.RESTAURANT_CS_ADDRESS_FIELD);
        attribute2SolrField.put("$location",Constants.RESTAURANT_CS_ADDRESS_FIELD);
        attribute2SolrField.put("#cuisine",Constants.RESTAURANT_CS_CUISINE_FIELD);
        attribute2SolrField.put("$nationality",Constants.RESTAURANT_CS_CUISINE_FIELD);
        attribute2SolrField.put("$restaurantname",Constants.RESTAURANT_CS_NAME_FIELD);
        attribute2SolrField.put("$ingredient",Constants.RESTAURANT_CS_INGREDIENT_FIELD);
        attribute2SolrField.put("$beverage",Constants.RESTAURANT_CS_BEVERAGES_FIELD);
        attribute2SolrField.put("$mealtype",Constants.RESTAURANT_CS_MEAL_FIELD);
        attribute2SolrField.put("$establishmenttype",Constants.RESTAURANT_CS_ESTABLISHMENT_FIELD);
        attribute2SolrField.put("$pricerange", Constants.RESTAURANT_CS_PRICERANGE_FIELD);
        attribute2SolrField.put("$restaurantfeature", Constants.RESTAURANT_CS_FEATURES_FIELD);
        attribute2SolrField.put("$accolade", Constants.RESTAURANT_CS_MSTAR_FIELD);
        attribute2SolrField.put("$event", Constants.RESTAURANT_CS_EVENT_FIELD);
    }

    public static RecommenderQuery createRecommenderQuery(Set<String> questionType, List<EntityExtractionUtil.EntityExtractionResult> entities, Map<String,String> otherParas, int page) {
        System.out.println("Entities to be converted to query : " + entities);
        System.out.println("Query type to use:" + questionType);

        if(questionType == null){
            System.out.println("Empty question types !!!!!!!!!!!! : " + questionType);
        }

        RecommenderQuery rq = new RecommenderQuery();
        Map<String, List<String>> properties = mapEntityList2SolrProperty(entities, false);

        // process other parameters here
        if(otherParas != null && otherParas.size() != 0){
            otherParas.forEach((key,val)->{
                switch (key){
                    case Constants.GEO_DISTANCE_TO_FILTER: {
                        properties.put(Constants.GEO_DISTANCE_TO_FILTER, Arrays.asList(val));
                        rq.setDistanceRefine(true);
                        break;
                    }
                    case Constants.API_PARA_CITY: {
                        // TODO: update the rules if we give derived city higher priority.
                        if(!properties.containsKey(Constants.RESTAURANT_ADDRESSCITY_FIELD)){
                            properties.put(Constants.RESTAURANT_ADDRESSCITY_FIELD, Arrays.asList(val));
                        }
                        break;
                    }
                    case Constants.API_PARA_GEO: {
                        // get the city of that geo
                        String cityOfGeo = null;
                        if(val != null && val.split(",").length == 2){
                            try {
                                cityOfGeo = GeoCalculator.getCityFromLatLongOpenMap(val.split(",")[0], val.split(",")[1]);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        // use the geo only when either no city is given or the geo's city is the same with the given city.
                        if(!properties.containsKey(Constants.RESTAURANT_ADDRESSCITY_FIELD)||(
                                properties.containsKey(Constants.RESTAURANT_ADDRESSCITY_FIELD) && cityOfGeo != null &&
                                properties.get(Constants.RESTAURANT_ADDRESSCITY_FIELD).contains(cityOfGeo.toLowerCase())
                                )){
                            properties.put(Constants.RESTAURANT_GEO_FIELD, Arrays.asList(val));
                        }
                        break;
                    }
                    // TODO: 9/22/16: handle more other parameters here
                    default:{
                        System.out.println("These parameters cannot be handled now");
                    }
                }
            });
        }

        if (questionType.contains("ConsumerQuery.SimilarRestaurant")) {
            System.out.println(">>question types - looking for similar to: " + questionType);
            Map<Constants.RecommenderTarget, Map<String, List<String>>> similarTo = new HashMap<>();
            similarTo.put(Constants.RecommenderTarget.RESTAURANT, properties);
            rq.setTarget(Constants.RecommenderTarget.RESTAURANT);
            rq.setSimilarTo(similarTo);
            rq.setQueryType(RecommenderQuery.QueryType.SimilarTo);
        }else if (questionType.contains("ConsumerQuery.ClassMember")
            || questionType.contains("ConsumerQuery.Refine")
            || questionType.contains("ConsumerQuery.Location")
                || questionType.contains("ConsumerQuery.City")
            || questionType.contains("ConsumerQuery.RestaurantInfo")
            || questionType.contains("ConsumerQuery.Offers")
            || questionType.contains("ConsumerQuery.Accolades")
                || questionType.contains("UserRefine.Attribute")) {
            System.out.println(">>question types - looking for a class member: " + questionType);
            rq.setTarget(Constants.RecommenderTarget.RESTAURANT);
            rq.setProperties(properties);
            rq.setQueryType(RecommenderQuery.QueryType.LookingForClassMember);
        }else if (questionType.contains("ConsumerQuery.SurpriseMe")) {
            System.out.println(">>question type - supprise me: " + questionType);
            rq.setTarget(Constants.RecommenderTarget.RESTAURANT);
            rq.setProperties(properties);
            rq.setQueryType(RecommenderQuery.QueryType.GeneralQuery);
        }else {
            System.out.println(">>Other question types !!!!!!!!!!!! : " + questionType);
            rq.setQueryType(RecommenderQuery.QueryType.Others);
        }
        rq.setPages(page);
        return rq;
    }

    public static Map<String, List<String>> mapEntityList2SolrProperty(List<EntityExtractionUtil.EntityExtractionResult> entities, boolean isProfileing){
        Map<String, List<String>> properties = new HashMap<>();
        if (entities != null) {
        	for (EntityExtractionUtil.EntityExtractionResult en : entities) {
                String value = String.join(" ", en.getEntityValue());
                if(value.trim().length() == 0){
                    continue;
                }
                switch (en.getEntityName()) {
                    case "$dish": {
                        properties.computeIfAbsent(Constants.RESTAURANT_CS_DISH_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "#location": {
//                        value = FlowManagement.contextToEntityMapping.getOrDefault(value,value);
                        if(isProfileing) value = value.replaceAll("\\b(in|on|near|within|along|close to|at|around|next to|across|to)\\b", "").trim();
                        properties.computeIfAbsent(Constants.RESTAURANT_GENERAL_LOCATION_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$location": {
                        properties.computeIfAbsent(Constants.RESTAURANT_GENERAL_LOCATION_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "#cuisine": {
                        value = FlowManagement.contextToEntityMapping.getOrDefault(value,value);
                        properties.computeIfAbsent(Constants.RESTAURANT_CS_CUISINE_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$regional": {
                        value = FlowManagement.contextToEntityMapping.getOrDefault(value,value);
                        properties.computeIfAbsent(Constants.RESTAURANT_CS_CUISINE_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$nationality": {
                        value = FlowManagement.contextToEntityMapping.getOrDefault(value,value);
                        properties.computeIfAbsent(Constants.RESTAURANT_CS_CUISINE_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$country": {
                        value = FlowManagement.contextToEntityMapping.getOrDefault(value,value);
                        properties.computeIfAbsent(Constants.RESTAURANT_CS_CUISINE_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$restaurantname": {
                        value = value.replaceAll("\\s+'s", "'s");
                        properties.computeIfAbsent(Constants.RESTAURANT_CS_NAME_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$ingredient": {
                        properties.computeIfAbsent(Constants.RESTAURANT_CS_INGREDIENT_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$beverage": {
                        properties.computeIfAbsent(Constants.RESTAURANT_CS_BEVERAGES_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$mealtype": {
                        properties.computeIfAbsent(Constants.RESTAURANT_CS_MEAL_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$establishmenttype": {
                        properties.computeIfAbsent(Constants.RESTAURANT_CS_ESTABLISHMENT_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$pricerange": {
                        value = value.replace(" price","");
                        properties.computeIfAbsent(Constants.RESTAURANT_CS_PRICERANGE_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$religious": {
                        properties.computeIfAbsent(Constants.RESTAURANT_RELIGIOUS_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$geo": {
                        properties.computeIfAbsent(Constants.RESTAURANT_GEO_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$restaurantfeature": {
                        properties.computeIfAbsent(Constants.RESTAURANT_CS_FEATURES_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$accolade": {
                        properties.computeIfAbsent(Constants.RESTAURANT_CS_MSTAR_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$distance": {
                        value = DistanceExtraction.getInstance().getFormattedDistanceFromText(value);
                        if(value != null){
                            properties.computeIfAbsent(Constants.GEO_DISTANCE_TO_FILTER, v -> new ArrayList<>()).add(value);
                        }
                        break;
                    }
                    case "$refinelocation": {
                        if(value != null && value.equalsIgnoreCase("within walking distance")){
                            properties.computeIfAbsent(Constants.GEO_DISTANCE_TO_FILTER, v -> new ArrayList<>()).add("0.5");
                        }
                        break;
                    }
                    case "$event": {
                        if(value != null){
                            properties.computeIfAbsent(Constants.RESTAURANT_CS_EVENT_FIELD, v -> new ArrayList<>()).add(value);
                        }
                        break;
                    }
                    case "$offer": {
                        properties.computeIfAbsent(Constants.RESTAURANT_OFFER_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$accompany": {
                        properties.computeIfAbsent(Constants.CONTEXT_ACCOMPANY, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$regular": {
                        properties.computeIfAbsent(Constants.CONTEXT_REGULAR, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$occasion": {
                        properties.computeIfAbsent(Constants.CONTEXT_OCASION, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    case "$city": {
                        properties.computeIfAbsent(Constants.RESTAURANT_ADDRESSCITY_FIELD, v -> new ArrayList<>()).add(value);
                        break;
                    }
                    // todo: add more dimensions here if needed
                }
            }
        }
        return properties;
    }

    public List<T> getRecommendationResults(RecommenderQuery recommenderQuery, UserProfile userProfile){
        SolrDocumentList results = calculateRecommendations(recommenderQuery, userProfile);
        // only display restaurant names, or review text only
        return resultWrapper.getResults(recommenderQuery, results, offerHander);
    }

    public SolrDocumentList calculateRecommendations(RecommenderQuery recommenderQuery, UserProfile userProfile){
        // Clear off the geo parameters
        rankByDistancePoint = null;

        // return directly if no query or user profile is given
        if((recommenderQuery == null||recommenderQuery.isEmpty()) && (userProfile == null||!userProfile.hasContextPreference())){
            System.out.println("Error: sorry, no recommender query or user profile is given.");
            return null;
        }

        // as offer info exist in a separate file, take the offer field out of the property or similar to if available.
        boolean isOfferAsked = false;
        if(recommenderQuery != null){
            if(recommenderQuery.getProperties() != null && recommenderQuery.getProperties().size() != 0){
                if(recommenderQuery.getProperties().get(Constants.RESTAURANT_OFFER_FIELD) != null){
                    recommenderQuery.getProperties().remove(Constants.RESTAURANT_OFFER_FIELD);
                    isOfferAsked = true;
                }
            }
            Map<Constants.RecommenderTarget, Map<String, List<String>>> similarTos = recommenderQuery.getSimilarTo();
            if(similarTos != null && similarTos.size()!=0 && similarTos.get(Constants.RecommenderTarget.RESTAURANT) != null && similarTos.get(Constants.RecommenderTarget.RESTAURANT).get(Constants.RESTAURANT_OFFER_FIELD) != null){
                similarTos.get(Constants.RecommenderTarget.RESTAURANT).remove(Constants.RESTAURANT_OFFER_FIELD);
                isOfferAsked = true;
            }
        }

        // construct solrQuery for different cases
        SolrQuery solrQuery = null;
        if((recommenderQuery == null || recommenderQuery.isEmpty())){
            System.out.println("================\nRecommend by userprofile");
            solrQuery = recommendByUserProfile(userProfile);
        }else if (userProfile == null || !userProfile.hasContextPreference()){
            System.out.println("================\nRecommend by query only");
            solrQuery = parseQuery(recommenderQuery, null);
        }else{
            System.out.println("================\nRecommend by both query and profile");
            solrQuery = parseQuery(recommenderQuery, userProfile);
        }

        // get the recommendation results based on the constructed solrQuery
        SolrDocumentList results = null;
        if(isOfferAsked){ // if offer is asked, modify the solrQuery first
            if(solrQuery == null){
                solrQuery = new SolrQuery("*:*");
                solrQuery.setRows(recommenderQuery.getPages()*recommenderQuery.getNumOfResult());
            }
            restaurantDao.addFilterQuery(solrQuery,Constants.RESTAURANT_ID_FIELD, new ArrayList(offerHander.getRestaurantsWithValidOffers()),true);
        }
        try {
            results = restaurantDao.searchDoc(solrQuery);
        }catch (Exception e){
            System.out.println("Exception while searching for solr.");
            e.printStackTrace();
        }

        // return directly if no results are found
        if(results == null || results.size() == 0){
            return null;
        }

        // rerank with user liked rest
        results = rerankResultsWithUserprofile(results, userProfile);

        // control the page of results to be returned if available
//        if(recommenderQuery.getPages() <= 1)
//            return results;
        SolrDocumentList pageResults = new SolrDocumentList();
        int cnt = (recommenderQuery.getPages() - 1) * recommenderQuery.getNumOfResult();
        while(cnt < Math.min(results.size(), recommenderQuery.getNumOfResult()*recommenderQuery.getPages())){
            pageResults.add(results.get(cnt));
            cnt ++;
        }
        return pageResults;
    }

    public List<RecommenderResultsRestaurant> getRestaurantsInfoByIds(List<String> restIds){
        if(restIds == null || restIds.size() == 0)
            return null;
        List<RecommenderResultsRestaurant> results = new ArrayList<>();
        restIds.forEach(x->{
            RecommenderResultsRestaurant arest = getRestaurantInfoById(x);
            if(arest != null){
                results.add(arest);
            }
        });
        return results.size() == 0 ? null : results;
    }

    public RecommenderResultsRestaurant getRestaurantInfoById(String restId){
        if(restId == null || restId.trim().length() == 0)
            return null;
        SolrQuery solrQuery = new SolrQuery(Constants.RESTAURANT_ID_FIELD + ":" + restId);
        SolrDocumentList results = null;
        try{
            results = restaurantDao.searchDoc(solrQuery);
        }catch (Exception e){
            e.printStackTrace();
        }
        if(results == null || results.size() < 1){
            System.out.println("Invalid restaurant id - " + restId + ". No restaurant found.");
            return null;
        }
        return resultWrapper.getSingleRestaurantInfo(results.get(0), offerHander);
    }

    public SolrQuery recommendByUserProfile(UserProfile userProfile){
        Map<String, Map<String,Map<String, Long>>> contextPreference = userProfile.getContextPreference();
        String solrString = null;
        Map<String, Map<String, Long>> attributePreference = null;
        if(contextPreference.get(Constants.CONTEXT_GENERAL) != null){
            attributePreference = contextPreference.get(Constants.CONTEXT_GENERAL);
        }else{
            attributePreference = new HashMap<>();
            // combine preference under different context as the general preference
            for(Map.Entry<String, Map<String,Map<String, Long>>> entry : contextPreference.entrySet()){
                attributePreference = UserProfiling.mergeMap(attributePreference,entry.getValue());
            }
        }
        solrString = convertPreference2SolrString(attributePreference);
        SolrQuery solrQuery = new SolrQuery(solrString);
        solrQuery.setRows(50);
        return solrQuery;
    }

    public String convertPreference2SolrString(Map<String, Map<String, Long>> attributePreference){
        StringJoiner outsj = new StringJoiner(" " + Constants.DELIMITER_OR + " ","","");

        attributePreference.forEach((attr, val)->{
            String solrField = null;
            if(attribute2SolrField.get(attr) != null){
                solrField = attribute2SolrField.get(attr);

                StringJoiner solrValSj = new StringJoiner(" OR ", "", "");
                for(Map.Entry<String,Long> entry:val.entrySet()){
                    solrValSj.add("\""+entry.getKey()+"\"^"+entry.getValue());
                }
                if(solrValSj.toString().length() != 0){
                    String solrString = solrField+":("+solrValSj.toString()+")";
                    outsj.add(solrString);
                }
            }
        });
        return outsj.toString();
    }

    public SolrQuery parseQuery(RecommenderQuery recommenderQuery, UserProfile userProfile){
        Map<String, List<String>> properties = recommenderQuery.getProperties();
        Map<Constants.RecommenderTarget, Map<String, List<String>>> similarTos = recommenderQuery.getSimilarTo();
        List<String> fields = recommenderQuery.getFields();
        List<Constants.RankCriteria> rankCriteria = recommenderQuery.getRankCriteria();

        Set<String> reviewFields = restaurantDao.getReviewMetaFields();
        Set<String> restFields = restaurantDao.getRestFields();
        SolrQuery solrQuery = null;

        // parse the properties
        Map<String, List<String>> restProperties = new HashMap<>();
        Map<String, List<String>> reviewProperties = new HashMap<>();
        List<String> contextProperties = new ArrayList<>();
        // dispatch the properties to: restaurant, review, or context-related if available
        if(properties != null && properties.size() != 0) {
            for(Map.Entry<String,List<String>> entry : properties.entrySet()){
                String propertyKey = entry.getKey();
                List<String> propertyValues = entry.getValue();
                if(propertyKey == Constants.RESTAURANT_ID_FIELD){
                    if(recommenderQuery.getTarget() == Constants.RecommenderTarget.RESTAURANT){
                        restProperties.put(propertyKey, propertyValues);
                    }else if(recommenderQuery.getTarget() == Constants.RecommenderTarget.REVIEW){
                        reviewProperties.put(propertyKey, propertyValues);
                    }
                }
                if(restFields.contains(propertyKey)){
                    restProperties.put(propertyKey, propertyValues);
                }
                if(reviewFields.contains(propertyKey)){
                    reviewProperties.put(propertyKey, propertyValues);
                }
                if(propertyKey.equalsIgnoreCase(Constants.GEO_DISTANCE_TO_FILTER)){
                    restProperties.put(propertyKey, propertyValues);
                }
                if(propertyKey.equalsIgnoreCase(Constants.CONTEXT_ACCOMPANY)
                        || propertyKey.equalsIgnoreCase(Constants.CONTEXT_OCASION)
                        || propertyKey.equalsIgnoreCase(Constants.CONTEXT_REGULAR)){
                    contextProperties.addAll(propertyValues);
                }
            }
        }


        // refine properities using userprofile under the particular context
        refineRestPropertyWithUserProfile(restProperties, userProfile, contextProperties);

        // process the properties for different recommender targets
        if(recommenderQuery.getTarget() == Constants.RecommenderTarget.RESTAURANT){
            if(recommenderQuery.getQueryType().equals(RecommenderQuery.QueryType.GeneralQuery)){
                solrQuery = processGeneralQueries(restProperties, userProfile);
            }else if(recommenderQuery.getQueryType().equals(RecommenderQuery.QueryType.LookingForClassMember)){
                solrQuery = parseRestProperty(restProperties);
            }
        }else if(recommenderQuery.getTarget() == Constants.RecommenderTarget.REVIEW){
            if(restProperties.size() == 0 && reviewProperties.size() == 0){
                System.out.println("Case 2: no review properties are given");
            }else if(restProperties.size() != 0){
                System.out.println("==process getting restaurants for filtering review");
                // process restaurant properties to get restaurant info.
                SolrQuery restSolrQuery = parseRestProperty(restProperties);
                if(restSolrQuery != null){
                    SolrDocumentList rests = restaurantDao.searchDoc(restSolrQuery);

                    List<String> restIds = null;
                    if(reviewProperties.get(Constants.REVIEW_RESTAURANTID_FIELD) != null){
                        restIds = reviewProperties.get(Constants.REVIEW_RESTAURANTID_FIELD);
                    }else{
                        restIds = new ArrayList<>();
                    }

                    for(SolrDocument arest : rests){
                        restIds.add((String)arest.getFieldValue(Constants.RESTAURANT_ID_FIELD));
                    }
                    reviewProperties.put(Constants.REVIEW_RESTAURANTID_FIELD, restIds);
                }
                System.out.println("restaurant used to filter:" + reviewProperties.get(Constants.REVIEW_RESTAURANTID_FIELD).size());

                // process review normally
                solrQuery = parseReviewProperty(reviewProperties);
            }else{
                System.out.println("== process getting review by review properties");
                // process review properties normally
                solrQuery = parseReviewProperty(reviewProperties);
            }
        }else{
            System.out.println("Sorry, this recommender target is not supported by now.");
        }

        // parse similarito
        if(similarTos != null && similarTos.size() != 0) {
            if(recommenderQuery.getTarget() == Constants.RecommenderTarget.RESTAURANT){
                Map<String, List<String>> restSimilarTo = similarTos.get(Constants.RecommenderTarget.RESTAURANT);
                solrQuery = parseRestSimilarTo(restSimilarTo);
            }else if(recommenderQuery.getTarget() == Constants.RecommenderTarget.REVIEW){
                Map<String, List<String>> reviewSimilarTo = similarTos.get(Constants.RecommenderTarget.REVIEW);
                solrQuery = parseReviewSimilarTo(reviewSimilarTo);
            }
        }

        // parse rank criteria, fields, and no of results
        if(solrQuery != null) {
            // parse and configure the rank criteria
            if(rankCriteria != null  && rankCriteria.size() != 0){
                for(int i = 0; i< rankCriteria.size(); i++){
                    Constants.RankCriteria field = rankCriteria.get(i);
                    String rankField = Constants.SOLR_SCORE;
                    SolrQuery.ORDER order = SolrQuery.ORDER.desc;

                    if (field == Constants.RankCriteria.SENTIMENT) {
                        rankField = Constants.REVIEW_SENTIMENT_FEILD;
                        order = SolrQuery.ORDER.desc;
                    }
                    if (field == Constants.RankCriteria.POPULARITY) {
//                        rankField = Constants.RESTAURANT_REVIEW_NO_FIELD;
                        rankField = Constants.RESTAURANT_OVERALL_RATING_FIELD;
                        order = SolrQuery.ORDER.desc;
                    }
                    solrQuery.addOrUpdateSort(rankField,order);
                }
            }else{
                solrQuery.addOrUpdateSort(Constants.SOLR_SCORE,SolrQuery.ORDER.desc);
            }

            // configure to rank by distance as a second rank criteria
            if(rankByDistancePoint != null && rankByDistancePoint.length() != 0){
                System.out.println("Rank by distance to point:" + rankByDistancePoint);
                solrQuery.set("sfield", Constants.RESTAURANT_GEO_FIELD);
                solrQuery.set("pt", rankByDistancePoint);
                if(!recommenderQuery.isDistanceRefine()) solrQuery.addOrUpdateSort("geodist()", SolrQuery.ORDER.asc);
            }

            // parse and configure the fields
            if(fields != null && fields.size() != 0){
                fields.forEach(solrQuery::addField);
            } else {
                solrQuery.addField(Constants.SOLR_ALL_FIELDS);// by default include all the fields
            }
            solrQuery.addField(Constants.SOLR_SCORE);
            if(rankByDistancePoint != null && rankByDistancePoint.length() != 0){
                solrQuery.addField(Constants.SOLR_DISTANCE + ":geodist()");
            }

            // parse and configure the idsToFilterOut fields if necessary:
            Set<String> ids2filterOut = getRestIdsToFilterOut(recommenderQuery,userProfile);
            solrQuery = parseIdsToFilterOut(solrQuery, recommenderQuery.getTarget(), ids2filterOut);

            // parse and set number of results to return
            solrQuery = parseNumberOfResults(solrQuery,Constants.DEFAULT_PAGE_NO_TO_FETCH,recommenderQuery.getNumOfResult());
        }
        return solrQuery;
    }

    public Set<String> getRestIdsToFilterOut(RecommenderQuery recommenderQuery, UserProfile userProfile){
        if(recommenderQuery == null)
            return null;
        Map<String, List<String>> properties = recommenderQuery.getProperties();
        Map<Constants.RecommenderTarget, Map<String, List<String>>> similarTos = recommenderQuery.getSimilarTo();
        // get the ids that indicated in the query
        Set<String> ids2filterOut = recommenderQuery.getIdsToFilterOut();

        if(recommenderQuery.getTarget() == Constants.RecommenderTarget.RESTAURANT){
            // filter out user's disliked restaurants if available in their user profile
            if(userProfile != null && userProfile.getDislikedRests() != null && userProfile.getDislikedRests().size() != 0){
                Set<String> dislikedRestIds = new HashSet<>(userProfile.getDislikedRests().stream().filter(x->x.trim().length()!=0).collect(Collectors.toList()));
                if(dislikedRestIds.size() != 0){
                    if(ids2filterOut == null){
                        ids2filterOut = new HashSet<>();
                    }
                    ids2filterOut.addAll(dislikedRestIds);
                }
            }
            // double check whether the users are searching for this restaurants or not, if yes, dont filter it out even he disliked it
            if(ids2filterOut == null || ids2filterOut.size() == 0)
                return ids2filterOut;
            Set<String> restNamesInQuery = new HashSet<>();
            if(properties != null && properties.size() != 0){
                List<String> tmp = properties.get(Constants.RESTAURANT_CS_NAME_FIELD);
                if(tmp != null && tmp.size() != 0){
                    restNamesInQuery.addAll(tmp.stream().filter(x->x.trim().length()!=0).collect(Collectors.toSet()));
                }
            }
            if(similarTos != null && similarTos.size() != 0){
                Map<String, List<String>> restSimilarTo = similarTos.get(Constants.RecommenderTarget.RESTAURANT);
                List<String> tmp = restSimilarTo.get(Constants.RESTAURANT_CS_NAME_FIELD);
                if(tmp != null && tmp.size() != 0){
                    restNamesInQuery.addAll(tmp.stream().filter(x->x.trim().length()!=0).collect(Collectors.toSet()));
                }
            }
            if(restNamesInQuery.size() != 0){
                // remove it from rests2FilterOut
                Set<String> idsToKeep = new HashSet<>();
                restNamesInQuery.stream().forEach(x->{
                    SolrQuery sq = restaurantDao.getRestuarantByName(x, true, 10);
                    if(sq != null){
                        try{
                            SolrDocumentList ids = restaurantDao.searchDoc(sq);
                            if(ids != null){
                                idsToKeep.addAll(ids.stream().map(e->String.valueOf(e.getFieldValue(Constants.RESTAURANT_ID_FIELD))).collect(Collectors.toSet()));
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                });
                if(idsToKeep.size() != 0){
                    ids2filterOut.removeAll(idsToKeep);
                }
            }
        }
        return ids2filterOut;
    }

    public SolrQuery parseIdsToFilterOut(SolrQuery solrQuery, Constants.RecommenderTarget target, Set<String> idsToFilterOut){
        if(solrQuery != null){
            if(idsToFilterOut != null && idsToFilterOut.size() != 0){
                if(target == Constants.RecommenderTarget.REVIEW){
                    restaurantDao.addFilterQuery(solrQuery, Constants.REVIEW_ID_FIELD, new ArrayList<>(idsToFilterOut),false);
                }else{
                    restaurantDao.addFilterQuery(solrQuery, Constants.RESTAURANT_ID_FIELD, new ArrayList<>(idsToFilterOut),false);
                }
            }
        }
        return solrQuery;
    }

    public SolrQuery parseNumberOfResults(SolrQuery solrQuery, int pages, int numOfResults){
        if(solrQuery != null){
            if(pages <= 0){
                pages = 1;
            }
            if(numOfResults <= 0){
                numOfResults = Constants.DEFAULT_PAGE_SIZE;
            }
            solrQuery.setRows(numOfResults*pages);
        }
        return solrQuery;
    }

    public SolrQuery parseRestProperty(Map<String, List<String>> properties){
        if(properties == null || properties.size() == 0)
            return null;

        SolrQuery solrQuery = null;

        // get the geo/location info out from the property, and convert it to a solr query string
        String fqString = convertGeoLocation2SolrString(properties);

        // process the other entities and convert it to a solr query string
        String propertyString = convertRestProperty2SolrString(properties);

        // use the property string as the main query
        if(propertyString.toString().length() != 0){
            System.out.println("The query string is:" + propertyString.toString());
            solrQuery = new SolrQuery(propertyString.toString());
        }

        // add geolocation info as filter if available
        if(fqString != null && fqString.length() != 0){
            if(solrQuery == null){
                solrQuery = new SolrQuery("*:*");
            }
            solrQuery.addFilterQuery(fqString);
        }
        return solrQuery;
    }

    public SolrQuery parseReviewProperty(Map<String, List<String>> properties){
        SolrQuery solrQuery = null;
        StringJoiner qsj = new StringJoiner(" " + Constants.DELIMITER_AND + " ", "", "");

        for(Map.Entry<String, List<String>> entry : properties.entrySet()) {
            String property = entry.getKey();
            List<String> values = entry.getValue();

            if (property.equalsIgnoreCase(Constants.REVIEW_ID_FIELD) || property.equalsIgnoreCase(Constants.REVIEW_RESTAURANTID_FIELD)) { // metadata fields
                if(values.size() != 0){
                    if(solrQuery == null){
                        solrQuery = new SolrQuery();
                    }
                    restaurantDao.addFilterQuery(solrQuery, property, values, true);
                }
            } else {// other fields
                String tagString = restaurantDao.constructQueryStringFromTags(values, Constants.DELIMITER_OR, true);
                if(tagString != null && tagString.length() != 0) {
                    qsj.add(property + ":(" + tagString + ")");
                }
            }
        }

        if(qsj.toString().length() != 0){
            if(solrQuery != null){
                solrQuery.setQuery(qsj.toString());
            }else{
                solrQuery = new SolrQuery(qsj.toString());
            }
        }else{
            if(solrQuery != null){
                solrQuery.setQuery(Constants.SOLR_ALL_FIELDS);
            }
        }

        return solrQuery;
    }

    public SolrQuery parseRestSimilarTo(Map<String, List<String>> simRest){
        SolrQuery solrQuery = null;
        String queryString = null;
        System.out.println("====similar to properties");
        printProperties(simRest);
        // Get the restid or name field out as the similar target
        String fieldName = null;
        List<String> fieldValues = null;
        SolrQuery restQuery = null;
        if(simRest.get(Constants.RESTAURANT_ID_FIELD) != null){
            fieldValues = simRest.remove(Constants.RESTAURANT_ID_FIELD);
            fieldName = Constants.RESTAURANT_ID_FIELD;
        }else if(simRest.get(Constants.RESTAURANT_CS_NAME_FIELD) != null){
            fieldValues = simRest.remove(Constants.RESTAURANT_CS_NAME_FIELD);
            fieldName = Constants.RESTAURANT_CS_NAME_FIELD;
        }

        // get the restaurant names to filter out later
        Set<String> restNamesToFilter = new HashSet<>();
        if(fieldValues != null){
            if(fieldName == Constants.RESTAURANT_CS_NAME_FIELD){
                restNamesToFilter.addAll(fieldValues);
            }else{
                restQuery = restaurantDao.getRestuarantByIdList(fieldValues);
                SolrDocumentList rests = restaurantDao.searchDoc(restQuery);
                for(SolrDocument arest : rests){
                    restNamesToFilter.add(String.valueOf(arest.getFieldValue(Constants.RESTAURANT_CS_NAME_FIELD)));
                }
            }
        }

        // get the other fields as similar aspect to compare
        // get the location out first, geo and location field got removed as well in this function
        String fqString = convertGeoLocation2SolrString(simRest);

        if(fieldValues != null){
            if(simRest.keySet().size() == 0){
                queryString = restaurantDao.constructQueryStringFromRestaurants(fieldValues,fieldName);
            }else{
                List<String> aspectList = new ArrayList<>();
                for(String field : simRest.keySet()){
                    if(field.equalsIgnoreCase("addresscity")){//only consider aspect of the restaurant, not city
                        continue;
                    }
                    if(field.equalsIgnoreCase(Constants.RESTAURANT_GENERAL_CUISINE_FIELD)){
                        aspectList.add(Constants.RESTAURANT_CS_CUISINE_FIELD);
                    }else if(field.equalsIgnoreCase(Constants.RESTAURANT_GENERAL_ALL_FIELD)){
                        aspectList.add(Constants.RESTAURANT_DESCRIPTION_FIELD);
                    }else{
                        aspectList.add(field);
                    }
                }
                queryString = restaurantDao.constructQueryStringFromRestaurants(fieldValues, fieldName, aspectList);
            }
            System.out.println("query string by similar to:" + queryString);
        }else{
            System.out.println("Sorry, no valid restaurant is given in similarto");
        }

        if(queryString != null){
            solrQuery = new SolrQuery(queryString);
        }
        if(restNamesToFilter != null && restNamesToFilter.size() != 0){
            if(solrQuery == null){
                solrQuery = new SolrQuery("*:*");
            }
            restaurantDao.addFilterQuery(solrQuery, Constants.RESTAURANT_CS_NAME_FIELD, new ArrayList<String>(restNamesToFilter), false);
        }
        if(fqString != null && fqString.length() != 0){
            if(solrQuery == null){
                solrQuery = new SolrQuery("*:*");
            }
            solrQuery.addFilterQuery(fqString);
        }
        return solrQuery;
    }

    public SolrQuery parseReviewSimilarTo(Map<String, List<String>> reviewSimilarTo){
        SolrQuery solrQuery = null;
        String fieldName = null;
        List<String> fieldValues = null;
        SolrQuery restQuery = null;

        if(reviewSimilarTo.get(Constants.REVIEW_RESTAURANTID_FIELD) != null){
            fieldValues = reviewSimilarTo.remove(Constants.REVIEW_RESTAURANTID_FIELD);
            fieldName = Constants.RESTAURANT_ID_FIELD;
        }else if(reviewSimilarTo.get(Constants.RESTAURANT_CS_NAME_FIELD) != null){
            fieldValues = reviewSimilarTo.remove(Constants.RESTAURANT_CS_NAME_FIELD);
            fieldName = Constants.RESTAURANT_CS_NAME_FIELD;
        }

        // get the restaurant names to filter out later
        Set<String> restNamesToFilter = new HashSet<>();
        if(fieldValues != null){
            if(fieldName == Constants.RESTAURANT_CS_NAME_FIELD){
                restNamesToFilter.addAll(fieldValues);
            }else{
                restQuery = restaurantDao.getRestuarantByIdList(fieldValues);
                SolrDocumentList rests = restaurantDao.searchDoc(restQuery);
                for(SolrDocument arest : rests){
                    restNamesToFilter.add(String.valueOf(arest.getFieldValue(Constants.RESTAURANT_CS_NAME_FIELD)));
                }
            }
        }

        if(reviewSimilarTo.size() != 0){
            String queryString = null;
            if(reviewSimilarTo.get(Constants.REVIEW_TEXT_FEILD) != null){
                queryString = restaurantDao.constructQueryStringFromTags(reviewSimilarTo.get(Constants.REVIEW_TEXT_FEILD),Constants.DELIMITER_OR,true);
            }else if(reviewSimilarTo.get(Constants.REVIEW_ID_FIELD) != null){
                queryString = restaurantDao.constructQueryFromReviewIDs(reviewSimilarTo.get(Constants.REVIEW_ID_FIELD));
            }
            if(queryString != null && queryString.length() != 0){
                if(solrQuery == null){
                    solrQuery = new SolrQuery();
                }
                System.out.println("=======\n" + queryString);
                solrQuery.setQuery(Constants.REVIEW_TEXT_FEILD + ":(" + queryString + ")");
            }
        }

        if(restNamesToFilter != null && restNamesToFilter.size() != 0){
            if(solrQuery == null){
                solrQuery = new SolrQuery(Constants.SOLR_ALL_FIELDS);
            }
            restaurantDao.addFilterQuery(solrQuery, Constants.REVIEW_RESTAURANTID_FIELD, new ArrayList<String>(restNamesToFilter), true);
        }else{
            if(solrQuery == null){
                System.out.println("Sorry, no valid information can be extracted from similar to.");
            }
        }

        return solrQuery;
    }
    public String convertGeoLocation2SolrString(Map<String, List<String>> properties){
        String fqString = "";
        if(properties == null || properties.size() == 0){
            return fqString;
        }
        float distanceInKmToFilter = Constants.DEFAULT_DISTANCE_TO_FILTER;
        Set<String> refinedLocations = null;
        Set<Tuple2<String, String>> latLons = null;

        if(properties.get(Constants.GEO_DISTANCE_TO_FILTER) != null && properties.get(Constants.GEO_DISTANCE_TO_FILTER).size() != 0){
            String distance = properties.remove(Constants.GEO_DISTANCE_TO_FILTER).get(0);
            System.out.println("====distance is given by refine location");
            System.out.println(distance);
            if(distance != null && distance.trim().length() != 0 && NumberUtils.isNumber(distance)){
                distanceInKmToFilter = Float.valueOf(distance);
                System.out.println(distanceInKmToFilter);
            }else{
                System.out.println("WRONG BRANCH HERE" + distance);
            }
        }

        if(properties.get(Constants.RESTAURANT_GENERAL_LOCATION_FIELD) != null && properties.get(Constants.RESTAURANT_GENERAL_LOCATION_FIELD).size() !=0 ){
            List<String> locations = properties.remove(Constants.RESTAURANT_GENERAL_LOCATION_FIELD);
            refinedLocations = new HashSet<>();
            // preprocess the locations
            Set<String> extendables = Sets.newHashSet("near ", "next to ", "close to ", "around ");

            for (String location : locations) {
                String locationWithoutPres = FlowManagement.contextToEntityMapping.getOrDefault(location,location);
                refinedLocations.add(locationWithoutPres);

                // get geos from locations
                Boolean hasPre = false;
                for (String extendable : extendables) {
                    if(location.trim().startsWith(extendable)){
                        hasPre = true;
                    }
                }
                String city = cityFromProperties(properties);
                if(hasPre || !LocationFromAddress.isBuildingOrStreet(locationWithoutPres, city)){
                    Tuple2<Double,Double> latlontp =  LocationFromAddress.getLatLong(locationWithoutPres, city);
                    if(latlontp != null){
                        Tuple2<String, String> tuple2 = new Tuple2<>(String.valueOf(latlontp._1), String.valueOf(latlontp._2));
                        if(latLons == null){
                            latLons = new HashSet<>();
                        }
                        latLons.add(tuple2);
                    }
                }
            }
        }

        if(properties.get(Constants.RESTAURANT_GEO_FIELD) != null && properties.get(Constants.RESTAURANT_GEO_FIELD).size() != 0 ){
            String latlong = properties.remove(Constants.RESTAURANT_GEO_FIELD).get(0);
            if(latlong.trim().length() != 0 && latlong.split(",").length == 2){
                Tuple2<String,String> tuple2 = new Tuple2<>(latlong.split(",")[0],latlong.split(",")[1]);
                // add the api-geo to the latLons only when no location is explicitly given in the query
                if(refinedLocations == null || refinedLocations.size() == 0){
                    if(latLons == null){
                        latLons = new HashSet<>();
                        latLons.add(tuple2);
                    }
                }
            }
        }

        // loop the latLons to create multiple filter by geo distance
        if(latLons != null){
            StringJoiner geosj = new StringJoiner(" OR ", "", "");
            for(Tuple2<String, String> tp : latLons){
                StringJoiner sj = new StringJoiner(" ", "{!geofilt ", "}");
                sj.add("pt=" + tp._1 + "," + tp._2);
                sj.add("sfield=" + Constants.RESTAURANT_GEO_FIELD);
                sj.add("d=" + String.valueOf(distanceInKmToFilter));
                geosj.add(sj.toString());
                // use one of the location geo for distance ranking in case there is no api-geo is given
                if(rankByDistancePoint == null){
                    rankByDistancePoint = tp._1 + "," + tp._2;
                }
            }
            fqString = geosj.toString();
        }

        if(refinedLocations != null && refinedLocations.size() != 0){
            StringJoiner sj2 = new StringJoiner(" OR ", "", "");
            refinedLocations.forEach(loc->{
                sj2.add(Constants.RESTAURANT_CS_ADDRESS_FIELD + ":\"" + loc + "\"");
            });

            String fqLocString = sj2.toString();

            if(fqString.trim().length() == 0){
                fqString = fqLocString;
            }else{
                if(distanceInKmToFilter >= Constants.DEFAULT_DISTANCE_TO_FILTER){
                    fqString = fqLocString + " OR " + fqString;
                }
            }
        }

        System.out.println("*******************************");
        System.out.println("Filter query string is:" + fqString);
        return fqString;
    }

    private String convertRestProperty2SolrString(Map<String, List<String>> properties){
        Map<String,String> restMetaFields = restaurantDao.getRestMetaFields().stream().collect(Collectors.toMap(i -> i, i -> i));
        StringJoiner qsj = new StringJoiner(" " + Constants.DELIMITER_AND + " ", "", "");
        for(Map.Entry<String, List<String>> entry : properties.entrySet()) {
            String property = entry.getKey();
            List<String> values = entry.getValue();

            String tagString = "";
            String exactNameMatchString = null;
            String exactCityMatchString = null;
            if (restMetaFields.get(property) != null) { // metadata fields
                if(property.equalsIgnoreCase(Constants.RESTAURANT_CS_NAME_FIELD)){
                    String[] tmp = restaurantDao.preprocessNameValues(values,Constants.DELIMITER_OR);
                    if(tmp != null && tmp.length == 2){
                        tagString = tmp[0];
                        exactNameMatchString = tmp[1];
                    }
                }else if(property.equalsIgnoreCase(Constants.RESTAURANT_ADDRESSCITY_FIELD)){
                    String [] tmp = restaurantDao.preprocessNameValues(values, Constants.DELIMITER_OR);
                    if(tmp != null && tmp.length == 2){
                        exactCityMatchString = tmp[1];
                    }
                }else{
                    tagString = restaurantDao.constructQueryStringFromTags(values, Constants.DELIMITER_OR, false);
                }
            } else {// other fields
                tagString = restaurantDao.constructQueryStringFromTags(values, Constants.DELIMITER_OR, true);
            }
            List<String> columnList = new ArrayList<>();
            if (property.equalsIgnoreCase(Constants.RESTAURANT_GENERAL_CUISINE_FIELD)) {
                columnList.add(Constants.RESTAURANT_CUISINES_FIELD);
                columnList.add(Constants.RESTAURANT_CS_CUISINE_FIELD);
            } else if (property.equalsIgnoreCase(Constants.RESTAURANT_GENERAL_ALL_FIELD)) {
                columnList.add(Constants.RESTAURANT_CS_DISH_FIELD);
                columnList.add(Constants.RESTAURANT_CS_CUISINE_FIELD);
            } else if (property.equalsIgnoreCase(Constants.RESTAURANT_CS_DISH_FIELD)){
                columnList.add(Constants.RESTAURANT_CS_NAME_FIELD);
                columnList.add(Constants.RESTAURANT_CS_DISH_FIELD);
            } else{
                columnList.add(property);
            }

            StringJoiner sj = new StringJoiner(" OR ", "", "");
            for (String clm : columnList) {
                if(clm.equalsIgnoreCase(Constants.RESTAURANT_CS_NAME_FIELD) && exactNameMatchString != null){
                    sj.add(Constants.RESTAURANT_NAME_FIELD + ":(" + exactNameMatchString + ")");
                }
                if(clm.equalsIgnoreCase(Constants.RESTAURANT_ADDRESSCITY_FIELD) && exactCityMatchString != null){
                    tagString = exactCityMatchString;
                }
                if(clm.equalsIgnoreCase(Constants.RESTAURANT_RELIGIOUS_FIELD)){
                    // search in cuisine as well
                    sj.add(Constants.RESTAURANT_CS_CUISINE_FIELD + ":(" + tagString +")");
                    // when search in religious field, converted to another format as the annotated values is only veg or nonveg
                    List<String> newValues = values.stream().map(x->getFormatedReligiousValues(x)).collect(Collectors.toList());
                    tagString = restaurantDao.constructQueryStringFromTags(newValues, Constants.DELIMITER_OR, false);
                }
                sj.add(clm + ":(" + tagString + ")");
            }

            qsj.add("(" + sj.toString() + ")");
        }
        return qsj.toString();
    }


    public SolrQuery processGeneralQueries(Map<String, List<String>> restProperties, UserProfile userProfile){
        System.out.println("Enter processGeneralQueries function " + restProperties);
        if(restProperties != null){
            System.out.println(restProperties.size());
        }
        SolrQuery solrQuery = null;
        if(userProfile != null && userProfile.getLikedRests() != null && userProfile.getLikedRests().size() != 0){
            List<String> likedRest = userProfile.getLikedRests();

            // search for similar restaurants using user liked restaurants
//            String queryString = restaurantDao.constructQueryStringFromRestaurants(likedRest,null, Arrays.asList(Constants.RESTAURANT_CS_CUISINE_FIELD, Constants.RESTAURANT_CS_DISH_FIELD));
//            System.out.println("Query string from user likedRest:" + queryString);
//            solrQuery = new SolrQuery(queryString);
//
            // get from niche_items and discover items
            solrQuery = restaurantDao.getDiscoveredItems(likedRest);

            // filter using other aspects if available
            filterUsingRestProperties(solrQuery,restProperties);

            // filter using user liked restaurant ids.
            restaurantDao.addFilterQuery(solrQuery,Constants.RESTAURANT_ID_FIELD, likedRest, false);
        }

        // when no user profile or liked restaurants for constructing queries, using the properties
        if(solrQuery == null && restProperties != null && restProperties.size() != 0){
            solrQuery = parseRestProperty(restProperties);
        }

        // if there are still get some popular restaurants or random restaurants
        if(solrQuery == null){
            System.out.println("Did not find your liked rests or other properties given, so here are the popular restaurants");
            solrQuery = restaurantDao.getPopurlarRestaurants(null);
        }
        return solrQuery;
    }


    public void filterUsingRestProperties(SolrQuery solrQuery, Map<String, List<String>> restProperties){
        if(solrQuery == null || restProperties == null || restProperties.size() == 0)
            return;
        String fqString = convertGeoLocation2SolrString(restProperties);
        String propertyString = convertRestProperty2SolrString(restProperties);

        // combine both as a filter query
        if(propertyString.toString().length() != 0){
            if(fqString == null || fqString.length() == 0){
                fqString = propertyString.toString();
            }else{
                fqString = fqString + " AND " + propertyString.toString();
            }
        }

        // add the filter query
        if(fqString != null && fqString.length() != 0){
            solrQuery.addFilterQuery(fqString);
        }
    }

    public void printProperties(Map<String, List<String>> properties){
        if(properties == null){
            return;
        }
        properties.forEach((att, vallist)->{
            System.out.println("attribute:" + att);
            System.out.println(vallist);
        });
    }

    public void refineRestPropertyWithUserProfile(Map<String, List<String>> properties, UserProfile userProfile, List<String> contextProperties){
        // if user profile is null or user profile does not have any context preference and likerests and likedRestAssociations, return directly.
        if(userProfile == null || (!userProfile.hasContextPreference() && ((userProfile.getLikedRestAssociations() == null) || userProfile.getLikedRestAssociations().size() == 0)))
            return;

        // ignore the refinement if searching for a restaurant name, or dish
        if(properties.get(Constants.RESTAURANT_CS_NAME_FIELD) != null ||
                properties.get(Constants.RESTAURANT_CS_DISH_FIELD) != null ||
                properties.get(Constants.RESTAURANT_CS_INGREDIENT_FIELD) != null ||
                properties.get(Constants.RESTAURANT_CS_BEVERAGES_FIELD) != null)
            return;

        // All the dimensions that we care for the refinement, suggested by Anissa
        Set<String> attributesToConsider = Sets.newHashSet(Constants.RESTAURANT_CS_CUISINE_FIELD,
                Constants.RESTAURANT_GENERAL_LOCATION_FIELD,
                Constants.RESTAURANT_CS_PRICERANGE_FIELD);

        // if user has context preference and contextProperties are not empty, refine property with context preference
        if(contextProperties != null && contextProperties.size() != 0 && userProfile.hasContextPreference()){
            // get the preference for the required context
            Map<String, Map<String, Long>> preference = new HashedMap();
            for(String context : contextProperties){
                if(userProfile.getContextPreference().get(context) != null){
                    preference = UserProfiling.mergeMap(preference, userProfile.getContextPreference().get(context));
                }
            }

            // return directly if no such preference is found
            if(preference != null && preference.size() != 0){
                refineProperty(properties,preference,attributesToConsider,preferenceFrequencyLimit);
            }
        }

        // if user have like associations, refine properties with like associations.
        if(userProfile.getLikedRestAssociations() != null && userProfile.getLikedRestAssociations().size() != 0){
           // derive the statistics from the likedRestsAssociations
            Map<String, Map<String,Long>> likedRestAttributePreference = UserProfiling.derivePreferenceFromUserLikedRests(userProfile);

            // print the like associations
            likedRestAttributePreference.forEach((k,v)->{
                System.out.println(k + ":");
                v.forEach((kk,vv)->{System.out.println(kk+"--"+vv);});
            });

            if(likedRestAttributePreference != null && likedRestAttributePreference.size() != 0){
                // user likeAssociations to refine the properties
                refineProperty(properties, likedRestAttributePreference,attributesToConsider,preferenceFrequencyLimit);
            }
        }
    }

    private boolean propertyOverlapped(Map<String, List<String>> properties, String attribute){
        if(properties.containsKey(attribute)){
            return true;
        }
        Set<String> locationRelatedAttributes = Sets.newHashSet(Constants.RESTAURANT_CS_ADDRESS_FIELD, Constants.RESTAURANT_ADDRESSCITY_FIELD, Constants.RESTAURANT_ADDRESSCOUNTRY_FIELD,
                Constants.RESTAURANT_ADDRESS_FIELD, Constants.RESTAURANT_GENERAL_LOCATION_FIELD);
        if(locationRelatedAttributes.contains(attribute) && CollectionUtils.intersection(locationRelatedAttributes, properties.keySet()).size() > 0){
            return true;
        }
        Set<String> cuisineRelatedAttributes = Sets.newHashSet(Constants.RESTAURANT_CS_CUISINE_FIELD, Constants.RESTAURANT_CUISINES_FIELD,
                Constants.RESTAURANT_GENERAL_CUISINE_FIELD);
        if(cuisineRelatedAttributes.contains(attribute) && CollectionUtils.intersection(cuisineRelatedAttributes, properties.keySet()).size() > 0){
            return true;
        }
        return false;
    }
    private void refineProperty(Map<String, List<String>> properties, Map<String, Map<String,Long>> preference,Set<String> attributesToConsider, int limit){
        if(properties == null || preference == null || preference.size() == 0 || attributesToConsider == null || attributesToConsider.size() == 0)
            return;

        // set default frequency limit if no valid value is given
        limit = limit <= 0 ? preferenceFrequencyLimit : limit;

        // select the top value for each
        for(Map.Entry<String, Map<String, Long>> outerEntry : preference.entrySet()){
            String attri = outerEntry.getKey();
            Map<String,Long> valCount = outerEntry.getValue();
            if(!propertyOverlapped(properties, attri) && attributesToConsider.contains(attri)){
                Map<String, Long> result = new LinkedHashMap<>();

                //sort map by count in descending order...
                valCount.entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .forEachOrdered(x -> result.put(x.getKey(), x.getValue()));

                // get the top 2 preferences whose frequency is larger than the limit, and add it to property
                int count = 0;
                List<String> valList = new ArrayList<String>();
                for(Map.Entry<String,Long> entry : result.entrySet()){
                    if(entry.getValue() > limit){
                        valList.add(entry.getKey());
                        count ++;
                        if(count == 2){
                            break;
                        }
                    }
                }
                if(valList.size() != 0){
                    properties.put(attri, valList);
                }
            }
        }
    }


    public SolrDocumentList rerankResultsWithUserprofile(SolrDocumentList existingResults, UserProfile userProfile){
        if(userProfile == null || userProfile.getLikedRests() == null || userProfile.getLikedRests().size() == 0)
            return existingResults;
        List<String> likedRests = userProfile.getLikedRests();
        LinkedHashMap<SolrDocument, Double> resultsScoreMap = new LinkedHashMap<>();

        // print the original results
//        System.out.println("Before reranking the results are: ");
//        existingResults.forEach(x->{
//            System.out.println(x.getFieldValue(Constants.RESTAURANT_ID_FIELD) + " --" + x.getFieldValue(Constants.RESTAURANT_NAME_FIELD)  + "--" + x.getFieldValue(Constants.SOLR_SCORE) + "--" + x.getFieldValue(Constants.SOLR_DISTANCE));
//        });

        // get weights and normalized scores
        float maxScore = Collections.max(existingResults.stream().map(x->(Float)x.getFieldValue(Constants.SOLR_SCORE)).collect(Collectors.toList()));
        float maxDistance = -1f;
        if(existingResults.stream().filter(x->x.getFieldValue(Constants.SOLR_DISTANCE) != null).count() > 0){
            maxDistance = Collections.max(existingResults.stream().filter(x->x.getFieldValue(Constants.SOLR_DISTANCE) != null).map(x->Float.valueOf(String.valueOf(x.getFieldValue(Constants.SOLR_DISTANCE)))).collect(Collectors.toList()));
        }

        float w_score = 0.333f;
        float w_dist = 0.333f;
        float w_likeness = 0.333f;
        if(maxDistance == -1f){
            w_score = 0.3f;
            w_dist = 0.0f;
            w_likeness = 0.7f;
        }else{
            w_score = 0.3f;
            w_dist = 0.3f;
            w_likeness = 0.4f;
        }

        // get the final score for each result
        for (SolrDocument x : existingResults) {
            double refinedScore = 0.0f;
            if(maxScore != 0.0f){
                refinedScore += w_score * (Float)x.getFieldValue(Constants.SOLR_SCORE)/maxScore;
            }
            if(maxDistance != -1f){
                if(maxDistance != 0f){
                    refinedScore += (1 - w_dist  * (Double)x.getFieldValue(Constants.SOLR_DISTANCE)/maxDistance);
                }
            }
            if(likedRests.contains(String.valueOf(x.getFieldValue(Constants.RESTAURANT_ID_FIELD)))){
                refinedScore += w_likeness;
            }
            resultsScoreMap.put(x,refinedScore);
        }

//        resultsScoreMap.forEach((k,v)->{
//            System.out.println(k.getFieldValue(Constants.RESTAURANT_ID_FIELD) + ":" + v);
//        });
        // rerank the results based on the final score
        List<SolrDocument> rst = resultsScoreMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder())).map(Map.Entry::getKey).collect(Collectors.toList());

//         print the results after reranking
//        System.out.println("After reranking the results are: ");
//        rst.forEach(x->{
//            System.out.println(x.getFieldValue(Constants.RESTAURANT_ID_FIELD) + " --" + x.getFieldValue(Constants.RESTAURANT_NAME_FIELD) + "--" + x.getFieldValue(Constants.SOLR_SCORE) + "--" + x.getFieldValue(Constants.SOLR_DISTANCE));
//        });
        SolrDocumentList results2return = new SolrDocumentList();
        rst.forEach(x->{results2return.add(x);});
        return results2return;
    }

    private static String getFormatedReligiousValues(String value){
        /*  the current religious dictionary:
            vegan
            vegetarian
            halal
            kosher
            veg
            nonveg
            non-veg
            non vegetarian
            non-vegetarian
            nonvegetarian
         */
        Set<String> vegSet = Sets.newHashSet("vegan", "vegetarian", "veg");
        Set<String> nonVegSet = Sets.newHashSet("nonveg", "non-veg", "non vegetarian", "non-vegetarian","nonvegetarian");
        if(vegSet.contains(value.toLowerCase())){
            return "veg";
        }
        if(nonVegSet.contains(value.toLowerCase())){
            return "nonveg";
        }
        return value;
    }

    private String cityFromProperties(Map<String, List<String>> properties){
        try{
            return properties.entrySet().stream().filter(x ->x.getKey().equalsIgnoreCase("addresscity"))
                    .collect(Collectors.toList()).stream().map(x -> x.getValue()).findFirst().get().get(0);
        }catch (Exception e){
            return "";
        }
    }

}
