package com.aibot.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.aibot.entity.Customer;

public class CustomerDao {
    private static CustomerDao customerDao = null;
    String tableName = "customer";
    public static CustomerDao getInstance() {
        if (customerDao == null) {
        	customerDao = new CustomerDao();
        }
        return customerDao;
    }

    public Customer getCustomer(String applicationId){
        Customer customer = null;
        try {
            String query = "SELECT c.name, c.email, c.company_name FROM  " + tableName + " c JOIN customer_app ca ON c.id = ca.customer_id WHERE application_id = ? AND c.STATUS = 2";
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, applicationId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
            	customer = new Customer();
                customer.setName(rs.getString("name"));
                customer.setEmail(rs.getString("email"));
                customer.setCompanyName(rs.getString("company_name"));
                break;
            }
            ps.close();
            connection.close();
        } catch (Exception e) {
            System.out.println("Error getting customer: " + e.getMessage());
            e.printStackTrace();
        }
        return customer;
    }
    
    public void insertCustomer(Customer customer){
    	try{
            Connection connection = MySQL.getConnection();
            PreparedStatement ps = connection.prepareStatement("INSERT INTO " + tableName + " (name, password, phone, email, company_name, created_date, status) " +
            													"VALUES( " + "?, ?, ?, ?, ?, ?, ?)");
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getPassword());
            ps.setString(3, customer.getPhone());
            ps.setString(4, customer.getEmail());
            ps.setString(5, customer.getCompanyName());
            ps.setTimestamp(6, customer.getCreatedDate());
            ps.setInt(7, customer.getStatus());
            
            ps.execute();
            ps.close();
            connection.close();

        }catch (Exception e){
            System.out.println("Error inserting customer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
