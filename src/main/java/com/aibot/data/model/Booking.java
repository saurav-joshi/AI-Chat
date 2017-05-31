package com.aibot.data.model;

import javax.persistence.*;

@Entity
@Table(name = "booking")
public class Booking{
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    @Column(name = "booking_id")
    private long bookingId;
    @Column(name = "qa_id")
    private long qaId;
    @Column(name = "restaurant_id")
    private long restaurantId;
    @Column(name = "user_id")
    private String userId;
    private String email;
    private String phone;
    private String date;
    @Transient
    private String time;
    @Column(name = "special")
    private String specialRequest;
    private String pax;
    private int confirm;
    @Transient
    public String getTime() {
        return time;
    }
    @Transient
    public void setTime(String time) {
        this.time = time;
    }

    public String getSpecialRequest() {
        return specialRequest;
    }

    public void setSpecialRequest(String specialRequest) {
        this.specialRequest = specialRequest;
    }

    public long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getPax() {
        return pax;
    }

    public void setPax(String pax) {
        this.pax = pax;
    }

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public long getQaId() {
        return qaId;
    }

    public void setQaId(long qaId) {
        this.qaId = qaId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getConfirm() {
        return confirm;
    }

    public void setConfirm(int confirm) {
        this.confirm = confirm;
    }

}

