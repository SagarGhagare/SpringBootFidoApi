package com.websystique.springboot.service;

import java.util.HashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.DecryptionFailureException;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.InternalServiceErrorException;
import com.amazonaws.services.secretsmanager.model.InvalidParameterException;
import com.amazonaws.services.secretsmanager.model.InvalidRequestException;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.websystique.springboot.utils.Configuration;


public class SecretHelper {

	private static final Logger logger = LogManager.getLogger(SecretHelper.class);

	private static SecretHelper instance = new SecretHelper();
	private static final String VERSIONSTAGECURRENT = "AWSCURRENT";
	private static final String VERSIONSTAGEPREVIOUS = "AWSPREVIOUS";
	private static HashMap<String, String> currentSecret = new HashMap<String, String>();
	private static HashMap<String, String> previousSecret = new HashMap<String, String>();
	// private static final String region = "eu-west-2";

	public static SecretHelper getInstance() {
		return instance;
	}

	public void updateSecrets(String secretName) {
		currentSecret.put(secretName, getSecret(secretName, VERSIONSTAGECURRENT));
		previousSecret.put(secretName, getSecret(secretName, VERSIONSTAGEPREVIOUS));
	}

	public String getCurrent(String secretName) {
		logger.debug("entered getCurrent for key "+ secretName);
		if (!currentSecret.containsKey(secretName)) {
			logger.debug("currentSecret is null for key " + secretName + ", so updating");
			updateSecrets(secretName);
		}
		return currentSecret.get(secretName);
	}

	public String getPrevious(String secretName) {
		logger.debug("entered getPrevious for key "+ secretName);
		if (!previousSecret.containsKey(secretName)) {
			logger.debug("previousSecret is null for key " + secretName + ", so updating");
			updateSecrets(secretName);
		}
		return previousSecret.get(secretName);
	}

	private String getSecret(String secretName, String versionStage) {
		logger.debug("entered getSecret for secretName " + secretName + " and versionStage " + versionStage);
		// Create a Secrets Manager client
		AWSSecretsManager client = AWSSecretsManagerClientBuilder.standard().withRegion(Regions.EU_WEST_2)
				.build();

		// In this sample we only handle the specific exceptions for the
		// 'GetSecretValue' API.
		// See
		// https://docs.aws.amazon.com/secretsmanager/latest/apireference/API_GetSecretValue.html
		// We rethrow the exception by default.

		GetSecretValueRequest getSecretValueRequest = new GetSecretValueRequest().withSecretId(secretName)
				.withVersionStage(versionStage);
		GetSecretValueResult getSecretValueResult = null;

		try {
			getSecretValueResult = client.getSecretValue(getSecretValueRequest);
		} catch (DecryptionFailureException e) {
			// Secrets Manager can't decrypt the protected secret text using the provided
			// KMS key.
			// Deal with the exception here, and/or rethrow at your discretion.
			logger.error("Caught DecryptionFailureException ", e);
		} catch (InternalServiceErrorException e) {
			// An error occurred on the server side.
			// Deal with the exception here, and/or rethrow at your discretion.
			logger.error("Caught InternalServiceErrorException ", e);
		} catch (InvalidParameterException e) {
			// You provided an invalid value for a parameter.
			// Deal with the exception here, and/or rethrow at your discretion.
			logger.error("Caught InvalidParameterException ", e);
		} catch (InvalidRequestException e) {
			// You provided a parameter value that is not valid for the current state of the
			// resource.
			// Deal with the exception here, and/or rethrow at your discretion.
			logger.error("Caught InvalidRequestException ", e);
		} catch (ResourceNotFoundException e) {
			// We can't find the resource that you asked for.
			// Deal with the exception here, and/or rethrow at your discretion.
			logger.warn("Caught ResourceNotFoundException ", e);
		}

		// Decrypts secret using the associated KMS CMK.
		// Depending on whether the secret is a string or binary, one of these fields
		// will be populated.
		if (getSecretValueResult != null) {
			if (getSecretValueResult.getSecretString() != null) {
				logger.debug("The secretValueResult is " + getSecretValueResult.toString());
				return processSecretValueResultString(getSecretValueResult.getSecretString());
			} else {
				logger.warn("Failed to retrieve secret for secretName " + secretName + " and versionStage "
						+ versionStage + " as string value was null");
				return new String();
			}
		} else {
			logger.warn("Failed to retrieve secret for secretName " + secretName + " and versionStage " + versionStage);
			return new String();
		}
	}

	private String processSecretValueResultString(String secretValueResultString) {
		logger.debug("Entered processSecretValueResultString with secretValueResultString "+ secretValueResultString);
		// string is in format {"[[[secretName]]]":"[[[secretValue]]]"} - this function
		// returns [[[secretValue]]]
		try {
			int colonPosition = secretValueResultString.indexOf(":");
			int firstQuotePosition = secretValueResultString.indexOf("\"", colonPosition);
			int lastQuotePosition = secretValueResultString.lastIndexOf("\"");
			String secretValue = secretValueResultString.substring(firstQuotePosition + 1, lastQuotePosition);
			return secretValue;
		} catch (Exception e) {
			logger.error("Caught error processing secret value result string ", e);
			return new String();
		}
	}
}
