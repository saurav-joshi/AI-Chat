package com.aibot.recommender;

import com.aibot.entity.RecommenderResultsReview;
import com.aibot.entity.RecommenderQuery;
import com.aibot.entity.RecommenderResultsRestaurant;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.util.ArrayList;
import java.util.List;

public class ResultWrapper <T> {

    public List<String> getSimpleResults(RecommenderQuery recommenderQuery, SolrDocumentList results){
        List<String> resultsToReturn = new ArrayList<>();
        if(recommenderQuery.getTarget() == Constants.RecommenderTarget.RESTAURANT){
            for(SolrDocument aResult : results){
                resultsToReturn.add((String)aResult.getFieldValue(Constants.RESTAURANT_CS_NAME_FIELD));
            }
        }else if(recommenderQuery.getTarget() == Constants.RecommenderTarget.REVIEW){
            for(SolrDocument aResult : results){
                resultsToReturn.add((String)aResult.getFieldValue(Constants.REVIEW_TEXT_FEILD));
            }
        }else{
            return null;
        }
        return resultsToReturn;
    }

    public List<T> getResults(RecommenderQuery recommenderQuery, SolrDocumentList results, OfferHandler offerHandler){
        if(recommenderQuery.getTarget() == Constants.RecommenderTarget.RESTAURANT){
            List<RecommenderResultsRestaurant> resultsToReturn = new ArrayList<>();
            if(results == null || results.size() == 0){
                return null;
            }
            for(SolrDocument aResult : results){
                String restId = String.valueOf(aResult.getFieldValue(Constants.RESTAURANT_ID_FIELD));
                RecommenderResultsRestaurant rrest = new RecommenderResultsRestaurant(
                        restId,
                        (String)aResult.getFieldValue(Constants.RESTAURANT_NAME_FIELD),
                        (String)aResult.getFieldValue(Constants.RESTAURANT_ADDRESS_FIELD),
                        aResult.getFieldValue(Constants.RESTAURANT_OVERALL_RATING_FIELD) == null ? null : String.valueOf(aResult.getFieldValue(Constants.RESTAURANT_OVERALL_RATING_FIELD)),
                        (String)aResult.getFieldValue(Constants.RESTAURANT_IMAGE_FIELD),
                        (String)aResult.getFieldValue(Constants.RESTAURANT_GEO_FIELD),
                        aResult.getFieldValue(Constants.RESTAURANT_CS_CUISINE_FIELD)==null?null:String.join(",", (ArrayList)aResult.getFieldValue(Constants.RESTAURANT_CS_CUISINE_FIELD)),
                        aResult.getFieldValue(Constants.RESTAURANT_CS_DISH_FIELD)==null?null:String.join(",", (ArrayList)aResult.getFieldValue(Constants.RESTAURANT_CS_DISH_FIELD)),
                        aResult.getFieldValue(Constants.RESTAURANT_MSTAR_FIELD)==null?null:String.join(",", (ArrayList)aResult.getFieldValue(Constants.RESTAURANT_MSTAR_FIELD)),
                        (String)aResult.getFieldValue(Constants.RESTAURANT_PRICERANGE_FIELD),
                        offerHandler.getValidOffersByRestaurant(restId),
                        aResult.getFieldValue(Constants.SOLR_DISTANCE) == null ? null : String.valueOf(aResult.getFieldValue(Constants.SOLR_DISTANCE)),
                        (String)aResult.getFieldValue(Constants.RESTAURANT_TELEPHONE_FIELD),
                        aResult.getFieldValue(Constants.RESTAURANT_OPTIONS_FIELD) == null ? null : String.join(",", (ArrayList)aResult.getFieldValue(Constants.RESTAURANT_OPTIONS_FIELD)),
                        (String)aResult.getFieldValue(Constants.RESTAURANT_WEBSITE_FIELD),
                        (String)aResult.getFieldValue(Constants.RESTAURANT_ADDRESSCITY_FIELD),
                        (String)aResult.getFieldValue(Constants.RESTAURANT_OPERATING_HOURS_FIELD),
                        (String)aResult.getFieldValue(Constants.RESTAURANT_CHOPE_BOOKING_LINK) == null ? null : String.valueOf(aResult.getFieldValue(Constants.RESTAURANT_CHOPE_BOOKING_LINK))
                );
                resultsToReturn.add(rrest);
            }
            return (List<T>) resultsToReturn;
        }

        if(recommenderQuery.getTarget() == Constants.RecommenderTarget.REVIEW){
            List<RecommenderResultsReview> resultsToReturn = new ArrayList<>();
            for(SolrDocument aResult : results){
                RecommenderResultsReview rreview = new RecommenderResultsReview(
                        (String)aResult.getFieldValue(Constants.REVIEW_ID_FIELD),
                        (String)aResult.getFieldValue(Constants.REVIEW_RESTAURANTID_FIELD),
                        (String)aResult.getFieldValue(Constants.REVIEW_TEXT_FEILD)
                );
                resultsToReturn.add(rreview);
            }
            return (List<T>) resultsToReturn;
        }

        System.out.println("Invalid target type");
        return null;
    }

