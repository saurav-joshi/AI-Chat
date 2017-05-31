package com.aibot.integration.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.aibot.entity.UserMessage;
import com.aibot.Application;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.clearspring.analytics.util.Lists;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { Application.class })
@WebAppConfiguration
@IntegrationTest("server.port:9093")
public class TestIT {
	RestTemplate rest = new TestRestTemplate();
	final String LATEST_URL = "http://localhost:9093/query/";
	final String RELEASE_URL = "http://release-20161223.us-east-1.elasticbeanstalk.com:8080/query";

	@Test
	public void test() throws IOException {
		AmazonS3 s3Client = new AmazonS3Client(new InstanceProfileCredentialsProvider());
		String testFile = "test_cases.csv";
		String localSampleFile = System.getProperty("java.io.tmpdir") + File.separator + testFile;
		s3Client.getObject(new GetObjectRequest("tastebot-integration-test", testFile), new File(localSampleFile));
		// resourceService.downloadResource(localSampleFile,"s3://tastebot-integration-test/sample.csv");
		CSVParser csvParser = CSVParser.parse(new File(localSampleFile), Charset.defaultCharset(), CSVFormat.RFC4180);
		List<String> failTestCases = Lists.newArrayList();
		int totalTestCase = 0;
		for (CSVRecord record : csvParser.getRecords()) {
			totalTestCase++;
			String id = record.get(0);
			String question = record.get(1);
			String expectedResult = record.get(2);
			String expectedMessage = record.get(3);
			String latitude = record.get(4);
			String longitude = record.get(5);
			String user = record.get(6);
			String token = record.get(7);
			String[] envs = record.get(8).split(",");
			UserMessage userMessage = new UserMessage();
			userMessage.setQuestion(question);
			userMessage.setType("QUERY");
			userMessage.setToken(token);
			if (latitude != null && !latitude.trim().equals(""))
				userMessage.setLatitude(latitude);
			if (longitude != null && !longitude.trim().equals(""))
				userMessage.setLongitude(longitude);
			for (String env : envs) {
				ResponseEntity<String> output = null;
				if (env.equalsIgnoreCase("release"))
					output = rest.postForEntity(RELEASE_URL, userMessage, String.class);
				else
					output = rest.postForEntity(LATEST_URL, userMessage, String.class);
			
				if (output == null || output.getBody() == null) {
					String errMessage = buildErrMessage(id, question, expectedResult, env, "NULL RESPONSE");
					failTestCases.add(errMessage);
					continue;
				}
				JSONObject json = new JSONObject(output.getBody());
				if (expectedResult != null && !expectedResult.trim().equals("")) {

					Set<String> expectedRestaurant = Sets.newHashSet();
					String[] restaurants = expectedResult.split("\\|\\|");
					for (String restaurant : restaurants) {
						expectedRestaurant.add(question + "-" + restaurant.toLowerCase().trim());
					}
					if (json.isNull("resultRestaurants") || json.get("resultRestaurants") == null
							|| json.get("resultRestaurants").equals(null)) {
						String errMessage = buildErrMessage(id, question, expectedResult, env, "NULL");
						failTestCases.add(errMessage);
						// System.err.println(errMessage.toString());
					} else {
						JSONArray array = (JSONArray) json.get("resultRestaurants");
						Iterator<Object> ite = array.iterator();
						List<String> restaurantNames=Lists.newArrayList();
						boolean isError=false;
						while (ite.hasNext()) {
							JSONObject jsonRestaurant = (JSONObject) ite.next();
							String restaurantName = jsonRestaurant.get("name").toString();
							restaurantNames.add(restaurantName);
							if (!isError && !expectedRestaurant.contains(question + "-" + restaurantName.toLowerCase().trim())) {
								//String errMessage = buildErrMessage(id, question, expectedResult, env, restaurantName);
								//failTestCases.add(errMessage);
								isError=true;
							}
						}
						if(isError){
							String errMessage = buildErrMessage(id, question, expectedResult, env, Joiner.on("||").join(restaurantNames));
							failTestCases.add(errMessage);
						}
					}
				} else {
					if(!json.isNull("resultRestaurants")){
						String errMessage = buildErrMessage(id, question, "NULL", env, "NOT NULL");
						failTestCases.add(errMessage);
					}
				}
				if (expectedMessage != null && !expectedMessage.trim().equals("")) {
					if (json.isNull("message")) {
						String errMessage = buildErrMessage(id, question, expectedMessage, env, "NULL MESSAGE");
						failTestCases.add(errMessage);
					} else {
						String message = json.get("message").toString();
						if (!message.trim().equalsIgnoreCase(expectedMessage.trim())) {
							String errMessage = buildErrMessage(id, question, expectedMessage, env, message);
							failTestCases.add(errMessage);
						}
					}
				}
			}
		}
		System.out.println("Tastebot Test: Total of test cases " + totalTestCase);
		System.out.println("Tastebot Test: Number of failed test cases " + failTestCases.size());
		if (failTestCases.size() > 0) {
			System.out.println("Tastebot Test: Detail of failed test cases:");
			System.out.println("Tastebot Test: id,	question,	enviroment,	expect_result,	actual_result");
			for (String errMessage : failTestCases)
				System.out.println(errMessage);
			throw new AssertionError();
		}

	}

	private String buildErrMessage(String id, String question, String expectedMessage, String env, String message) {
		StringBuffer errMessage = new StringBuffer();
		errMessage.append("Tastebot Test: ");
		errMessage.append(id);
		errMessage.append(", \"");
		errMessage.append(question);
		errMessage.append("\", ");
		errMessage.append(env);
		errMessage.append(", Expect: \"");
		errMessage.append(expectedMessage);
		errMessage.append("\", Actual:\"");
		errMessage.append(message);
		errMessage.append("\"");
		return errMessage.toString();
	}
}
