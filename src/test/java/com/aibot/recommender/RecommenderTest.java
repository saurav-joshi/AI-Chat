package com.aibot.recommender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.aibot.qa.GlobalConstants;
import com.aibot.dao.RestaurantDao;
import com.aibot.entity.*;
import com.aibot.entity.RecommenderQuery;
import com.aibot.entity.RecommenderResultsRestaurant;
import com.aibot.entity.UserProfile;
import junit.framework.TestCase;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;

import java.util.*;

@Ignore
public class RecommenderTest extends TestCase {

    private RecommenderQuery constructSampleRestQuery(){
        RecommenderQuery restRq = new RecommenderQuery();
        restRq.setTarget(Constants.RecommenderTarget.RESTAURANT);
        restRq.setQueryType(RecommenderQuery.QueryType.LookingForClassMember);
        // set properties
        Map<String, List<String>> properties = new HashMap<>();
        properties.put(Constants.RESTAURANT_GENERAL_LOCATION_FIELD, Arrays.asList("riverside"));
//        properties.put(Constants.RESTAURANT_CS_NAME_FIELD, Arrays.asList("chicken rice", "noodles"));
//        properties.put(Constants.RESTAURANT_CS_CUISINE_FIELD, Arrays.asList("chinese"));
//        properties.put(Constants.RESTAURANT_CS_MEAL_FIELD, Arrays.asList("lunch"));
//        properties.put(Constants.RESTAURANT_CS_INGREDIENT_FIELD, Arrays.asList("lobster"));
//        properties.put(Constants.RESTAURANT_CS_DISH_FIELD, Arrays.asList("dim sum"));
//        properties.put(Constants.CONTEXT_ACCOMPANY, Arrays.asList("withMom"));
//        properties.put(Constants.RESTAURANT_CS_PRICERANGE_FIELD, Arrays.asList("high","very high","medium"));

        restRq.setProperties(properties);

        // set similar to
        Map<Constants.RecommenderTarget, Map<String, List<String>>> similarTo = new HashMap<>();
        Map<String, List<String>> similarToFields = new HashMap<>();
//        similarToFields.put(Constants.RESTAURANT_ID_FIELD, Arrays.asList("301223658"));
//        similarToFields.put(Constants.RESTAURANT_CS_NAME_FIELD, Arrays.asList("McDonald's"));
//        similarToFields.put(Constants.RESTAURANT_CS_DISH_FIELD, Arrays.asList("burger"));
//        similarToFields.put(Constants.RESTAURANT_CS_CUISINE_FIELD, Arrays.asList("thai"));
//        similarToFields.put(Constants.RESTAURANT_DESCRIPTION_FIELD, Arrays.asList("chicken rice"));
//        similarToFields.put(Constants.RESTAURANT_GENERAL_LOCATION_FIELD, Arrays.asList("kallang"));
//        similarToFields.put(Constants.RESTAURANT_GEO_FIELD, Arrays.asList("1.333134,103.742288"));
//        similarTo.put(Constants.RecommenderTarget.RESTAURANT,similarToFields);
        restRq.setSimilarTo(similarTo);

        // set rank criteria
        restRq.setRankCriteria(Arrays.asList(Constants.RankCriteria.RELEVANCE));
        restRq.setPages(1);
        return restRq;
    }

    private RecommenderQuery constructSampleSupriseMeRestQuery(){
        RecommenderQuery restRq = new RecommenderQuery();
        restRq.setTarget(Constants.RecommenderTarget.RESTAURANT);
        restRq.setQueryType(RecommenderQuery.QueryType.LookingForClassMember);
        // set properties
        Map<String, List<String>> properties = new HashMap<>();
        properties.put(Constants.CONTEXT_ACCOMPANY,Arrays.asList("withMom"));
        properties.put(Constants.RESTAURANT_GENERAL_LOCATION_FIELD, Arrays.asList("orchard road"));
//        properties.put(Constants.RESTAURANT_GENERAL_LOCATION_FIELD, Arrays.asList("orchard"));
        restRq.setProperties(properties);

        return restRq;
    }

