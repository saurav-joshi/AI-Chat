package com.aibot.entity;

import java.util.List;

public class RecommenderResultsRestaurant {
    String id;
    String name;
    String address;
    String rating;
    String image;
    String geo;
    String cuisine;
    String dish;
    String mstar;
    String price;
    List<Offer> offers;
    //boolean allowBooking;
    String distance;
    String telephone;
    String options;
    String website;
    String city;
    String operating_hours;
    String chope_link;
    public RecommenderResultsRestaurant(String id, String name, String address, String rating,
                                        String image, String geo,
                                        String cuisine, String dish, String mstar, String price, List<Offer> offers,
                                        String distance, String telephone, String options,String website,
                                        String city, String operating_hours, String url) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.rating = rating;
        this.image = image;
        this.geo = geo;
        this.cuisine = cuisine;
        this.dish = dish;
        this.mstar = mstar;
        this.price = price;
        this.offers = offers;
        this.distance = distance;
        this.telephone = telephone;
        this.options = options;
        this.website = website;
        this.city = city;
        this.operating_hours = operating_hours;
        //this.allowBooking = Math.random() < 0.5; //removing this for a moment
        this.chope_link = url;
    }

    public RecommenderResultsRestaurant(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getGeo() {
        return geo;
    }

    public void setGeo(String geo) {
        this.geo = geo;
    }

    public String getCuisine() {
        return cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public String getDish() {
        return dish;
    }

    public void setDish(String dish) {
        this.dish = dish;
    }

    public String getMstar() {
        return mstar;
    }

    public void setMstar(String mstar) {
        this.mstar = mstar;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public List<Offer> getOffers() {
        return offers;
    }

    public void setOffers(List<Offer> offers) {
        this.offers = offers;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

//    public boolean getAllowBooking() {
//        return allowBooking;
//    }

//    public void setAllowBooking(boolean allowBooking) {
//        this.allowBooking = allowBooking;
//    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getChope_link() {
        return chope_link;
    }

    public void setChope_link(String city) {
        this.chope_link = chope_link;
    }

    public String getOperating_hours() {
        return operating_hours;
    }

    public void setOperating_hours(String operating_hours) {
        this.operating_hours = operating_hours;
    }

    public void print(){
        System.out.println("========================");
        System.out.println("-Id:" + id);
        System.out.println("-Name:" + name);
        System.out.println("-Address:" + address);
        System.out.println("-Rating:" + rating);
        System.out.println("-Image:" + image);
        System.out.println("-Geo:" + geo);
        System.out.println("-Cuisine:" + cuisine);
        System.out.println("-Dish:" + dish);
        System.out.println("-Michellin star:" + mstar);
        System.out.println("-Price:" + price);
        System.out.println("_Offers:" + offers);
        System.out.println("_Telephone:" + telephone);
        System.out.println("_Options:" + options);
        System.out.println("_Website:" + website);
        System.out.println("_city:" + city);
        System.out.println("_operating_hours:" + operating_hours);

    }
}
