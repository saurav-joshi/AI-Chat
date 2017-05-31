package com.aibot.data.repo;

import java.util.List;

import com.aibot.data.model.CustomerQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CustomerQueryRepo extends JpaRepository<CustomerQuery, Long> {
	List<CustomerQuery> findByApplicationId(String applicationId);
	@Query(value="SELECT CAST(queried_date AS DATE) AS qdate, COUNT(id) AS total  FROM customer_query WHERE application_id=?1 GROUP BY CAST(queried_date AS DATE) ORDER BY queried_date ASC", nativeQuery=true)
	List<Object> findUsagedByApplicationId(String applicationId);
	@Query(value="SELECT is_success, COUNT(id) AS total FROM customer_query WHERE application_id=?1 GROUP BY is_success", nativeQuery=true)
	List<Object> findStatusByApplicationId(String applicationId);
	@Query(value="SELECT ca.application_name, COUNT(cq.id) AS total FROM customer_app ca LEFT JOIN customer_query cq ON ca.application_id = cq.application_id WHERE ca.customer_id=?1 GROUP BY ca.application_id", nativeQuery=true)
	List<Object> findUsagedByCustomerId(Long customerId);
}