package com.websystique.springboot.service;

import org.ebayopensource.fido.uaf.msg.DeregistrationRequest;
import org.ebayopensource.fido.uaf.msg.RegistrationRequest;
import org.ebayopensource.fido.uaf.msg.RegistrationResponse;
import org.ebayopensource.fido.uaf.storage.RegistrationRecord;


public interface IRegistrationService {

	public RegistrationRequest[] regReqPublic(String username);
	
	public RegistrationRecord[] processRegResponse(RegistrationResponse[] registrationResponse);
	
	public String deregRequestPublic(DeregistrationRequest[] deRegistrationRequest);
}
