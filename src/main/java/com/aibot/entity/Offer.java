package com.aibot.entity;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class Offer {
    private String id;
    private String merchant_name;
    private String merchant_id;
    private String start_date;
    private String end_date;
    private String short_desc;
    private String long_desc;
    private String tnc;

    public Offer(String id, String merchant_name, String merchant_id, String start_date, String end_date, String short_desc, String long_desc, String tnc) {
        this.id = id;
        this.merchant_name = merchant_name;
        this.merchant_id = merchant_id;
        this.start_date = start_date;
        this.end_date = end_date;
        this.short_desc = short_desc;
        this.long_desc = long_desc;
        this.tnc = tnc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMerchant_name() {
        return merchant_name;
    }

    public void setMerchant_name(String merchant_name) {
        this.merchant_name = merchant_name;
    }

    public String getMerchant_id() {
        return merchant_id;
    }

    public void setMerchant_id(String merchant_id) {
        this.merchant_id = merchant_id;
    }

    public String getShort_desc() {
        return short_desc;
    }

    public void setShort_desc(String short_desc) {
        this.short_desc = short_desc;
    }

    public String getLong_desc() {
        return long_desc;
    }

    public void setLong_desc(String long_desc) {
        this.long_desc = long_desc;
    }

    public String getTnc() {
        return tnc;
    }

    public void setTnc(String tnc) {
        this.tnc = tnc;
    }

    public String getStart_date() {
        return start_date;
    }

    public void setStart_date(String start_date) {
        this.start_date = start_date;
    }

    public String getEnd_date() {
        return end_date;
    }

    public void setEnd_date(String end_date) {
        this.end_date = end_date;
    }

    public String toJsonString(){
        Map<String,String> amap = new HashMap();
        amap.put("id",id);
        amap.put("merchant_name", merchant_name);
        amap.put("merchant_id", merchant_id);
        amap.put("short_desc",short_desc);
        amap.put("long_desc", long_desc);
        amap.put("start_date", start_date);
        amap.put("end_date", end_date);
        amap.put("tnc", tnc);

        ObjectMapper mapper = new ObjectMapper();
        String resultJsonStr = null;
        try {
            resultJsonStr = mapper.writeValueAsString(amap);
            System.out.println(resultJsonStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultJsonStr;
    }
    public boolean isValid(){
        DateFormat dateFormat = new SimpleDateFormat("d/M/yyyy");
        //get current date time with Date()
        Date today = new Date();
        try {
            Date startDate = dateFormat.parse(start_date);
            Date endDate = dateFormat.parse(end_date);
            if(today.after(startDate) && today.before(endDate)){
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

//    public static void main(String[] args) {
//        Offer offer = new Offer("offer_1","Bookmyshow","300034089","1/10/2016","31/12/2016","10% Off","10% off total bill with min S$100 spend in a single receipt Extended Happy hour from 3pm to pm",null);
//        System.out.println(offer.isValid());
//        System.out.println(offer.toJsonString());
//    }
}
