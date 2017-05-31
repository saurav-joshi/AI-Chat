package com.aibot.data.repo;

import java.util.List;

import com.aibot.data.model.CustomerApp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAppRepo extends JpaRepository<CustomerApp, Long> {
    //@Query("SELECT c FROM customer c JOIN customer_app ca ON c.id = ca.customer_id WHERE ca.application_id = ?1)
    CustomerApp findByApplicationId(String applicationId);
    List<CustomerApp> findByCustomerId(long customerId);
    CustomerApp findByCustomerIdAndId(long customerId, long id);
}