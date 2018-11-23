package com.websystique.springboot.utils;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.amazonaws.util.StringUtils;

import com.websystique.springboot.logging.Level;

@Service
public class EnvironmentVariableConfiguration implements Configuration {

	private final String AWS_REGION_NAME;
	private final String DYNAMO_ENDPOINT;
	private final String STAGE;
	private final String COGNITO_ADMIN_USER_POOL_ID;
	private final String COGNITO_ADMIN_CLIENT_ID;
	private final String COGNITO_SMARTCARD_USER_PASSWORD;
	private final String SSO_JWT_ISSUER;
	private final String ALLOWED_ORIGIN;
	private final String FIDO_REGISTRATIONS_TABLE;
	private String LOG_LEVEL;
	private final String SIGNATURES_TABLE_NAME;
	private final String FIDO_EXPIRY_MSECS;
	private final String SECRET_KEY_NAME;

	private final Map<String, String> vars;

	public EnvironmentVariableConfiguration() {

		vars = System.getenv();

		AWS_REGION_NAME = vars.get("AWS_REGION_NAME");
		DYNAMO_ENDPOINT = vars.get("DYNAMO_ENDPOINT");
		STAGE = vars.get("STAGE");
		COGNITO_ADMIN_USER_POOL_ID = vars.get("COGNITO_ADMIN_USER_POOL_ID");
		COGNITO_ADMIN_CLIENT_ID = vars.get("COGNITO_ADMIN_CLIENT_ID");
		COGNITO_SMARTCARD_USER_PASSWORD = vars.get("COGNITO_SMARTCARD_USER_PASSWORD");
		SSO_JWT_ISSUER = vars.get("SSO_JWT_ISSUER"); // eg "https://auth.uat.signin.nhs.uk"
		ALLOWED_ORIGIN = vars.get("ALLOWED_ORIGIN");
		LOG_LEVEL = vars.get("LOG_LEVEL");
		FIDO_REGISTRATIONS_TABLE = vars.get("FIDO_REGISTRATIONS_TABLE");
		SIGNATURES_TABLE_NAME = vars.get("SIGNATURES_TABLE_NAME");
		FIDO_EXPIRY_MSECS = vars.get("FIDO_EXPIRY_MSECS");
		SECRET_KEY_NAME = vars.get("SECRET_KEY_NAME");

		if (StringUtils.isNullOrEmpty(LOG_LEVEL)) {
			LOG_LEVEL = "DEBUG";
		}
	}

	private String getVar(String name) {

		if (!vars.containsKey(name)) {
			throw new RuntimeException("Missing environment variable " + name);
		}

		return vars.get(name);
	}

	public String getAllowedOrigin() {
		return ALLOWED_ORIGIN;
	}

	public String getAwsRegionName() {
		return AWS_REGION_NAME;
	}

	public String get_dynamoEndpoint() {
		return DYNAMO_ENDPOINT;
	}

	public String get_stage() {
		return STAGE;
	}

	public String get_cognitoAdminUserPoolId() {
		return COGNITO_ADMIN_USER_POOL_ID;
	}

	public String get_cognitoAdminClientId() {
		return COGNITO_ADMIN_CLIENT_ID;
	}

	public String get_cognitoSmartcardUserPassword() {
		return COGNITO_SMARTCARD_USER_PASSWORD;
	}

	@Override
	public String get_ssoJwtIssuer() {
		return SSO_JWT_ISSUER;
	}

	@Override
	public Level getLogLevel() {
		return Level.valueOf(this.LOG_LEVEL);
	}
	
	public String getFidoRegistrationsTable() {
		return FIDO_REGISTRATIONS_TABLE;
	}
	
	public String getFidoSignatureTable() {
		return SIGNATURES_TABLE_NAME;
	}
	
	public String getFidoSecretKeyName() {
		return SECRET_KEY_NAME;
	}
	
	public String getFidoExpiry() {
		return FIDO_EXPIRY_MSECS;
	}
}