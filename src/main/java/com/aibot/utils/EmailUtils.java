package com.aibot.utils;

import java.util.Date;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message.RecipientType;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtils {
	
	public static void sendMail(EMail email) throws Exception {
		sendMail(email, getMailSession());
	}

	/**
	 * Get default mail session
	 * @param host
	 * @return
	 * @throws Exception
	 */
	public static Session getMailSession() throws Exception {
		String host = "smtp.gmail.com";
		String userPwd = "support@tastebots.com/Cray@321$";
		
		String user = userPwd.substring(0, userPwd.indexOf("/"));
		String pwd = userPwd.substring(userPwd.indexOf("/")+1);
		return getMailSession(host, user, pwd);
		
	}
	
	/**
	 * Send an email
	 * 
	 * @param email
	 * @param session
	 * @return
	 */

	public static boolean sendMail(EMail email, Session session) throws Exception {
		MimeMessage msg = new MimeMessage(session);
		try {
			if (!email.doNotRedirect) {
				return false;
			}
			msg.setSubject(email.subject);
			addFromAddress(msg, email.fromAddress);// just one address expected
			addToAdresses(msg, email.toAddresses, 1);

			if (!StringUtils.isEmpty(email.cc)) {
				addToAdresses(msg, email.cc, 2);
			}
			if (!StringUtils.isEmpty(email.bcc)) {
				addToAdresses(msg, email.bcc, 3);
			}
			if (!email.hasEmbed()) { // email does not contain embed data
				msg.setContent(email.body, "text/html; charset=utf8");
			}
			msg.setSentDate(new Date());
			Transport transport = session.getTransport("smtp");
			transport.send(msg);

		} catch (Exception e) {
			throw e;
		}
		return true;

	}

	/**
	 * 
	 * @param msg
	 * @param addressStr
	 * @throws Exception
	 */
	private static void addFromAddress(MimeMessage msg, String addressStr) throws Exception {
		InternetAddress[] addresses = new InternetAddress[1];
		addresses[0] = new InternetAddress(getEmailAdress(addressStr), getDisplayName(addressStr));
		msg.addFrom(addresses);

	}

	/**
	 * Add the to adress
	 * 
	 * @param msg
	 * @param address
	 */
	private static void addToAdresses(MimeMessage msg, String addressStr, int intType) throws Exception {
		addressStr = addressStr.replaceAll(";", "|");
		addressStr = addressStr.replaceAll(",", "|");
		String[] addresses = addressStr.split("\\|");
		String oneAddress;
		InternetAddress internetAddress;

		RecipientType type = RecipientType.TO;
		if (intType == 2) {
			type = RecipientType.CC;
		} else if (intType == 3) {
			type = RecipientType.BCC;
		}
		for (int i = 0; i < addresses.length; i++) {
			oneAddress = addresses[i];
			if (!StringUtils.isEmpty(oneAddress)) {
				internetAddress = new InternetAddress(getEmailAdress(oneAddress), getDisplayName(oneAddress)); msg.addRecipient(type, internetAddress);
			}
		}
	}

	/**
	 * Get the display name
	 * 
	 * @param address
	 * @return
	 */
	private static String getDisplayName(String address) {
		if (StringUtils.isEmpty(address)) {
			return "";
		}
		if (address.indexOf("\"") >= 0 && address.indexOf("<") >= 0) {
			String displayName = address.substring(0, address.indexOf("<"));
			return displayName.replaceAll("\"", "");
		}
		return "";
	}

	/**
	 * 
	 * @param address
	 * @return
	 */
	private static String getEmailAdress(String address) {
		if (StringUtils.isEmpty(address)) {
			return "";
		}
		if (address.indexOf("<") >= 0) {
			String email = address.substring(address.indexOf("<") + 1,
					address.indexOf(">"));
			return email;
		}
		return address;
	}

	/**
	 * Get mail session with authentication
	 * 
	 * @param host
	 * @param userName
	 * @param password
	 * @return
	 * @throws Exception
	 */
	public static Session getMailSession(String host, String userName, String password) throws Exception {
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", host);
		props.put("mail.smtp.port", "587");
		return Session.getInstance(props, new SMTPAuthenticator(userName, password));
	}
}

class SMTPAuthenticator extends Authenticator {

	private String username;
	private String password;

	public SMTPAuthenticator(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public PasswordAuthentication getPasswordAuthentication() {
		sun.misc.BASE64Encoder enc = new sun.misc.BASE64Encoder();
		return new PasswordAuthentication(username, password);
	}
}
