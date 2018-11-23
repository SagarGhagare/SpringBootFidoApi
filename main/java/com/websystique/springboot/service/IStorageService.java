package com.websystique.springboot.service;

import java.util.Map;

import org.ebayopensource.fido.uaf.storage.DuplicateKeyException;
import org.ebayopensource.fido.uaf.storage.RegistrationRecord;
import org.ebayopensource.fido.uaf.storage.SystemErrorException;

public interface IStorageService {

	public void storeServerDataString(String username, String serverDataString);

	public String getUsername(String serverDataString);

	public void store(RegistrationRecord[] records)
			throws DuplicateKeyException, SystemErrorException;

	public RegistrationRecord readRegistrationRecord(String key);

	public void update(RegistrationRecord[] records);

	public void deleteRegistrationRecord(String key);

	public Map<String, RegistrationRecord> dbDump();
}
