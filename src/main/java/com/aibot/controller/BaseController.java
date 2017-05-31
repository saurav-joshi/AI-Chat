package com.aibot.controller;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import com.aibot.data.model.CustomerApp;
import com.aibot.data.repo.CustomerAppRepo;
import com.aibot.utils.EncryptUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import com.aibot.data.model.CustomerQuery;
import com.aibot.data.repo.CustomerQueryRepo;


@RestController
public class BaseController {
	@Autowired
    private CustomerQueryRepo customerQueryRepo;
	@Autowired
    private CustomerAppRepo customerAppRepo;
	
	protected Map<String, Object> getRespErr(String msg) {
		Map<String, Object> resp = new HashMap<>();
		resp.put("error", msg);
		resp.put("status", false);
		return resp;
    }
	
	protected Map<String, Object> getRespSess(String msg) {
		Map<String, Object> resp = new HashMap<>();
		resp.put("success", msg);
		resp.put("status", true);
		return resp;
    }
	
	protected void trackingRequest(String token, boolean isSuccess) {
		// track query
    	CustomerQuery cQuery = new CustomerQuery();
    	String appId = EncryptUtil.getApplicationIdByToken(token);
    	cQuery.setApplicationId(appId);
    	cQuery.setIsSuccess(isSuccess);
    	cQuery.setQueriedDate(new Timestamp(System.currentTimeMillis()));
    	cQuery.setToken(token);
    	customerQueryRepo.save(cQuery);
    	
    	// increase counter
    	CustomerApp cApp = customerAppRepo.findByApplicationId(appId);
    	cApp.setRequestCounted(cApp.getRequestCounted()+1);
    	customerAppRepo.save(cApp);
    }

	protected boolean isRequestLimited(String token) {
		String appId = EncryptUtil.getApplicationIdByToken(token);
		CustomerApp cApp = customerAppRepo.findByApplicationId(appId);
		if (cApp.getRequestLimited() != -1 && cApp.getRequestCounted() >= cApp.getRequestLimited()) {
			return true;
		}
		return false;
	}
}