    public RecommenderResultsRestaurant getSingleRestaurantInfo(SolrDocument rest, OfferHandler offerHandler){
        String restId = String.valueOf(rest.getFieldValue(Constants.RESTAURANT_ID_FIELD));
        RecommenderResultsRestaurant resultRest = new RecommenderResultsRestaurant(
                restId,
                (String)rest.getFieldValue(Constants.RESTAURANT_NAME_FIELD),
                (String)rest.getFieldValue(Constants.RESTAURANT_ADDRESS_FIELD),
                rest.getFieldValue(Constants.RESTAURANT_OVERALL_RATING_FIELD) == null ? null : String.valueOf(rest.getFieldValue(Constants.RESTAURANT_OVERALL_RATING_FIELD)),
                (String)rest.getFieldValue(Constants.RESTAURANT_IMAGE_FIELD),
                (String)rest.getFieldValue(Constants.RESTAURANT_GEO_FIELD),
                rest.getFieldValue(Constants.RESTAURANT_CS_CUISINE_FIELD)==null?null:String.join(",", (ArrayList)rest.getFieldValue(Constants.RESTAURANT_CS_CUISINE_FIELD)),
                rest.getFieldValue(Constants.RESTAURANT_CS_DISH_FIELD)==null?null:String.join(",", (ArrayList)rest.getFieldValue(Constants.RESTAURANT_CS_DISH_FIELD)),
                rest.getFieldValue(Constants.RESTAURANT_MSTAR_FIELD)==null?null:String.join(",", (ArrayList)rest.getFieldValue(Constants.RESTAURANT_MSTAR_FIELD)),
                (String)rest.getFieldValue(Constants.RESTAURANT_PRICERANGE_FIELD),
                offerHandler.getValidOffersByRestaurant(restId),
                rest.getFieldValue(Constants.SOLR_DISTANCE) == null ? null : String.valueOf(rest.getFieldValue(Constants.SOLR_DISTANCE)),
                (String)rest.getFieldValue(Constants.RESTAURANT_TELEPHONE_FIELD),
                rest.getFieldValue(Constants.RESTAURANT_OPTIONS_FIELD) == null ? null : String.join(",", (ArrayList)rest.getFieldValue(Constants.RESTAURANT_OPTIONS_FIELD)),
                (String)rest.getFieldValue(Constants.RESTAURANT_WEBSITE_FIELD),
                (String)rest.getFieldValue(Constants.RESTAURANT_ADDRESSCITY_FIELD),
                (String)rest.getFieldValue(Constants.RESTAURANT_OPERATING_HOURS_FIELD),
                (String)rest.getFieldValue(Constants.RESTAURANT_CHOPE_BOOKING_LINK) == null ? null : String.valueOf(rest.getFieldValue(Constants.RESTAURANT_CHOPE_BOOKING_LINK))
                );
        return resultRest;
    }
}
