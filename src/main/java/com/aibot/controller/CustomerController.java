package com.aibot.controller;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.aibot.data.model.Customer;
import com.aibot.data.repo.CustomerAppRepo;
import com.aibot.data.repo.CustomerRepo;
import com.aibot.entity.CustomerRep;
import com.aibot.utils.EncryptUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerController extends BaseController {

	@Autowired
    CustomerRepo customerRepo;
	@Autowired
    private CustomerAppRepo customerAppRepo;
	
    @RequestMapping(value = "/customer", method = RequestMethod.POST)
    public Object createCustomer(@RequestBody CustomerRep custReq, HttpServletResponse res) {
    	// check email exist
    	Customer customer = customerRepo.findByEmail(custReq.getEmail());
    	if (customer != null && customer.getId() > 0) {
    		res.setStatus(HttpServletResponse.SC_CONFLICT);
            return getRespErr("Customer already existed!");
    	}
    	customer = new Customer();
    	customer.setCompanyName(custReq.getCompanyName());
    	customer.setContractEnded(new Timestamp(custReq.getContractEnded()));
    	customer.setContractStarted(new Timestamp(custReq.getContractStarted()));
    	customer.setEmail(custReq.getEmail());
    	customer.setName(custReq.getName());
    	customer.setParentId(custReq.getParentId());
    	customer.setPassword(EncryptUtil.hash(custReq.getPassword()));
    	customer.setPhone(custReq.getPhone());
    	customer.setIsAdmin(custReq.getIsAdmin());
    	customer.setIsSystemAdmin(custReq.getIsSystemAdmin());
    	customer.setRequestLimited(custReq.getRequestLimited());
    	customer.setCreatedDate(new Timestamp(System.currentTimeMillis()));
    	customerRepo.save(customer);
    	
    	res.setStatus(HttpServletResponse.SC_OK);
        return getRespSess("Customer created successful!");
    }
    
    @RequestMapping(value = "/customer", method = RequestMethod.PUT)
    public Object updateCustomer(@RequestBody CustomerRep custReq, HttpServletResponse res) {
    	Customer customer = customerRepo.findOne(Long.valueOf(custReq.getId()));
    	if (customer == null || customer.getId() <= 0) {
    		res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return getRespErr("Customer does not exist!");
    	}
    	customer.setCompanyName(custReq.getCompanyName());
    	customer.setContractEnded(new Timestamp(custReq.getContractEnded()));
    	customer.setContractStarted(new Timestamp(custReq.getContractStarted()));
    	customer.setName(custReq.getName());
    	customer.setPhone(custReq.getPhone());
    	customer.setIsAdmin(custReq.getIsAdmin());
    	customer.setIsSystemAdmin(custReq.getIsSystemAdmin());
    	customer.setRequestLimited(custReq.getRequestLimited());
    	if (custReq.getPassword() != null && custReq.getPassword() != "") {
    		customer.setPassword(EncryptUtil.hash(custReq.getPassword()));
    	}
    	customerRepo.save(customer);
    	
    	res.setStatus(HttpServletResponse.SC_OK);
        return customer;
    }
    
    @RequestMapping(value = "/customer", method = RequestMethod.GET)
    public Object findAllCustomers(HttpServletResponse res) {
    	res.setStatus(HttpServletResponse.SC_OK);
        return customerRepo.findByParentId(0);
    }
    
    @RequestMapping(value = "/customer/parent/{parentId}", method = RequestMethod.GET)
    public Object findCustomersByParent(@PathVariable("parentId") long parentId, HttpServletResponse res) {
    	res.setStatus(HttpServletResponse.SC_OK);
    	List<Customer> lstCustomer = customerRepo.findByParentId(parentId);
    	if (lstCustomer == null) {
    		lstCustomer = new ArrayList<Customer>();
    	}
        return lstCustomer;
    }
    
    @RequestMapping(value = "/customer/{id}", method = RequestMethod.GET)
    public Object findCustomer(@PathVariable("id") long id, HttpServletResponse res) {
    	res.setStatus(HttpServletResponse.SC_OK);
        return customerRepo.findOne(Long.valueOf(id));
    }
    
    @RequestMapping(value = "/customer/login", method = RequestMethod.POST)
    public Object customerLogin(@RequestBody CustomerRep custReq, HttpServletResponse res) {
    	Customer customer = customerRepo.findByEmailAndPassword(custReq.getEmail(), EncryptUtil.hash(custReq.getPassword()));
    	if (customer == null || customer.getId() <= 0) {
    		res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return getRespErr("Customer does not exist!");
    	}
    	res.setStatus(HttpServletResponse.SC_OK);
        return customer;
    }
    
    @RequestMapping(value="/customer/{id}", method=RequestMethod.DELETE)
    public void deleteCustomer(@PathVariable("id") Long id, HttpServletResponse res) {
    	try {
    		Customer customer = customerRepo.findOne(id);
        	if (customer.getParentId() == 0) {
        		// delete all accounts under and delete all apps
        		List<Customer> lstCustomer = customerRepo.findByParentId(customer.getId());
        		for (Customer cus: lstCustomer) {
        			customerAppRepo.delete(customerAppRepo.findByCustomerId(cus.getId()));
        			customerRepo.delete(cus);
        		}
        	}
        	customerAppRepo.delete(customerAppRepo.findByCustomerId(customer.getId()));
        	customerRepo.delete(customer);
        	res.setStatus(HttpServletResponse.SC_OK);
    	} catch(Exception ex) {
    		res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    	}
    }
}

