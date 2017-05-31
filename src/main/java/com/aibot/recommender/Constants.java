package com.aibot.recommender;

public class Constants {
    public static String TAG_EXTEND_FILE_PATH = "recommender-config/tagsWithWeights.txt";
    public static String OFFER_FILE_PATH ="recommender-config/offers_short.csv";

    // restaurants metadata fields
    public static String RESTAURANT_ID_FIELD = "id";
    public static String RESTAURANT_NAME_FIELD = "name";
    public static String RESTAURANT_ADDRESS_FIELD = "address";
    public static String RESTAURANT_ADDRESSCITY_FIELD = "addresscity";
    public static String RESTAURANT_ADDRESSCOUNTRY_FIELD = "addresscountry";
    public static String RESTAURANT_GEO_FIELD = "geo";
    public static String RESTAURANT_IMAGE_FIELD = "image";
    public static String RESTAURANT_TELEPHONE_FIELD = "telephone";
    public static String RESTAURANT_EMAIL_FIELD = "email";
    public static String RESTAURANT_WEBSITE_FIELD = "website";
    public static String RESTAURANT_AMBIENCE_RATING_FIELD = "ambiencerating";
    public static String RESTAURANT_SERVICE_RATING_FIELD = "servicerating";
    public static String RESTAURANT_FOOD_RATING_FIELD = "foodrating";
    public static String RESTAURANT_VALUE_RATING_FIELD = "valuerating";
    public static String RESTAURANT_OVERALL_RATING_FIELD = "aggregaterating";
    public static String RESTAURANT_CUISINES_FIELD = "cuisines";
    public static String RESTAURANT_OPTIONS_FIELD = "options";
    public static String RESTAURANT_PRICERANGE_FIELD = "price";
    // manually curated fields
    public static String RESTAURANT_MSTAR_FIELD = "accolades";
    public static String RESTAURANT_OFFER_FIELD = "offers";

    // BCP data fields
    public static String RESTAURANT_RELIGIOUS_FIELD = "cs_veg_nonveg";
    public static String RESTAURANT_OPERATING_HOURS_FIELD = "operating_hours";
    public static String RESTAURANT_CHOPE_BOOKING_LINK = "chope_link";

    // TG fields
    public static  String RESTAURANT_NICHE_ITEMS_FIELD = "niche_items";
    public static  String RESTAURANT_DISCOVERY_ITEMS_FIELD = "discovery_items";
    public static  String RESTAURANT_POPULAR_ITEMS_FIELD = "popular_items";

    // re-indexed metadata fields
    public static String RESTAURANT_CS_NAME_FIELD = "cs_name";
    public static String RESTAURANT_CS_ADDRESS_FIELD = "cs_address";
    public static String RESTAURANT_CS_PRICERANGE_FIELD = "cs_price";
    public static String RESTAURANT_CS_MSTAR_FIELD = "cs_accolades";

    // attributes derived from reviews
    public static String RESTAURANT_CS_CUISINE_FIELD = "cs_imp_cuisine";
    public static String RESTAURANT_CS_MEAL_FIELD = "cs_imp_meal";
    public static String RESTAURANT_CS_ESTABLISHMENT_FIELD = "cs_imp_establishment";
    public static String RESTAURANT_CS_FEATURES_FIELD = "cs_notimp_features";
    public static String RESTAURANT_CS_PEOPLE_FIELD = "cs_notimp_people";
    public static String RESTAURANT_CS_BEVERAGES_FIELD = "cs_notimp_beverages";
    public static String RESTAURANT_CS_EVENT_FIELD = "cs_notimp_event";
    public static String RESTAURANT_CS_CHEF_FIELD = "cs_notimp_chef";
    public static String RESTAURANT_CS_COOKINGMETHOD_FIELD = "cs_notimp_cookingmethod";
    public static String RESTAURANT_CS_DISH_FIELD = "cs_notimp_dish";
    public static String RESTAURANT_CS_INGREDIENT_FIELD = "cs_notimp_ingredient";
    // old columns when using the old solr
    public static  String RESTAURANT_SENTIMENT_FIELD = RESTAURANT_OVERALL_RATING_FIELD;
    public static String RESTAURANT_DESCRIPTION_FIELD;

    // name space for different tables
    public static String NAMESPACE_FIELD = "category";
    public static String NAMESPACE_VALUE_RESTAURANT = "Restaurant";
    public static String NAMESPACE_VALUE_REVIEW = "ReviewLevelSentiment";


    // general fields
    public static final String RESTAURANT_GENERAL_ALL_FIELD = "general_all";
    public static final String RESTAURANT_GENERAL_LOCATION_FIELD = "general_location";
    public static final String RESTAURANT_GENERAL_CUISINE_FIELD = "general_cuisine";

    // fields of review table
    public static final String REVIEW_ID_FIELD = "id";
    public static final String REVIEW_RESTAURANTID_FIELD = "restaurantID";
    public static final String REVIEW_TEXT_FEILD = "review_text";
    public static final String REVIEW_SENTIMENT_FEILD = "sentiment";

    // default solr fields
    public static final String SOLR_ALL_FIELDS = "*";
    public static final String SOLR_SCORE = "score";
    public static final String SOLR_DISTANCE = "_dist_";
    public static final String SOLR_ORDER_ASC = "asc";
    public static final String SOLR_ORDER_DESC = "desc";

    // delimiter constants
    public static final String DELIMITER_OR = "OR";
    public static final String DELIMITER_AND = "AND";

    // sentiment sort orders
    public static final String SENTIMENT_NEUTRAL = "neutral";

    // constants about the method name of combining relevance and sentiment
    public static final String RELEVANCE_SENTIMENT_METHOD_SORT = "sort";
    public static final String RELEVANCE_SENTIMENT_METHOD_BOOST = "boost";
    public static final int NO_OF_SNIPPETS = 20;

    // distance to the geo
    public static final String GEO_DISTANCE_TO_FILTER = "distance";

    // other parameters from apis
    public static final String API_PARA_GEO = "geo";
    public static final String API_PARA_CITY = "city";

    // target enum
    public enum RecommenderTarget {
        RESTAURANT, REVIEW, DISH
    }

    public enum RankCriteria {
        RELEVANCE, SENTIMENT, POPULARITY, DISTANCE
    }

    public static final String CONTEXT_GENERAL = "General";
    public static final String CONTEXT_ACCOMPANY = "$accompany";
    public static final String CONTEXT_OCASION = "$occasion";
    public static final String CONTEXT_REGULAR = "$regular";

    public static int DEFAULT_PAGE_NO = 1;
    public static int DEFAULT_PAGE_SIZE = 6;
    public static int DEFAULT_PAGE_NO_TO_FETCH = 5;
    public static float DEFAULT_DISTANCE_TO_FILTER = 1;
    public static float DEFAULT_DISTANCE_LIMIT_TO_EXTEND = 8;
}
