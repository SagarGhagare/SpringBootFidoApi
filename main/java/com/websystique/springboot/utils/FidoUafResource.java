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

package com.websystique.springboot.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.ebayopensource.fido.uaf.crypto.Notary;
import org.ebayopensource.fido.uaf.msg.AuthenticationRequest;
import org.ebayopensource.fido.uaf.msg.AuthenticationResponse;
import org.ebayopensource.fido.uaf.msg.Operation;
import org.ebayopensource.fido.uaf.msg.OperationHeader;
import org.ebayopensource.fido.uaf.msg.RegistrationRequest;
import org.ebayopensource.fido.uaf.msg.RegistrationResponse;
import org.ebayopensource.fido.uaf.msg.Transaction;
import org.ebayopensource.fido.uaf.msg.Version;
import org.ebayopensource.fido.uaf.storage.AuthenticatorRecord;
import org.ebayopensource.fido.uaf.storage.DuplicateKeyException;
import org.ebayopensource.fido.uaf.storage.RegistrationRecord;
import org.ebayopensource.fido.uaf.storage.StorageInterface;
import org.ebayopensource.fido.uaf.storage.SystemErrorException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.websystique.springboot.dto.GetUAFRequest;
import com.websystique.springboot.dto.ReturnUAFAuthenticationRequest;
import com.websystique.springboot.dto.ReturnUAFDeregistrationRequest;
import com.websystique.springboot.dto.ReturnUAFRegistrationRequest;
import com.websystique.springboot.dto.ServerResponse;
import com.websystique.springboot.facets.Facets;
import com.websystique.springboot.facets.TrustedFacets;
import com.websystique.springboot.service.FetchRequest;
import com.websystique.springboot.service.ProcessResponse;
import com.websystique.springboot.stats.Dash;
import com.websystique.springboot.stats.Info;


public class FidoUafResource {

	private final StorageInterface storage;
	private final Notary notary;
	protected Gson gson = new GsonBuilder().disableHtmlEscaping().create();

	public FidoUafResource(StorageInterface storage, Notary notary)
	{
		this.storage = storage;
		this.notary = notary;
	}
	public String info() {
		return gson.toJson(new Info());
	}
	
	public String whitelistuuad(String uuid) {
		Dash.getInstance().uuids.add(uuid);
		return gson.toJson(Dash.getInstance().getInstance().uuids);
	}
	
	public String whitelistfacetid(String facetId) {
		Dash.getInstance().facetIds.add(facetId);
		return gson.toJson(Dash.getInstance().facetIds);
	}

	public String getStats() {
		return gson.toJson(Dash.getInstance().stats);
	}

	public List<Object> getHistory() {
		return Dash.getInstance().history;
	}

	public Map<String, RegistrationRecord> getDbDump() {
		return storage.dbDump();
	}

	public RegistrationRequest[] getRegisReqPublic(String username) {
		System.out.println("Received /public/regRequest/{username} GET");
		System.out.println("Received getRegisReqPublic GET for " + username);
		return regReqPublic(username);
	}

	private RegistrationRequest[] regReqPublic(String username){
		RegistrationRequest[] regReq = new RegistrationRequest[1];
		regReq[0] = new FetchRequest(getAppId(), getAllowedAaids(), notary)
				.getRegistrationRequest(username);
		Dash.getInstance().stats.put(Dash.LAST_REG_REQ, regReq);
		Dash.getInstance().history.add(regReq);
		return regReq;
	}
	
	public String getRegReqForAppId(String username, 
			String appId) {
		RegistrationRequest[] regReq = getRegisReqPublic(username);
		setAppId(appId, regReq[0].header);
		return gson.toJson(regReq);
	}

