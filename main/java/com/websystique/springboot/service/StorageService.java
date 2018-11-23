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

package com.websystique.springboot.service;

import java.util.HashMap;
import java.util.Map;

import org.ebayopensource.fido.uaf.storage.DuplicateKeyException;
import org.ebayopensource.fido.uaf.storage.RegistrationRecord;
import org.ebayopensource.fido.uaf.storage.SystemErrorException;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.websystique.springboot.logging.Logger;
import com.websystique.springboot.utils.Configuration;


public class StorageService implements IStorageService {

	@Inject
	protected Logger logger;
	
	@Inject
    private Configuration config;
	
	private static IStorageService instance = new StorageService();
	private String AWS_REGION_NAME;
	private AmazonDynamoDB ddb;
	private DynamoDB dynamoDB;
	private Table registrationsTable;
	private Map<String, RegistrationRecord> db = new HashMap<String, RegistrationRecord>();
	private Map<String, String> db_names = new HashMap<String, String>();
	private static String FIDO_REGISTRATIONS_TABLE;

	protected Gson gson = new GsonBuilder().disableHtmlEscaping().create();

	private StorageService() {
		// Init
		try {
			AWS_REGION_NAME = config.getAwsRegionName();
			FIDO_REGISTRATIONS_TABLE = config.getFidoRegistrationsTable();
			ddb = AmazonDynamoDBClient.builder().withRegion(AWS_REGION_NAME).build();
			dynamoDB = new DynamoDB(ddb);
			registrationsTable = dynamoDB.getTable(FIDO_REGISTRATIONS_TABLE);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static IStorageService getInstance() {
		return instance;
	}

	public void storeServerDataString(String username, String serverDataString) {
		logger.debug("Entered storeServerDataString with username " + username + " and serverDataString " + serverDataString);
		if (db_names.containsKey(serverDataString)){
			db_names.remove(serverDataString);
		}
		db_names.put(serverDataString, username);
	}

	public String getUsername(String serverDataString) {
		logger.debug("Entered getUsername with serverDataString ", serverDataString);
		if (db_names.containsKey(serverDataString)){
			return db_names.get(serverDataString);
		}
		return null;
	}

	public void store(RegistrationRecord[] records)
			throws DuplicateKeyException, SystemErrorException {
		if (records != null && records.length > 0) {
			for (int i = 0; i < records.length; i++) {
				if (db.containsKey(records[i].authenticator.toString())) {
					throw new DuplicateKeyException();
				}
				records[i].authenticator.username = records[i].username;
				db.put(records[i].authenticator.toString(), records[i]);
			}
			storeAWS(records);
		}
	}
	
	private void storeAWS(RegistrationRecord[] records)
	{
		logger.debug("Entered storeAWS to store ... " + records.length + " items");
		//registrationsTable
		if (records != null && records.length > 0) {
			for (int i = 0; i < records.length; i++) {
				// check duplicate key
				//if (db.containsKey(records[i].authenticator.toString())) {
				//	throw new DuplicateKeyException();
				//}
				records[i].authenticator.username = records[i].username;
				Item regItem = new Item()
						.withPrimaryKey("authenticator_string", records[i].authenticator.toString())
						.withString("record", gson.toJson(records[i]));
				PutItemSpec putSpec = new PutItemSpec().withItem(regItem);
				registrationsTable.putItem(putSpec);
				logger.debug("Successfull put item ... " + i + " with key " + records[i].authenticator.toString());
			}

		}
	}

	public RegistrationRecord readRegistrationRecord(String key) {
		logger.debug("Got request for Registration Record with key ", key);
		RegistrationRecord rr = db.get(key);
		rr = readRegistrationRecordAWS(key);
		if (rr != null)
		{
			logger.debug("Registration Record username details are ", rr.username);
		}
		return rr;
	}
	
	private RegistrationRecord readRegistrationRecordAWS(String key)
	{
		logger.debug("Entered readRegistrationRecordAWS with key ", key);
		GetItemSpec spec = new GetItemSpec().withPrimaryKey("authenticator_string", key);
        try {
        	logger.debug("Attempting to read the item with key... ", key);
        	logger.debug("Attempting to read the item...");
            Item outcome = registrationsTable.getItem(spec);
            logger.debug("GetItem succeeded: ", outcome.toJSONPretty());
            return gson.fromJson(outcome.getString("record"), RegistrationRecord.class);
        }
        catch (Exception e) {
        	logger.error(e.getMessage(), e);
        	return null;
        }
	}

	public void update(RegistrationRecord[] records) {
		// TODO Auto-generated method stub
	}

	public void deleteRegistrationRecord(String key) {
		if (db != null && db.containsKey(key)) {
			logger.debug("Deleting object associated with key=", key);
			db.remove(key);
			deleteRegistrationRecordAWS(key);
		}
	}

	private void deleteRegistrationRecordAWS(String key)
	{
		DeleteItemSpec spec = new DeleteItemSpec().withPrimaryKey("authenticator_string", key);
        try {
        	logger.debug("Attempting to read the item...");
            DeleteItemOutcome outcome = registrationsTable.deleteItem(spec);
            logger.debug("Deleted item from DynamoDB with key ", key);
            logger.debug("GetItem succeeded: ", outcome);
        }
        catch (Exception e) {
        	logger.error(e.getMessage(), e);
        }
	}

	public Map<String, RegistrationRecord> dbDump() {
		logger.debug("Entered dbDump");
		//TODO - return from DynamoDB
		return db;
	}

}
