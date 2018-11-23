package com.websystique.springboot.service;

/*
 * Copyright 2015 eBay Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.security.MessageDigest;

import org.apache.commons.codec.binary.Base64;
import org.ebayopensource.fido.uaf.crypto.HMAC;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.websystique.springboot.logging.Logger;
import com.websystique.springboot.utils.Configuration;

/**
 * This is just an example implementation. You should implement this class based on your operational environment.
 */
public class NotaryService implements INotaryService {

	@Autowired
	protected Logger logger;
	
	@Autowired
    private Configuration config;
	
	private static int SERVER_DATA_EXPIRY_IN_MS;
	private String DDB_REGION;
	private AmazonDynamoDB ddb;
	private DynamoDB dynamoDB;
	private Table signaturesTable;
	//private String hmacSecret = "HMAC-is-just-one-way";
	private static INotaryService instance = new NotaryService();
	private SecretHelper secretHelper = null;
	private String secretName;   //"test/HMACNotarySecret";
	private String SIGNATURES_TABLE_NAME;

	public NotaryService() {
		// Init
		try {
			logger.debug("Initialising NotaryService");
			SERVER_DATA_EXPIRY_IN_MS = Integer.parseInt(config.getFidoExpiry());
			DDB_REGION = config.getAwsRegionName();
			secretName = config.getFidoSecretKeyName();
			SIGNATURES_TABLE_NAME = config.getFidoSignatureTable();
			secretHelper =  SecretHelper.getInstance();
			logger.debug("Created secretHelper");
			logger.debug("Secret Name within secrets manager is ", secretName);
			ddb = AmazonDynamoDBClient.builder().withRegion(DDB_REGION).build();
			dynamoDB = new DynamoDB(ddb);
			signaturesTable = dynamoDB.getTable(SIGNATURES_TABLE_NAME);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static INotaryService getInstance() {
		return instance;
	}

	public void rotateSecret()
	{
		logger.debug("Entered rotateSecret");
		secretHelper.updateSecrets(secretName);
		logger.debug("Exiting rotateSecret - secret values have been updated from Secrets Manager");
	}
	
	public String sign(String signData) {
		try {
			String signature = Base64.encodeBase64URLSafeString(HMAC.sign(signData, secretHelper.getCurrent(secretName)));
			storeAWS(signature);
			return signature;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public boolean verify(String signData, String signature) {
		logger.debug("Entered verify of NotaryService");
		if (!verifyAWS(signature))
		{
			logger.warn("The signature failed the replay check");
			return false;
		}
		try {
			boolean result = false;
			result = MessageDigest.isEqual(Base64.decodeBase64(signature), HMAC.sign(signData, secretHelper.getCurrent(secretName)));
			if (!result)
			{
				logger.warn("Verification of signature failed using current secret - trying previous version");
				//try the previous version of the secret
				result = MessageDigest.isEqual(Base64.decodeBase64(signature), HMAC.sign(signData, secretHelper.getPrevious(secretName)));
				if (!result)
				{
					logger.warn("Verification of signature failed using previous secret - updating secrets in case they have been rotated");
					//signatures do not match on current and previous. Check that secrets have not been rotated and check current again
					rotateSecret();
					result = MessageDigest.isEqual(Base64.decodeBase64(signature), HMAC.sign(signData, secretHelper.getCurrent(secretName)));
				}
			}
			logger.debug("Result if signature verification is ", result);
			return result;
		} catch (Exception e) {
			logger.error(e.toString());
		}
		return false;
	}

	private void storeAWS(String signature)
	{
		logger.debug("Entered storeAWS to store ... ", signature);
		//signaturessTable
		Item signatureItem = new Item()
				.withPrimaryKey("signature", signature)
				.withNumber("expires", ((System.currentTimeMillis() + SERVER_DATA_EXPIRY_IN_MS)/ 1000L));
		PutItemSpec putSpec = new PutItemSpec().withItem(signatureItem);
		signaturesTable.putItem(putSpec);
		logger.debug("Successfully stored item in DDB ... ");
	}

	private boolean verifyAWS(String signature)
	{
		logger.debug("Entered verifyAWS to check signature ... ", signature);
		GetItemSpec spec = new GetItemSpec().withPrimaryKey("signature", signature);
        try {
        	logger.debug("Attempting to read the item with key... ", signature);
        	logger.debug("Attempting to read the item...");
            Item outcome = signaturesTable.getItem(spec);
            if (outcome == null)
            {
            	logger.debug("Item was not present - return false");
            	return false;
            }
            else
            {
            	logger.debug("GetItem succeeded: " + outcome);
            	logger.debug("Deleting item to prevent replay attacks");
        		DeleteItemSpec delSpec = new DeleteItemSpec().withPrimaryKey("signature", signature);
                try {
                	logger.debug("Attempting to delete the signature...");
                    DeleteItemOutcome delOutcome = signaturesTable.deleteItem(delSpec);
                    logger.debug("Deleted signature from DynamoDB");
                    logger.debug(delOutcome.getDeleteItemResult());
            		return true;
                }
                catch (Exception dex) {
                    logger.error("DeleteItem failed: ", dex.getMessage());
                    return false;
                }
            }
        }
        catch (Exception e) {
            logger.error("GetItem failed: ", e.getMessage());
        	return false;
        }
	}

}

