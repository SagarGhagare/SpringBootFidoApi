package com.websystique.springboot.service;

import org.ebayopensource.fido.uaf.msg.AuthenticationRequest;
import org.ebayopensource.fido.uaf.msg.AuthenticationResponse;
import org.ebayopensource.fido.uaf.storage.AuthenticatorRecord;

public interface IAuthenticationService {

	public AuthenticationRequest[] getAuthReq();
	
	public AuthenticatorRecord[] processAuthResponse(AuthenticationResponse[] authenticationResponse);
	
	public com.websystique.springboot.facets.Facets facets();
}