    private RecommenderQuery constructSampleReviewQuery(){
        RecommenderQuery restRq = new RecommenderQuery();
        restRq.setTarget(Constants.RecommenderTarget.REVIEW);

        // set properties
        Map<String, List<String>> properties = new HashMap<>();
//        properties.put(Constants.REVIEW_ID_FIELD, Arrays.asList("e699ce57-0df1-4acf-88a7-f044df9b18f2"));
//        properties.put(Constants.REVIEW_RESTAURANTID_FIELD, Arrays.asList("item_1_saturam_restaurant_metadata_info:rv:543b497a894ce437dc98510040a6528c"));
        properties.put(Constants.REVIEW_TEXT_FEILD, Arrays.asList("crab"));
        properties.put(Constants.RESTAURANT_CS_NAME_FIELD, Arrays.asList("Jumbo"));
//        restRq.setProperties(properties);

        // set similar to
        Map<Constants.RecommenderTarget, Map<String, List<String>>> similarTo = new HashMap<>();
        Map<String, List<String>> similarToFields = new HashMap<>();
        similarToFields.put(Constants.REVIEW_ID_FIELD, Arrays.asList("f5bca607-cd47-4893-9a2a-48a4468e81c6"));
//        similarToFields.put(Constants.RESTAURANT_CS_NAME_FIELD, Arrays.asList("chicken rice"));
        similarToFields.put(Constants.REVIEW_TEXT_FEILD, Arrays.asList("chicken rice", "noodles"));
        similarTo.put(Constants.RecommenderTarget.REVIEW, similarToFields);
        restRq.setSimilarTo(similarTo);

        // set rank criteria
        restRq.setRankCriteria(Arrays.asList(Constants.RankCriteria.SENTIMENT));
        return restRq;
    }

    public UserProfile constructUserProfile(){
        Map<String, Map<String,Map<String, Long>>> contextPreference = new HashMap<>();

        Map<String, Long> map1 = new HashMap<>();
//        map1.put("chinese", 1l);
//        map1.put("italian", 10l);
        map1.put("indian", 10l);
        map1.put("chinese", 5l);
        map1.put("italian", 3l);
        Map<String,Map<String, Long>> mmap1 = new HashMap<>();
        mmap1.put(Constants.RESTAURANT_CS_CUISINE_FIELD,map1);
        contextPreference.put("withMom",mmap1);

        Map<String, Long> map2 = new HashMap<>();
        map2.put("chinese", 10l);

        Map<String, Long> map3 = new HashMap<>();
//        map3.put("chicken rice", 5l);
//        map3.put("noodles", 3l);
//        map3.put("naan", 2l);

        Map<String,Map<String, Long>> mmap2 = new HashMap<>();
        mmap2.put(Constants.RESTAURANT_CS_CUISINE_FIELD,map2);
        mmap2.put(Constants.RESTAURANT_CS_DISH_FIELD,map3);
        contextPreference.put("withFamily", mmap2);


        Map<String, Long> map4 = new HashMap<>();
        map4.put("Japanese", 10l);
        Map<String,Map<String, Long>> mmap3 = new HashMap<>();
        mmap3.put(Constants.RESTAURANT_CS_CUISINE_FIELD, map4);
        contextPreference.put("General", mmap3);

        UserProfile up = new UserProfile();
        up.setContextPreference(contextPreference);
        up.setUserId("1");

        // create a list of likedList
        List<String> likedRest = Arrays.asList("300137174","300623318","301475355");
        up.setLikedRests(likedRest);
        return up;
    }

    public void testGetRecommendations() throws Exception {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.WARN);

        Recommender rm = new Recommender();
        RecommenderQuery restRq = constructSampleRestQuery();
//        RecommenderQuery restRq = constructSampleSupriseMeRestQuery();
//        RecommenderQuery restRq = constructSampleReviewQuery();

