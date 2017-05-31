package com.aibot.controller;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.aibot.data.model.Customer;
import com.aibot.entity.EMailReq;
import com.aibot.utils.EncryptUtil;
import com.aibot.data.repo.CustomerRepo;
import com.aibot.utils.EmailUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.aibot.utils.EMail;


@RestController
public class EmailController extends BaseController {

	@Autowired
    CustomerRepo customerRepo;
	private static final String DEFAULT_PASSWORD = "th@nks";
	
    @RequestMapping(value = "/invite", method = RequestMethod.POST)
    public Object invite(@RequestBody EMailReq emailReq, HttpServletResponse res) {
    	EMail email = new EMail();
    	email.fromAddress = "support@tastebots.com";
    	email.toAddresses = emailReq.getCustomerName()+"<"+emailReq.customerEmail+">";
    	email.subject = "Welcome to #Maya Tastebot APIs.";
    	email.doNotRedirect = true;
    	StringBuffer body = new StringBuffer();
    	body.append("<div>Dear CUSTOMER_NAME,</div>");
    	body.append("<br>");
    	body.append("<div>Here is your login to Maya Tastebot APIs page.</div>");
    	body.append("<br>");
    	body.append("<div>Login Account: CUSTOMER_EMAIL</div>");
    	body.append("<div>Password: DEFAULT_PASSWORD</div>");
    	body.append("<div>Maya Tastebot APIs webstie: <a href=\"http://maya.tastebots.com\">http://maya.tastebots.com</a></div>");
    	body.append("<br>");
    	body.append("<div>Have an awesome time exploring our APIs and building your chatbot!</div>");
    	body.append("<br>");
    	body.append("<div>Cheers,</div>");
    	body.append("<div>Maya Tastbots</div>");
    	email.body = body.toString().replaceAll("CUSTOMER_NAME", emailReq.getCustomerName()).replaceAll("CUSTOMER_EMAIL", emailReq.customerEmail).replaceAll("DEFAULT_PASSWORD", DEFAULT_PASSWORD);
    	try {
			EmailUtils.sendMail(email);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	Map<String, String> resp = new HashMap<>();
        resp.put("success", "success");
        res.setStatus(HttpServletResponse.SC_OK);
        return resp;
    }
    
    @RequestMapping(value = "/request", method = RequestMethod.POST)
    public Object requestAccount(@RequestBody EMailReq emailReq, HttpServletResponse res) {
    	EMail email = new EMail();
    	email.fromAddress = "support@tastebots.com";
    	email.toAddresses = emailReq.getCustomerName()+"<"+emailReq.customerEmail+">";
    	email.bcc = "Anissa Wong <anissa@crayondata.com>";
    	email.subject = "Your request to access Maya Tastebot APIs";
    	email.doNotRedirect = true;
    	StringBuffer body = new StringBuffer();
    	body.append("<div>Dear CUSTOMER_NAME,</div>");
    	body.append("<br>");
    	body.append("<div>Thanks for your interest in Maya Tastebot APIs!</div>");
    	body.append("<br>");
    	body.append("<div>Your access request is being processed.<br>We will send you your login details shortly.</div>");
    	body.append("<br>");
    	body.append("<div>Have a great day.</div>");
    	body.append("<br>");
    	body.append("<div>Best regards,</div>");
    	body.append("<div>Anissa</div>");
    	email.body = body.toString().replaceAll("CUSTOMER_NAME", emailReq.getCustomerName());
    	try {
    		Customer customer = new Customer();
    		customer.setCompanyName(emailReq.getCustomerCompany());
    		customer.setCreatedDate(new Timestamp(Calendar.getInstance().getTimeInMillis()));
    		customer.setEmail(emailReq.getCustomerEmail());
    		customer.setName(emailReq.getCustomerName());
    		customer.setPassword(EncryptUtil.hash(DEFAULT_PASSWORD));
    		customer.setPhone("");
    		customer.setStatus(0);
    		//CustomerDao customerDao = new CustomerDao();
    		//customerDao.insertCustomer(customer);
			customerRepo.save(customer);
    		
			EmailUtils.sendMail(email);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	Map<String, String> resp = new HashMap<>();
        resp.put("success", "success");
        res.setStatus(HttpServletResponse.SC_OK);
        return resp;
    }
}

