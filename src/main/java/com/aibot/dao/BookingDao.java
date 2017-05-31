package com.aibot.dao;

import com.aibot.entityextraction.DateExtraction;
import com.aibot.state.BookingState;
import org.apache.commons.collections.map.HashedMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class BookingDao {
    public enum detailType {restaurant_id, pax, date, phone, email, special}
    public final String bookingActivate = "book";
    public final String bookingCancel = "cancelbook";
    public final String bookingConfirm = "confirmbook";
    private static BookingDao bookingDao = null;
    private final String tableName = "booking";
    public static BookingDao getInstance() {
        if (bookingDao == null) {
            bookingDao = new BookingDao();
        }
        return bookingDao;
    }

    //insert a new record and return
    public void insertNewBooking(int qaId, int bookingRestId, String userId) {
        try {
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement("INSERT INTO " + tableName + " (qa_id,restaurant_id,user_id,email,phone,date,special,pax,confirm) " +
                    "VALUES( " + "?,?,?,?,?,?,?,?,?)");

            ps.setInt(1,qaId);
            ps.setInt(2,bookingRestId);
            ps.setString(3,userId);

            ps.setString(4,"");
            ps.setString(5,"");
            ps.setString(6,"");
            ps.setString(7,"");
            ps.setString(8,"");
            ps.setInt(9,0);//default confirm is 0, mean no

            ps.execute();
            ps.close();
            connection.close();

        } catch (Exception e) {
            System.out.println("Error inserting booking: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //update booking
    public void updateBookingDetail(int qaId, String userId, String key, String value){
        try{
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement("UPDATE " + tableName + " SET " + key + "=? WHERE qa_id = " + qaId + " AND user_id = '" + userId + "'");
            ps.setString(1,value);
            ps.executeUpdate();
            ps.close();
            connection.close();
        }catch (Exception e){
            System.out.println("Error updating booking detail: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getEmail(String userId){
        try {
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT email FROM " + tableName + " WHERE user_id = '" + userId + "' ORDER BY email DESC");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                return rs.getString("email");
            }

            ps.close();
            connection.close();

        } catch (Exception e) {
            System.out.println("Error getting email by user id: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public String getPhone(String userId){
        try {
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT phone FROM " + tableName + " WHERE user_id = '" + userId + "' ORDER BY phone DESC");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                return rs.getString("phone");
            }
            ps.close();
            connection.close();

        } catch (Exception e) {
            System.out.println("Error getting phone by user id: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public String getDate(int qaId, String userId){
        try {
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT date FROM " + tableName + " WHERE qa_id = " + qaId + " AND user_id = '" + userId + "'");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                return rs.getString("date");
            }
            ps.close();
            connection.close();

        } catch (Exception e) {
            System.out.println("Error getting date by user id: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public String getPax(int qaId, String userId){
        try {
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT pax FROM " + tableName + " WHERE qa_id = " + qaId + " AND user_id = '" + userId + "'");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                return rs.getString("pax");
            }
            ps.close();
            connection.close();

        } catch (Exception e) {
            System.out.println("Error getting date by user id: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }

    public Map<String,String> getBookingDetail(int qaId, String userId){
        Map<String, String> result = new HashedMap();
        try {
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM " + tableName + " WHERE qa_id = " + qaId + " AND user_id = '" + userId + "'");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String restaurantId = rs.getString("restaurant_id");
                if(!restaurantId.isEmpty()){
                    result.put(detailType.restaurant_id.name(), restaurantId);
                }
                String phone = rs.getString("phone");
                if(!phone.isEmpty()){
                    result.put(detailType.phone.name(), phone);
                }
                String email = rs.getString("email");
                if(!email.isEmpty()){
                    result.put(detailType.email.name(), email);
                }
                String date = rs.getString("date");
                if(!date.isEmpty()){
                    result.put(detailType.date.name(), date);
                }
                String special = rs.getString("special");
                if(!special.isEmpty()){
                    result.put(detailType.special.name(), special);
                }
                String pax = rs.getString("pax");
                if(!pax.isEmpty()){
                    result.put(detailType.pax.name(), pax);
                }
            }
            ps.close();
            connection.close();

        } catch (Exception e) {
            System.out.println("Error getting booking detail. " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    //update booking
    public void confirmBooking(int qaId, String userId, int restaurantId){
        try{
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement("UPDATE " + tableName + " SET confirm = 1 WHERE qa_id = " + qaId
                    + " AND user_id ='" + userId + "'"
                    + " AND restaurant_id = " + restaurantId);
            ps.executeUpdate();
            ps.close();
            connection.close();
        }catch (Exception e){
            System.out.println("Error confirming booking: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isBookingExisted(int qaId, String userId){
        boolean existed = false;

        try{
            Connection connection = MySQL.getConnection();
            String query = "SELECT EXISTS(SELECT * FROM " + tableName + " WHERE  qa_id = " + qaId
                    + " AND user_id = '" + userId + "') AS existed";
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                existed = rs.getInt("existed")==1;
            }
            ps.close();
            connection.close();
        }catch (Exception e){
            System.out.println("Exception while checking profile exists: " + e.getMessage());
            e.printStackTrace();
        }
        return existed;
    }

    //Faking the data, later use 3rd party
    public boolean isValidTimeSlot(String date, String time, String restaurantId){
        //TODO: check with 3rd party API
        boolean existed = false;

        try{
            Connection connection = MySQL.getConnection();
            String query = "SELECT EXISTS(SELECT * FROM time_slot" + " WHERE  restaurant_id = " + restaurantId
                    + " AND date = '" + date + "'" + " AND time = '" + time + "')" + " AS existed";
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                existed = rs.getInt("existed")==1;
            }
            ps.close();
            connection.close();
        }catch (Exception e){
            System.out.println("Exception while checking profile exists: " + e.getMessage());
            e.printStackTrace();
        }
        return existed;
    }

    //Faking the data, later use 3rd party
    //dinner and lunch
    public List<String> selectValidTimeSlot(String date, String restaurantId, String timezone){
        //TODO: check with 3rd party API
        List<String> slot = new ArrayList<>();

        try{
            Connection connection = MySQL.getConnection();
            String query = "SELECT * FROM time_slot " + " WHERE  restaurant_id = " + restaurantId + " AND date = '" + date + "' order by time" ;
            PreparedStatement ps = connection.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            Date today = BookingState.getCurrentDate(timezone);//TODO: choose timezone
            while (rs.next()) {
                int time = Integer.valueOf(rs.getString("time").split(":")[0]);
                if(date.equalsIgnoreCase(DateExtraction.formatDateOnly(today)) & today.getHours()>time){
                    //System.out.println("Time in the past: " + time);
                    continue;
                }
                String time_slot = new StringBuilder("Date: ").append(rs.getString("date"))
                        .append(", Time: ").append(rs.getString("time")).toString();
                if (!slot.contains(time_slot)) slot.add(time_slot);
            }
            ps.close();
            connection.close();
        }catch (Exception e){
            System.out.println("Exception while checking profile exists: " + e.getMessage());
            e.printStackTrace();
        }
        return slot;
    }

    public void createValidTimeSlot(List<String> restaurantIds){
        //TODO: check with 3rd party API

        Random random = new Random();

        List<String> times = new ArrayList<>();
        for(int i=11; i< 24; i++){
            times.add(i +":00");
        }

        try {
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement("INSERT INTO time_slot " + " (restaurant_id,date,time) " +
                    "VALUES( " + "?,?,?)");
            connection.setAutoCommit(false);
            int count =0;
            for(int i=0; i< 14; i++){
                for(int j =0 ; j< 3; j++){
                    for(String restaurantId: restaurantIds){
                        ps.setInt(1,Integer.valueOf(restaurantId));
                        Date today = new Date();
                        today.setDate(today.getDate()+i);
                        ps.setString(2,DateExtraction.formatDateOnly(today));
                        ps.setString(3,times.get(random.nextInt(times.size())));
                        ps.addBatch();
                        count++;
                        if(count%1000==0){
                            System.out.println("Records to insert: " + count);
                            ps.executeBatch();
                            connection.commit();
                        }
                    }
                }
            }

            //last batch
            ps.executeBatch();
            connection.commit();
            ps.close();
            connection.close();
        } catch (Exception e) {
            System.out.println("Error inserting time slot: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