        UserProfile uprofile = constructUserProfile();
        SolrDocumentList results = rm.calculateRecommendations(restRq, uprofile);

//        List<String> rst = rm.resultWrapper.getSimpleResults(restRq, results);
        List<RecommenderResultsRestaurant> rst = rm.resultWrapper.getResults(restRq,results, rm.offerHander);
        System.out.println("==========================");
        if(rst == null){
            System.out.println("Sorry, we cannot find any match");
        }else{
            System.out.println("The results to return are:" + rst.size());
            rst.forEach(x->x.print());
        }


//        for(Object arst : rst){
//            try{
//                RecommenderResultsRestaurant rest = (RecommenderResultsRestaurant) arst;
//                rest.print();
//            }catch (Exception e){
//                RecommenderResultsReview rest = (RecommenderResultsReview) arst;
//                rest.print();
//            }
//        }
    }

    public void testParseQuery() throws Exception {
        String s = "next to orchard";
        System.out.println(s.substring(s.indexOf("next to ") + "next to ".length(), s.length()));
        List<String> alist = Arrays.asList("1","2","3","4");
        ListIterator li = alist.listIterator(alist.size()-1);
        while(li.hasPrevious()) {// Iterate in reverse.
            System.out.println(li.previous());
        }
    }

    public void testConvertPreference2SolrString(){
        Map<String, Long> map1 = new HashMap<>();
        map1.put("chinese", 1l);
        map1.put("italian", 10l);
        map1.put("indian", 2l);
        Map<String,Map<String, Long>> mmap1 = new HashMap<>();
        mmap1.put("#cuisine",map1);

        Map<String, Long> map2 = new HashMap<>();
        map2.put("chinese", 3l);
        map2.put("singaporean", 3l);
        map2.put("indian", 3l);

        Map<String, Long> map3 = new HashMap<>();
        map3.put("cheese", 3l);
        map3.put("pizza", 3l);
        map3.put("apple", 3l);

        Map<String,Map<String, Long>> mmap2 = new HashMap<>();
        mmap2.put("#cuisine",map2);
        mmap2.put("$dish",map3);

        Recommender rm = new Recommender();
        System.out.println(rm.convertPreference2SolrString(mmap2));
    }

    public void getAllRestNames(){
        RestaurantDao restaurantDao = new RestaurantDao(GlobalConstants.solrUrl_V2, "recommender-config/tagsWithWeights.txt");
        SolrQuery solrQuery = new SolrQuery("name:*");
        solrQuery.setFields("name");
        solrQuery.setRows(9000);
        SolrDocumentList results = restaurantDao.searchDoc(solrQuery);
        Set<String> restNames = new HashSet<>();
        results.forEach(r->{
            restNames.add((String)r.getFieldValue("name"));
        });

        restNames.forEach(x->{
            System.out.println(x);
        });
        System.out.println("Total names:" + restNames.size());
    }

    public void getAllRestInfo(){
        RestaurantDao restaurantDao = new RestaurantDao(GlobalConstants.solrUrl_V2, "recommender-config/tagsWithWeights.txt");
        SolrQuery solrQuery = new SolrQuery("*:*");
        solrQuery.setFields("id, name, address, aggregaterating");
        solrQuery.setRows(9000);
        SolrDocumentList results = restaurantDao.searchDoc(solrQuery);
//        List<Map<String,String>> restInfo = new ArrayList<>();

        results.forEach(r->{
            String id = String.valueOf(r.getFieldValue(Constants.RESTAURANT_ID_FIELD));
            String name = String.valueOf(r.getFieldValue(Constants.RESTAURANT_NAME_FIELD));
            String address = (String)r.getFieldValue(Constants.RESTAURANT_ADDRESS_FIELD);
            String rating = String.valueOf(r.getFieldValue("aggregaterating"));
            System.out.println(id + "||" + name + "||" + address + "||" + rating);
            ;
        });

    }



    public void testmap(){
//        List<String> alist = new ArrayList<>();
//        alist.add("a");
//        alist.add("b");
//        alist.add("c");
//        alist.add("d");
//        String restName = alist.stream().map(e -> e.toString()).reduce(",", String::concat);
//        String restName1 = String.join(",", alist.stream().map(e -> e.toString()).collect(Collectors.toList()));
//        System.out.println(restName);
//        System.out.println(restName1);

        Set<String> aset = new HashSet<>();
        aset.add("a");
        aset.add("b");
        aset.add("c");
        aset.add("d");

        Set<String> bset = new HashSet<>();
        bset.add("a");
        bset.add("b");
        bset.add("d");
        bset.add("e");

        System.out.println(aset.removeAll(bset));
        aset.stream().forEach(e->{System.out.println(e);});
    }

    public void testCap(){
        List<String> alist = new ArrayList<>();
        alist.add("Table at 7");
        alist.add("Big O Cafe and Restaurant");
        alist.add("Choice 3");
        alist.add("9 Goubuli");
        alist.add("Paprika and Cumin - P and C");
        alist.add("Caffe B");
        alist.add("J-Membina Food House");
        alist.add("4 and A Half Gourmands");
        alist.add("A Spoonful Of Sugar");
        alist.add("Bar on 5");
        alist.add("S B Fish Head Steamboat Seafood Restaurant");
        alist.add("E-Sarn Thai Corner");
        alist.add("7 Adam");
        alist.add("O Learys Sports Bar and Grill");
        alist.add("K Ki Sweets");
        alist.add("Los Primos Taberna Y Tapas Bar");
        alist.add("F and B");
        alist.add("7 Degrees C");
        alist.add("A B Mohamed Restaurant");
        alist.add("8 Degree Taiwanese Bistro");
        alist.add("Once Upon A Milkshake");
        alist.add("O Bar Tradehub 21");
        alist.add("Fatien Bar A Vin");
        alist.add("No. 5 Emerald Hill Cocktail Bar");
        alist.add("Mumbai 2 Goa");
        alist.add("R and J Cosy Corner");
        alist.add("P and R Eating House");
        alist.add("Aziz s Dream");
        alist.add("Hot N Spicy Nasi Lemak Family Restaurant");
        alist.add("The 3 Bistro and Bar");
        alist.add("Holland V First Stop Cafe");
        alist.add("Geylang Lor 9 Fresh Frog Leg Porridge");
        alist.add("White Lies 7 Deadly Sins");
        alist.add("Number 9 Boulevard");
        alist.add("4 Fingers Crispy Chicken");
        alist.add("Fill a Pita");
        alist.add("O My Dog");
        alist.add("8 Stanley Street");
        alist.add("8 Noodles");
        alist.add("3 Crab Delicacy Seafood");
        alist.add("7 Sensations");
        alist.add("No.1 Western Food");
        alist.add("A Sweet Tooth");
        alist.add("7 Star Indian Muslim");
        alist.add("Past 2 Present");
        alist.add("7 KICKstart BREWiches");
        alist.add("Lady M Confections");
        alist.add("O Comptoir");
        alist.add("Cups n Canvas");
        alist.add("T Time by 93 Degrees");
        alist.add("Metro Y Restaurant");
        alist.add("O Sole Mio");
        alist.add("6 Stone Jars");
        alist.add("Tori Q");
        alist.add("I am...");
        alist.add("J Dees Gourmet");
        alist.add("Muslim Stall 2");
        alist.add("O Batignolles Wine Bar and French Bistrot");
        alist.add("Happy V");
        alist.add("J and J Special Beef Noodle");
        alist.add("N and B Snacks - Pp");
        alist.add("Chocolat N Spice");
        alist.add("7 Treasures Hut");

        List<String> blist = new ArrayList<>();
        alist.forEach(x->{
            blist.add(x.toLowerCase());
        });

        int truecount = 0;
        for (int i=0; i<blist.size(); i++) {
            String s = blist.get(i);
            String org = alist.get(i);
//            System.out.println(org + "----" + WordUtils.capitalize(s) + "----" + (WordUtils.capitalizeFully(s).equals(org)));
//            if(WordUtils.capitalizeFully(s).equals(org))
//                truecount++;
//
            System.out.println(org + "----" + capitalize(s) + "----" + (capitalize(s).equals(org)));
            if(capitalize(s).equals(org))
                truecount++;
        }
        System.out.println(truecount + "--" + blist.size() + "----" + truecount*1.0/blist.size());



    }
    private static String capitalize(String string) {
        if (string == null) return null;
        String[] wordArray = string.split(" "); // Split string to analyze word by word.
        int i = 0;
        lowercase:
        for (String word : wordArray) {
            if (word != wordArray[0]) { // First word always in capital
                String [] lowercaseWords = {"an", "as", "and", "although", "at", "because", "but", "by", "for", "in", "nor", "of", "on", "or", "so", "the", "to", "up", "yet", "am", "is", "are"};
                for (String word2 : lowercaseWords) {
                    if (word.equals(word2)) {
                        wordArray[i] = word;
                        i++;
                        continue lowercase;
                    }
                }
            }
            char[] characterArray = word.toCharArray();
            characterArray[0] = Character.toTitleCase(characterArray[0]);
            wordArray[i] = new String(characterArray);
            i++;
        }
        return StringUtils.join(wordArray, " "); // Re-join string
    }


}