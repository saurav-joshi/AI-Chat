package com.aibot.recommender;

import com.aibot.entity.Offer;
import com.opencsv.CSVReader;
import org.apache.commons.collections.map.HashedMap;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class OfferHandler {
    private Map<String,List<Offer>> offerData;
    private Set<String> restaurantsWithValidOffers;

    public OfferHandler(String offerFilePath) {
        offerData = loadOfferData(offerFilePath);
        // get restaurants with valid offers
        restaurantsWithValidOffers = new HashSet<>();
        offerData.forEach((id,offerList)->{
            List<Offer> validOffers = offerList.stream().filter(x->x.isValid()).collect(Collectors.toList());
            if(validOffers != null && validOffers.size() != 0){
                restaurantsWithValidOffers.add(id);
            }
        });
    }

    public Map<String,List<Offer>> loadOfferData(String offerFilePath){
        Map<String,List<Offer>> offers = new HashedMap();
        try {
            CSVReader reader = new CSVReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(offerFilePath)));
            String [] nextLine;
            // title line
            // format: 0:id 1:category 2:cleaned_merchant_name 3:description 4:merchant_id 5:short_desc 6:start_date 7:end_date 8:tnc
            nextLine = reader.readNext();
            while ((nextLine = reader.readNext()) != null) {
                //String id, String merchant_name, String merchant_id, String start_date, String end_date, String short_desc, String long_desc, String tnc
                String genId = nextLine[4];
                Offer anOffer = new Offer(nextLine[0],nextLine[2],genId,nextLine[6],nextLine[7],nextLine[5],nextLine[3],nextLine[8]);
                if(offers.get(genId) != null){
                    offers.get(genId).add(anOffer);
                }else{
                    List<Offer> offerList = new ArrayList<>();
                    offerList.add(anOffer);
                    offers.put(genId,offerList);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return offers;
    }

    public Map<String, List<Offer>> getOfferData() {
        return offerData;
    }

    public void setOfferData(Map<String, List<Offer>> offerData) {
        this.offerData = offerData;
    }

    public Set<String> getRestaurantsWithValidOffers() {
        return restaurantsWithValidOffers;
    }

    public void setRestaurantsWithValidOffers(Set<String> restaurantsWithValidOffers) {
        restaurantsWithValidOffers = restaurantsWithValidOffers;
    }

    public List<Offer> getValidOffersByRestaurant(String restId){
        if(offerData.get(restId) == null){
            return null;
        }
        List<Offer> validOffers = offerData.get(restId).stream().filter(x->x.isValid()).collect(Collectors.toList());
        return validOffers;
    }

    private void printOffers(){
        offerData.forEach((id,olist)->{
            System.out.println("restaurant id: " + id);
            System.out.println("offer no: " + olist.size());

            olist.forEach(o->{
                System.out.println("offer id:" + o.getId());
                System.out.println("name: " + o.getMerchant_name());
                System.out.println("short desc: " + o.getShort_desc());
                System.out.println("start date: " + o.getStart_date());
                System.out.println("end date: " + o.getEnd_date() +"\n");
            });

        });
    }

}
