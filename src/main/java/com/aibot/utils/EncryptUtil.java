package com.aibot.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.UUID;

import org.apache.commons.net.util.Base64;

public class EncryptUtil {
	
	private static final String HASH_PREP = "HASHED";
    private static final String HASH_APPE = "/HASHED";

	public static String generateApplicationId() {
		SecureRandom random = new SecureRandom();
		long aid = random.nextLong();
		if (aid < 0) {
			aid = aid*-1;
		}
		return String.valueOf(aid);
	}
	
	public static String generateToken(String applicationId) {
		String token = applicationId + "-" + UUID.randomUUID().toString();
		return encrypt(token);
	}
	
	public static String getApplicationIdByToken(String token) {
		if (token == null) {
			return "";
		}
		token = decrypt(token);
		return token.substring(0, token.indexOf("-"));
	}
	
	public static String encrypt(String plainText) {  
		byte[] encryptArray = Base64.encodeBase64(plainText.getBytes());
		try {
			return new String(encryptArray, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
		return "";
	}
	
	public static String decrypt(String encryptedText) {
		byte[] dectryptArray = encryptedText.getBytes();
		byte[] decarray = Base64.decodeBase64(dectryptArray);
		return new String(decarray);
	}
	
	public static String base64(String input) {
        Base64 b64e = new Base64();
        byte[] valueText = b64e.encode(input.getBytes());
        return new String(valueText);
    }
	
	public static String base64Decode(String input) {
        Base64 b64e = new Base64();
        byte[] valueText = b64e.decode(input.getBytes());

        return new String(valueText);
    }
	
	public static String hash(String input) {
        try {
            if (StringUtils.isEmpty(input)) {
                return input;
            }
            Base64 b64e = new Base64();
            MessageDigest md = MessageDigest.getInstance("SHA-256", "SUN");
            //System.out.println(md.getProvider().getName());
            md.update(input.getBytes());
            byte[] output = b64e.encode(md.digest());
            return HASH_PREP + new String(output).trim() + HASH_APPE;
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchProviderException ex) {
            throw new RuntimeException(ex);
        }

    }
}
