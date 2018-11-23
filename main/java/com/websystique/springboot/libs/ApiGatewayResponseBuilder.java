package com.websystique.springboot.libs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ApiGatewayResponseBuilder {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private HashMap<String, String> corsHeaders;

	private static HashMap<String, String> securityHeaders;

	static {
		securityHeaders = new HashMap<>();
		securityHeaders.put("Strict-Transport-Security", "max-age=2592000; includeSubdomains; preload");
		securityHeaders.put("X-Content-Type-Options", "nosniff");
		securityHeaders.put("X-Frame-Options", "SAMEORIGIN");
		securityHeaders.put("X-XSS-Protection", "1; mode=block");
		securityHeaders.put("Referrer-Policy", "strict-origin-when-cross-origin");
		securityHeaders.put("Cache-Control", "no-cache,no-store"); // Prevent caching for HTTP 1.1+ clients
		securityHeaders.put("Pragma", "no-cache"); // Prevent caching for (some) HTTP 1.0 clients - Usage on response header is undefined and implementation-specific
		securityHeaders.put("Expires", "0"); // Used in conjunction with Pragma no-cache
	}

	private int statusCode = 200;
	private Map<String, String> headers = Collections.emptyMap();
	private String rawBody;
	private Object objectBody;
	private byte[] binaryBody;
	private boolean base64Encoded;
	
	public ApiGatewayResponseBuilder(String allowedOrigin) {
		
		corsHeaders = new HashMap<>();
		corsHeaders.put("Access-Control-Allow-Origin", allowedOrigin);
		corsHeaders.put("Access-Control-Allow-Credentials", "true");
		corsHeaders.put("Vary", "Origin");

	}

	public ApiGatewayResponseBuilder setStatusCode(int statusCode) {
		this.statusCode = statusCode;
		return this;
	}

	public ApiGatewayResponseBuilder addHeaders(Map<String, String> headers) {
		HashMap<String, String> newHeaders = new HashMap<String,String>();
		newHeaders.putAll(this.headers);
		newHeaders.putAll(headers);
		this.headers = newHeaders;
		return this;
	}

	/**
	 * Builds the {@link ApiGatewayResponse} using the passed raw body string.
	 */
	public ApiGatewayResponseBuilder setRawBody(String rawBody) {
		this.rawBody = rawBody;
		return this;
	}

	/**
	 * Builds the {@link ApiGatewayResponse} using the passed object body
	 * converted to JSON.
	 */
	public ApiGatewayResponseBuilder setObjectBody(Object objectBody) {
		this.objectBody = objectBody;
		return this;
	}

	/**
	 * Builds the {@link ApiGatewayResponse} using the passed binary body
	 * encoded as base64. {@link #setBase64Encoded(boolean)
	 * setBase64Encoded(true)} will be in invoked automatically.
	 */
	public ApiGatewayResponseBuilder setBinaryBody(byte[] binaryBody) {
		this.binaryBody = binaryBody;
		setBase64Encoded(true);
		return this;
	}

	/**
	 * Adds default CORS access-control-allow-origin headers to the response
	 * </ol>
	 */
	public ApiGatewayResponseBuilder setBase64Encoded(boolean base64Encoded) {
		this.base64Encoded = base64Encoded;
		return this;
	}

	/**
	 * A binary or rather a base64encoded responses requires
	 * <ol>
	 * <li>"Binary Media Types" to be configured in API Gateway
	 * <li>a request with an "Accept" header set to one of the "Binary Media
	 * Types"
	 * </ol>
	 */
	public ApiGatewayResponseBuilder addCorsHeaders(){
		addHeaders(corsHeaders);
		return this;
	}

	public ApiGatewayResponseBuilder addSecurityHeaders(){
		addHeaders(securityHeaders);
		return this;
	}

	public ApiGatewayResponse build() {
		String body = null;
		if (rawBody != null) {
			body = rawBody;
		} else if (objectBody != null) {
			try {
				body = objectMapper.writeValueAsString(objectBody);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		} else if (binaryBody != null) {
			body = new String(Base64.getEncoder().encode(binaryBody), StandardCharsets.UTF_8);
		}
		ApiGatewayResponse response =  new ApiGatewayResponse(statusCode, body, headers, base64Encoded);

		ClearState();

		return response;
	}

	private void ClearState() {
		this.statusCode = 200;
		this.rawBody = null;
		this.objectBody = null;
		this.base64Encoded = false;
		this.binaryBody = null;
	}
}
