package com.websystique.springboot.libs;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Defines an API Gateway Response in accordance with the format defined at
 * https://docs.aws.amazon.com/apigateway/latest/developerguide/set-up-lambda-proxy-integrations.html#api-gateway-simple-proxy-for-lambda-input-format
 */
public class ApiGatewayResponse {

	private static final ObjectMapper mapper = new ObjectMapper();

	private final int statusCode;
	private final String body;
	private final Map<String, String> headers;
	private final boolean isBase64Encoded;

	public ApiGatewayResponse(int statusCode, String body, Map<String, String> headers, boolean isBase64Encoded) {
		this.statusCode = statusCode;
		this.body = body;
		this.isBase64Encoded = isBase64Encoded;
		this.headers = headers;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getBody() {
		return body;
	}

	public <T> T getObjectBody(Class<T> clazz) throws JsonParseException, JsonMappingException, IOException {

		T result = mapper.readValue(body, clazz);

		return result;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	// API Gateway expects the property to be called "isBase64Encoded" => isIs
	public boolean isIsBase64Encoded() {
		return isBase64Encoded;
	}

	/*public ApiGatewayResponseBuilder builder() {
		return new ApiGatewayResponseBuilder("*");
	}*/
}


