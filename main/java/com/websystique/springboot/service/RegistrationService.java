package com.websystique.springboot.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ebayopensource.fido.uaf.crypto.Notary;
import org.ebayopensource.fido.uaf.msg.DeregistrationRequest;
import org.ebayopensource.fido.uaf.msg.RegistrationRequest;
import org.ebayopensource.fido.uaf.msg.RegistrationResponse;
import org.ebayopensource.fido.uaf.storage.DuplicateKeyException;
import org.ebayopensource.fido.uaf.storage.RegistrationRecord;
import org.ebayopensource.fido.uaf.storage.StorageInterface;
import org.ebayopensource.fido.uaf.storage.SystemErrorException;
import org.springframework.stereotype.Service;

import com.google.inject.Inject;
import com.websystique.springboot.stats.Dash;

@Service
public class RegistrationService implements IRegistrationService {

	private final StorageInterface storage;
	private final Notary notary;

	@Inject
	public RegistrationService(StorageInterface storage, Notary notary)
	{
		this.storage = storage;
		this.notary = notary;
	}
	
	public RegistrationRequest[] regReqPublic(String username) {
		RegistrationRequest[] regReq = new RegistrationRequest[1];
		regReq[0] = new FetchRequest(getAppId(), getAllowedAaids(), notary)
				.getRegistrationRequest(username);
		Dash.getInstance().stats.put(Dash.LAST_REG_REQ, regReq);
		Dash.getInstance().history.add(regReq);
		return regReq;
	}
	
	/**
	 * The AppID is an identifier for a set of different Facets of a relying
	 * party's application. The AppID is a URL pointing to the TrustedFacets,
	 * i.e. list of FacetIDs related to this AppID.
	 * @return a URL pointing to the TrustedFacets
	 */
	private String getAppId() {
		// You can get it dynamically.
		// It only works if your server is not behind a reverse proxy
		//return uriInfo.getBaseUri() + "v1/public/uaf/facets";
		// Or you can define it statically
		//TODO - determine app id url dynamically
		//return "https://api.mr-b.click/fido/fidouaf/v1/public/uaf/facets";
		return "";
	}
	
	/**
	 * List of allowed AAID - Authenticator Attestation ID.
	 * Authenticator Attestation ID / AAID.
	 * A unique identifier assigned to a model, class or batch of FIDO Authenticators
	 * that all share the same characteristics, and which a Relying Party can use
	 * to look up an Attestation Public Key and Authenticator Metadata for the device.
	 * The first 4 characters of the AAID are the vendorID.
	 *
	 * @return  list of allowed AAID - Authenticator Attestation ID.
	 */
	private String[] getAllowedAaids() {
		String[] ret = { "EBA0#0001", "0015#0001", "0012#0002", "0010#0001",
				"4e4e#0001", "5143#0001", "0011#0701", "0013#0001",
				"0014#0000", "0014#0001", "53EC#C002", "DAB8#8001",
				"DAB8#0011", "DAB8#8011", "5143#0111", "5143#0120",
				"4746#F816", "53EC#3801" };
		List<String> retList = new ArrayList<String>(Arrays.asList(ret));
		retList.addAll(Dash.getInstance().uuids);
		return retList.toArray(new String[0]);
	}
	
	public RegistrationRecord[] processRegResponse(RegistrationResponse[] registrationResponse) {
		RegistrationRecord[] result = null;
		if (registrationResponse != null) {
			Dash.getInstance().stats.put(Dash.LAST_REG_RES, registrationResponse);
			Dash.getInstance().history.add(registrationResponse);

			RegistrationResponse response = registrationResponse[0];
			result = new ProcessResponse(notary, storage).processRegResponse(response);
			if (result[0].status.equals("SUCCESS")) {
				try {
					storage.store(result);
				} catch (DuplicateKeyException e) {
					result = new RegistrationRecord[1];
					result[0] = new RegistrationRecord();
					result[0].status = "Error: Duplicate Key";
				} catch (SystemErrorException e1) {
					result = new RegistrationRecord[1];
					result[0] = new RegistrationRecord();
					result[0].status = "Error: Data couldn't be stored in DB";
				}
			}
		}else{
			//TODO Could be interesting refactor this method (and its callers) and modify return type to javax.ws.rs.core.Response and send Response.Status.PRECONDITION_FAILED error code.
			result = new RegistrationRecord[1];
			result[0] = new RegistrationRecord();
			result[0].status = "Error: payload could not be empty";
		}
		return result;
	}
	
	public String deregRequestPublic(DeregistrationRequest[] deRegistrationRequest) {
		return new DeregRequestProcessor().process(deRegistrationRequest);
	}
}
