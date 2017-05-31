package com.aibot.controller;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.aibot.data.model.Customer;
import com.aibot.data.model.CustomerApp;
import com.aibot.data.repo.CustomerAppRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.aibot.data.repo.CustomerQueryRepo;
import com.aibot.data.repo.CustomerRepo;
import com.aibot.entity.CustomerAppRep;


@RestController
public class CustomerAppController extends BaseController {

	@Autowired
    CustomerAppRepo customerAppRepo;
	@Autowired
    CustomerRepo customerRepo;
	@Autowired
    CustomerQueryRepo customerQueryRepo;
	
	@RequestMapping(value = "/customer/app/{customerId}", method = RequestMethod.GET)
    public Object findAppByCustomer(@PathVariable("customerId") long customerId, HttpServletResponse res) {
    	res.setStatus(HttpServletResponse.SC_OK);
        return customerAppRepo.findByCustomerId(customerId);
    }
	
	@RequestMapping(value = "/customer/app/{customerId}/{id}", method = RequestMethod.GET)
    public Object findAppByCustomerAndId(@PathVariable("customerId") long customerId, @PathVariable("id") long id, HttpServletResponse res) {
    	res.setStatus(HttpServletResponse.SC_OK);
        return customerAppRepo.findByCustomerIdAndId(customerId, id);
    }
	
	@RequestMapping(value = "/customer/app", method = RequestMethod.POST)
    public Object createCustomerApp(@RequestBody CustomerAppRep custAppReq, HttpServletResponse res) {
		CustomerApp cusApp = new CustomerApp();
    	cusApp.setApplicationId(custAppReq.getApplicationId());
    	cusApp.setApplicationName(custAppReq.getApplicationName());
    	cusApp.setCreatedDate(new Timestamp(System.currentTimeMillis()));
    	cusApp.setCustomer(customerRepo.findOne(custAppReq.getCustomerId()));
    	cusApp.setRequestLimited(custAppReq.getRequestLimited());
    	
    	customerAppRepo.save(cusApp);
    	res.setStatus(HttpServletResponse.SC_OK);
        return getRespSess("Customer app created successful!");
    }
	
	@RequestMapping(value = "/customer/app", method = RequestMethod.PUT)
    public Object updateCustomer(@RequestBody CustomerAppRep custAppReq, HttpServletResponse res) {
		CustomerApp cusApp = customerAppRepo.findOne(custAppReq.getId());
    	if (cusApp == null || cusApp.getId() <= 0) {
    		res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return getRespErr("Customer app does not exist!");
    	}
    	cusApp.setApplicationName(custAppReq.getApplicationName());
    	cusApp.setRequestLimited(custAppReq.getRequestLimited());
    	customerAppRepo.save(cusApp);
    	
    	res.setStatus(HttpServletResponse.SC_OK);
        return cusApp;
    }
	
	@RequestMapping(value = "/customer/app/query/{applicationId}", method = RequestMethod.GET)
	public Object getUsagedByApplication(@PathVariable("applicationId") String applicationId, HttpServletResponse res) {
		List<Object> lstUsaged = customerQueryRepo.findUsagedByApplicationId(applicationId);
		List<Object> lstStatus = customerQueryRepo.findStatusByApplicationId(applicationId);
		List<Map<String, List<Map<String, Object>>>> lstRet = new ArrayList<>();
		
		Map<String, List<Map<String, Object>>> mapUsaged = new HashMap<>();
		List<Map<String, Object>> lstUsagedData = new ArrayList<>();
		if (lstUsaged != null && lstUsaged.size() > 0) {
			for (Object obj: lstUsaged) {
				Map<String, Object> map = new HashMap<>();
				map.put("date", ((Object[])obj)[0]);
				map.put("total", ((Object[])obj)[1]);
				lstUsagedData.add(map);
			}
		}
		mapUsaged.put("usaged", lstUsagedData);
		lstRet.add(mapUsaged);
		
		Map<String, List<Map<String, Object>>> mapStatus = new HashMap<>();
		List<Map<String, Object>> lstStatusData = new ArrayList<>();
		if (lstStatus != null && lstStatus.size() > 0) {
			for (Object obj: lstStatus) {
				Map<String, Object> map = new HashMap<>();
				map.put("is_success", ((Object[])obj)[0]);
				map.put("total", ((Object[])obj)[1]);
				lstStatusData.add(map);
			}
		}
		mapStatus.put("status", lstStatusData);
		lstRet.add(mapStatus);
		
		res.setStatus(HttpServletResponse.SC_OK);
        return lstRet;
    }
	
	@RequestMapping(value="/customer/app/{id}", method=RequestMethod.DELETE)
	public void deleteApplication(@PathVariable("id") Long id, HttpServletResponse res) {
		try {
			customerAppRepo.delete(id);
			res.setStatus(HttpServletResponse.SC_OK);
		} catch(Exception ex) {
			res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		}
	}
	
	@RequestMapping(value = "/customer/member/query/{customerId}", method = RequestMethod.GET)
	public Object getUsagedByCustomer(@PathVariable("customerId") Long customerId, HttpServletResponse res) {
		List<Object> lstUsaged = customerQueryRepo.findUsagedByCustomerId(customerId);
		List<Map<String, Object>> lstRet = new ArrayList<>();
		if (lstUsaged != null && lstUsaged.size() > 0) {
			for (Object obj: lstUsaged) {
				Map<String, Object> map = new HashMap<>();
				map.put("application_name", ((Object[])obj)[0]);
				map.put("total", ((Object[])obj)[1]);
				lstRet.add(map);
			}
		}
		
		res.setStatus(HttpServletResponse.SC_OK);
        return lstRet;
    }
	
	@RequestMapping(value = "/customer/admin/member/query/{parentId}", method = RequestMethod.GET)
	public Object getUsagedByCustomerForAdmin(@PathVariable("parentId") Long parentId, HttpServletResponse res) {
		List<Customer> lstCustomer = customerRepo.findByParentId(parentId);
		if (lstCustomer == null) {
			lstCustomer = new ArrayList<>();
		}
		lstCustomer.add(customerRepo.findOne(parentId));
		
		List<Map<String, Object>> lstRet = new ArrayList<>();
		for (Customer cus: lstCustomer) {
			List<Object> lstUsaged = customerQueryRepo.findUsagedByCustomerId(cus.getId());
			List<Map<String, Object>> lstMember = new ArrayList<>();
			if (lstUsaged != null && lstUsaged.size() > 0) {
				for (Object obj: lstUsaged) {
					Map<String, Object> map = new HashMap<>();
					map.put("application_name", ((Object[])obj)[0]);
					map.put("total", ((Object[])obj)[1]);
					lstMember.add(map);
				}
			}
			Map<String, Object> map = new HashMap<>();
			map.put("customer_id", cus.getId());
			map.put("email", cus.getEmail());
			map.put("data", lstMember);
			lstRet.add(map);
		}
		res.setStatus(HttpServletResponse.SC_OK);
        return lstRet;
    }
}