	public RegistrationRequest[] postRegisReqPublic(String username) {
		System.out.println("Received postRegisReqPublic GET for " + username);
		return regReqPublic(username);
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

	/**
	 * List of trusted Application Facet ID.
	 * An (application) facet is how an application is implemented on various
	 * platforms. For example, the application MyBank may have an Android app,
	 * an iOS app, and a Web app. These are all facets of the MyBank application.
	 *
	 * A platform-specific identifier (URI) for an application facet.
	 * For Web applications, the facet id is the RFC6454 origin [RFC6454].
	 * For Android applications, the facet id is the URI
	 * android:apk-key-hash:<hash-of-apk-signing-cert>
	 * For iOS, the facet id is the URI ios:bundle-id:<ios-bundle-id-of-app>.
	 *
	 * @return List of trusted Application Facet ID.
	 */
	public Facets facets() {
		String timestamp = new Date().toString();
		Dash.getInstance().stats.put(Dash.LAST_REG_REQ, timestamp);
		String[] trustedIds = { "https://www.head2toes.org",
				"android:apk-key-hash:Df+2X53Z0UscvUu6obxC3rIfFyk",
				"android:apk-key-hash:bE0f1WtRJrZv/C0y9CM73bAUqiI",
				"android:apk-key-hash:Lir5oIjf552K/XN4bTul0VS3GfM",
				"https://openidconnect.ebay.com" };
		List<String> trustedIdsList = new ArrayList<String>(Arrays.asList(trustedIds));
		trustedIdsList.addAll(Dash.getInstance().facetIds);
		trustedIdsList.add(readFacet());
		Facets facets = new Facets();
		facets.trustedFacets = new TrustedFacets[1];
		TrustedFacets trusted = new TrustedFacets();
		trusted.version = new Version(1, 0);
		trusted.ids = trustedIdsList.toArray(new String[0]);
		facets.trustedFacets[0] = trusted;
		return facets;
	}
	
	private String readFacet() {
		try
		{
		InputStream in = getClass().getResourceAsStream("config.properties");
		String facetVal = "";
		try {
			Properties props = new Properties();
			props.load(in);
			facetVal = props.getProperty("facetId");
		} catch (IOException e) {
			e.printStackTrace();
		} 
		return facetVal.toString();
		}
		catch (Exception ex)
		{
			//hard-code test facet for PoC
			//TODO - retrieve from DynamoDB or similar
			return "android:apk-key-hash:CxHdfRYR5KEkAfDMe4jOHGt6RKg";
		}
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

	public RegistrationRecord[] processRegResponse(String payload) {
		System.out.println("Received processRegResponse POST");
		System.out.println(payload);
		RegistrationRecord[] result = null;
		if (! payload.isEmpty()) {
			RegistrationResponse[] fromJson = (new Gson()).fromJson(payload,
					RegistrationResponse[].class);
			Dash.getInstance().stats.put(Dash.LAST_REG_RES, fromJson);
			Dash.getInstance().history.add(fromJson);

			RegistrationResponse registrationResponse = fromJson[0];
			result = new ProcessResponse(notary, storage).processRegResponse(registrationResponse);
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

	public String deregRequestPublic(String payload) {
		System.out.println("Received deregRequestPublic POST");
		System.out.println(payload);
		return null;//new DeregRequestProcessor().process(payload);
	}

	public String getAuthReq() {
		System.out.println("Received authResponse GET");
		return gson.toJson(getAuthReqObj());
	}

	public String getAuthForAppIdReq(String appId) {
		AuthenticationRequest[] authReqObj = getAuthReqObj();
		setAppId(appId, authReqObj[0].header);
		
		return gson.toJson(authReqObj);
	}

	private void setAppId(String appId, OperationHeader header) {
		if (appId == null || appId.isEmpty()){
			return;
		}
		String decodedAppId = new String (Base64.decodeBase64(appId));
		Facets facets = facets();
		if (facets == null || facets.trustedFacets == null || facets.trustedFacets.length == 0
				 || facets.trustedFacets[0] == null || facets.trustedFacets[0].ids == null){
			return;
		}
		String[] ids = facets.trustedFacets[0].ids;
		for (int i = 0; i < ids.length; i++) {
			
			if (decodedAppId.equals(ids[i])){
				header.appID = decodedAppId;
				break;
			}
		}
	}

	public String getAuthTrxReq(String appId,
			String trxContent) {
		AuthenticationRequest[] authReqObj = getAuthReqObj();
		setAppId(appId, authReqObj[0].header);
		setTransaction(trxContent, authReqObj);
		
		return gson.toJson(authReqObj);
	}

	private void setTransaction(String trxContent, AuthenticationRequest[] authReqObj) {
		authReqObj[0].transaction = new Transaction[1];
		Transaction t = new Transaction();
		t.content = trxContent;
		t.contentType = "txt/plain"; // MediaType.TEXT_PLAIN;
		authReqObj[0].transaction[0] = t;
	}

	public AuthenticationRequest[] getAuthReqObj() {
		AuthenticationRequest[] ret = new AuthenticationRequest[1];
		ret[0] = new FetchRequest(getAppId(), getAllowedAaids(), notary)
				.getAuthenticationRequest();
		Dash.getInstance().stats.put(Dash.LAST_AUTH_REQ, ret);
		Dash.getInstance().history.add(ret);
		return ret;
	}

	public AuthenticatorRecord[] processAuthResponse(String payload) {
		if (!payload.isEmpty()) {
			System.out.println("Received authResponse POST");
			System.out.println(payload);
			Dash.getInstance().stats.put(Dash.LAST_AUTH_RES, payload);
			Gson gson = new Gson();
			AuthenticationResponse[] authResp = gson.fromJson(payload,
					AuthenticationResponse[].class);
			return processAuthResponseObject(authResp);
		}
		return new AuthenticatorRecord[0];
	}
	
	protected AuthenticatorRecord[] processAuthResponseObject(AuthenticationResponse[] authResp) {
		Dash.getInstance().stats.put(Dash.LAST_AUTH_RES, authResp);
		Dash.getInstance().history.add(authResp);
		AuthenticatorRecord[] result = new ProcessResponse(notary, storage)
				.processAuthResponse(authResp[0]);
		System.out.println("Response to authResponse POST is");
		System.out.println(stringifyAuthenticatorRecordArray(result));
		return result;
	}
	
	private String stringifyAuthenticatorRecordArray(AuthenticatorRecord[] result)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (AuthenticatorRecord ar : result)
		{
			sb.append("{");
			sb.append("AAID  : \"");
			sb.append(ar.AAID);
			sb.append("\",");
			sb.append("deviceId : \"");
			sb.append(ar.deviceId);
			sb.append("\",");
			sb.append("KeyID : \"");
			sb.append(ar.KeyID);
			sb.append("\",");
			sb.append("status : \"");
			sb.append(ar.status);
			sb.append("\",");
			sb.append("username : \"");
			sb.append(ar.username);
			sb.append("\"");
			sb.append("}");
		}
		sb.append("]");
		return sb.toString();
	}

	public ReturnUAFRegistrationRequest GetUAFRegistrationRequest(String payload) {
		RegistrationRequest[] result = getRegisReqPublic("iafuser01");
		ReturnUAFRegistrationRequest uafReq = null;
		if (result != null) {
			uafReq = new ReturnUAFRegistrationRequest();
			uafReq.statusCode = 1200;
			uafReq.uafRequest = result;
			uafReq.op = Operation.Reg;
			uafReq.lifetimeMillis = 5 * 60 * 1000;
		}
		return uafReq;
	}

	public ReturnUAFAuthenticationRequest GetUAFAuthenticationRequest(
			String payload) {
		AuthenticationRequest[] result = getAuthReqObj();
		ReturnUAFAuthenticationRequest uafReq = null;
		if (result != null) {
			uafReq = new ReturnUAFAuthenticationRequest();
			uafReq.statusCode = 1200;
			uafReq.uafRequest = result;
			uafReq.op = Operation.Auth;
			uafReq.lifetimeMillis = 5 * 60 * 1000;
		}
		return uafReq;
	}

	public ReturnUAFDeregistrationRequest GetUAFDeregistrationRequest(
			String payload) {
		String result = deregRequestPublic(payload);
		ReturnUAFDeregistrationRequest uafReq = new ReturnUAFDeregistrationRequest();
		if (result.equalsIgnoreCase("Success")) {
			uafReq.statusCode = 1200;
		} else if (result
				.equalsIgnoreCase("Failure: Problem in deleting record from local DB")) {
			uafReq.statusCode = 1404;
		} else if (result
				.equalsIgnoreCase("Failure: problem processing deregistration request")) {
			uafReq.statusCode = 1491;
		} else {
			uafReq.statusCode = 1500;

		}
		uafReq.uafRequest = null;
		uafReq.op = Operation.Dereg;
		uafReq.lifetimeMillis = 0;
		return uafReq;
	}

	public ServerResponse UAFAuthResponse(String payload) {
		ServerResponse servResp = new ServerResponse();
		if (!payload.isEmpty()) {
			String findOp = payload;
			findOp = findOp.substring(findOp.indexOf("op") + 6,
					findOp.indexOf(",", findOp.indexOf("op")) - 1);
			System.out.println("findOp=" + findOp);

			AuthenticatorRecord[] result = processAuthResponse(payload);

			if (result[0].status.equals("SUCCESS")) {
				servResp.statusCode = 1200;
				servResp.Description = "OK. Operation completed";
			} else if (result[0].status.equals("FAILED_SIGNATURE_NOT_VALID")
					|| result[0].status.equals("FAILED_SIGNATURE_VERIFICATION")
					|| result[0].status.equals("FAILED_ASSERTION_VERIFICATION")) {
				servResp.statusCode = 1496;
				servResp.Description = result[0].status;
			} else if (result[0].status.equals("INVALID_SERVER_DATA_EXPIRED")
					|| result[0].status
					.equals("INVALID_SERVER_DATA_SIGNATURE_NO_MATCH")
					|| result[0].status.equals("INVALID_SERVER_DATA_CHECK_FAILED")) {
				servResp.statusCode = 1491;
				servResp.Description = result[0].status;
			} else {
				servResp.statusCode = 1500;
				servResp.Description = result[0].status;
			}
		}else{
			servResp.Description = "Error: payload is empty";
		}

		return servResp;
	}

	public ServerResponse UAFRegResponse(String payload) {
		ServerResponse servResp = new ServerResponse();
		if (!payload.isEmpty()) {
			String findOp = payload;
			findOp = findOp.substring(findOp.indexOf("op") + 6,
					findOp.indexOf(",", findOp.indexOf("op")) - 1);
			System.out.println("findOp=" + findOp);

			RegistrationRecord[] result = processRegResponse(payload);

			if (result[0].status.equals("SUCCESS")) {
				servResp.statusCode = 1200;
				servResp.Description = "OK. Operation completed";
			} else if (result[0].status.equals("ASSERTIONS_CHECK_FAILED")) {
				servResp.statusCode = 1496;
				servResp.Description = result[0].status;
			} else if (result[0].status.equals("INVALID_SERVER_DATA_EXPIRED")
					|| result[0].status
					.equals("INVALID_SERVER_DATA_SIGNATURE_NO_MATCH")
					|| result[0].status.equals("INVALID_SERVER_DATA_CHECK_FAILED")) {
				servResp.statusCode = 1491;
				servResp.Description = result[0].status;
			} else {
				servResp.statusCode = 1500;
				servResp.Description = result[0].status;
			}
		}else{
			servResp.Description = "Error: payload is empty";
		}

		return servResp;
	}

	public String GetUAFRequest(String payload) {
		String uafReq = null;
		if (!payload.isEmpty()) {
			Gson gson = new Gson();
			GetUAFRequest req = gson.fromJson(payload, GetUAFRequest.class);

			if (req.op.name().equals("Reg")) {
				RegistrationRequest[] result = getRegisReqPublic("iafuser01");
				ReturnUAFRegistrationRequest uafRegReq = null;
				if (result != null) {
					uafRegReq = new ReturnUAFRegistrationRequest();
					uafRegReq.statusCode = 1200;
					uafRegReq.uafRequest = result;
					uafRegReq.op = Operation.Reg;
					uafRegReq.lifetimeMillis = 5 * 60 * 1000;
				}
				uafReq = gson.toJson(uafRegReq);
			} else if (req.op.name().equals("Auth")) {
				AuthenticationRequest[] result = getAuthReqObj();
				ReturnUAFAuthenticationRequest uafAuthReq = null;
				if (result != null) {
					uafAuthReq = new ReturnUAFAuthenticationRequest();
					uafAuthReq.statusCode = 1200;
					uafAuthReq.uafRequest = result;
					uafAuthReq.op = Operation.Auth;
					uafAuthReq.lifetimeMillis = 5 * 60 * 1000;
				}
				uafReq = gson.toJson(uafAuthReq);
			} else if (req.op.name().equals("Dereg")) {
				String result = deregRequestPublic(payload);
				ReturnUAFDeregistrationRequest uafDeregReq = new ReturnUAFDeregistrationRequest();
				if (result.equalsIgnoreCase("Success")) {
					uafDeregReq.statusCode = 1200;
				} else if (result
						.equalsIgnoreCase("Failure: Problem in deleting record from local DB")) {
					uafDeregReq.statusCode = 1404;
				} else if (result
						.equalsIgnoreCase("Failure: problem processing deregistration request")) {
					uafDeregReq.statusCode = 1491;
				} else {
					uafDeregReq.statusCode = 1500;

				}
				uafDeregReq.uafRequest = null;
				uafDeregReq.op = Operation.Dereg;
				uafDeregReq.lifetimeMillis = 0;
				uafReq = gson.toJson(uafDeregReq);
			}
		}
		return uafReq;
	}

	public ServerResponse UAFResponse(String payload) {
		ServerResponse servResp = new ServerResponse();
		if (!payload.isEmpty()) {
			String findOp = payload;
			findOp = findOp.substring(findOp.indexOf("op") + 6,
					findOp.indexOf(",", findOp.indexOf("op")) - 1);
			System.out.println("findOp=" + findOp);

			if (findOp.equals("Reg")) {
				RegistrationRecord[] result = processRegResponse(payload);

				if (result[0].status.equals("SUCCESS")) {
					servResp.statusCode = 1200;
					servResp.Description = "OK. Operation completed";
				} else if (result[0].status.equals("ASSERTIONS_CHECK_FAILED")) {
					servResp.statusCode = 1496;
					servResp.Description = result[0].status;
				} else if (result[0].status.equals("INVALID_SERVER_DATA_EXPIRED")
						|| result[0].status
						.equals("INVALID_SERVER_DATA_SIGNATURE_NO_MATCH")
						|| result[0].status
						.equals("INVALID_SERVER_DATA_CHECK_FAILED")) {
					servResp.statusCode = 1491;
					servResp.Description = result[0].status;
				} else {
					servResp.statusCode = 1500;
					servResp.Description = result[0].status;
				}
			} else if (findOp.equals("Auth")) {
				AuthenticatorRecord[] result = processAuthResponse(payload);

				if (result[0].status.equals("SUCCESS")) {
					servResp.statusCode = 1200;
					servResp.Description = "OK. Operation completed";
				} else if (result[0].status.equals("FAILED_SIGNATURE_NOT_VALID")
						|| result[0].status.equals("FAILED_SIGNATURE_VERIFICATION")
						|| result[0].status.equals("FAILED_ASSERTION_VERIFICATION")) {
					servResp.statusCode = 1496;
					servResp.Description = result[0].status;
				} else if (result[0].status.equals("INVALID_SERVER_DATA_EXPIRED")
						|| result[0].status
						.equals("INVALID_SERVER_DATA_SIGNATURE_NO_MATCH")
						|| result[0].status
						.equals("INVALID_SERVER_DATA_CHECK_FAILED")) {
					servResp.statusCode = 1491;
					servResp.Description = result[0].status;
				} else {
					servResp.statusCode = 1500;
					servResp.Description = result[0].status;
				}
			}
		}else{
			servResp.Description = "Error: payload is empty";
		}
		return servResp;
	}
}
