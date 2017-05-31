package com.aibot.data.repo;

import java.util.List;

import com.aibot.data.model.Customer;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface CustomerRepo extends PagingAndSortingRepository<Customer, Long> {
	public Customer findByEmail(String email);
	public Customer findByEmailAndPassword(String email, String password);
	public List<Customer> findByParentId(long parentId);
}
