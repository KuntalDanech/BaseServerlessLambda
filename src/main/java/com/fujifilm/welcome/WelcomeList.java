package com.fujifilm.welcome;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fujifilm.base.APIAbstractService;
import com.fujifilm.exception.BusinessException;

public class WelcomeList extends APIAbstractService {

	@Override
	protected Map<String, Object> handle(APIGatewayProxyRequestEvent input, Context context, String token)
			throws BusinessException {
		String body = input.getBody();
		// This is your body
		// Parse the Body into Specific DTO
		// Utilize the Multi-threaded code here
		String method = input.getHttpMethod();
		log.info("HTTP Method : "+method);
		Map<String, Object> response = new HashMap<>();
		switch (method) {
		case "GET":
			welcomeMessage(body, response);
			break;
		case "POST":
			welcomeMessagePost(body, response);
			break;
		}
		return response;
	}

	private void welcomeMessage(String body, Map<String, Object> response) {
		// Open a new/existing JDBC connection and insert data into DB.
		// Throw specific Exception if needed.
		log.info("welcome info");
		response.put("message", "Welcome to AWS Lambda serverless - GET");
	}

	private void welcomeMessagePost(String body, Map<String, Object> response) {
		// Open a new/existing JDBC connection and insert data into DB.
		// Throw specific Exception if needed.
		log.info("welcome info");
		response.put("message", "Welcome to AWS Lambda serverless - POST");
	}
}