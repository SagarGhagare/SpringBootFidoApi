package com.websystique.springboot.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.ebayopensource.fido.uaf.crypto.Notary;
import org.ebayopensource.fido.uaf.msg.AuthenticationRequest;
import org.ebayopensource.fido.uaf.msg.AuthenticationResponse;
import org.ebayopensource.fido.uaf.msg.Version;
import org.ebayopensource.fido.uaf.storage.AuthenticatorRecord;
import org.ebayopensource.fido.uaf.storage.StorageInterface;
import org.springframework.beans.factory.annotation.Autowired;

import com.websystique.springboot.facets.Facets;
import com.websystique.springboot.facets.TrustedFacets;
import com.websystique.springboot.logging.Logger;
import com.websystique.springboot.stats.Dash;


public class AuthenticationService implements IAuthenticationService {

	private final StorageInterface storage;
	private final Notary notary;

	@Autowired
	protected Logger logger;

	public AuthenticationService(StorageInterface storage, Notary notary) {
		this.storage = storage;
		this.notary = notary;
	}

	/**
	 * The AppID is an identifier for a set of different Facets of a relying party's
	 * application. The AppID is a URL pointing to the TrustedFacets, i.e. list of
	 * FacetIDs related to this AppID.
	 * 
	 * @return a URL pointing to the TrustedFacets
	 */
	private String getAppId() {
		// You can get it dynamically.
		// It only works if your server is not behind a reverse proxy
		// return uriInfo.getBaseUri() + "v1/public/uaf/facets";
		// Or you can define it statically
		// TODO - determine app id url dynamically
		// return "https://api.mr-b.click/fido/fidouaf/v1/public/uaf/facets";
		return "";
	}

	/**
	 * List of allowed AAID - Authenticator Attestation ID. Authenticator
	 * Attestation ID / AAID. A unique identifier assigned to a model, class or
	 * batch of FIDO Authenticators that all share the same characteristics, and
	 * which a Relying Party can use to look up an Attestation Public Key and
	 * Authenticator Metadata for the device. The first 4 characters of the AAID are
	 * the vendorID.
	 *
	 * @return list of allowed AAID - Authenticator Attestation ID.
	 */
	private String[] getAllowedAaids() {
		String[] ret = { "EBA0#0001", "0015#0001", "0012#0002", "0010#0001", "4e4e#0001", "5143#0001", "0011#0701",
				"0013#0001", "0014#0000", "0014#0001", "53EC#C002", "DAB8#8001", "DAB8#0011", "DAB8#8011", "5143#0111",
				"5143#0120", "4746#F816", "53EC#3801" };
		List<String> retList = new ArrayList<String>(Arrays.asList(ret));
		retList.addAll(Dash.getInstance().uuids);
		return retList.toArray(new String[0]);
	}

	public AuthenticationRequest[] getAuthReq() {
		return getAuthReqObj();
	}

	public AuthenticationRequest[] getAuthReqObj() {
		AuthenticationRequest[] ret = new AuthenticationRequest[1];
		ret[0] = new FetchRequest(getAppId(), getAllowedAaids(), notary).getAuthenticationRequest();
		Dash.getInstance().stats.put(Dash.LAST_AUTH_REQ, ret);
		Dash.getInstance().history.add(ret);
		return ret;
	}

	public AuthenticatorRecord[] processAuthResponse(AuthenticationResponse[] authenticationResponse) {
		if (authenticationResponse != null) {
			Dash.getInstance().stats.put(Dash.LAST_AUTH_RES, authenticationResponse);
			return processAuthResponseObject(authenticationResponse);
		}
		return new AuthenticatorRecord[0];
	}

	protected AuthenticatorRecord[] processAuthResponseObject(AuthenticationResponse[] authResp) {
		Dash.getInstance().stats.put(Dash.LAST_AUTH_RES, authResp);
		Dash.getInstance().history.add(authResp);
		AuthenticatorRecord[] result = new ProcessResponse(notary, storage).processAuthResponse(authResp[0]);
		logger.debug("Response to authResponse POST is", stringifyAuthenticatorRecordArray(result));
		return result;
	}

	private String stringifyAuthenticatorRecordArray(AuthenticatorRecord[] result) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (AuthenticatorRecord ar : result) {
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

	/**
	 * List of trusted Application Facet ID. An (application) facet is how an
	 * application is implemented on various platforms. For example, the application
	 * MyBank may have an Android app, an iOS app, and a Web app. These are all
	 * facets of the MyBank application.
	 *
	 * A platform-specific identifier (URI) for an application facet. For Web
	 * applications, the facet id is the RFC6454 origin [RFC6454]. For Android
	 * applications, the facet id is the URI
	 * android:apk-key-hash:<hash-of-apk-signing-cert> For iOS, the facet id is the
	 * URI ios:bundle-id:<ios-bundle-id-of-app>.
	 *
	 * @return List of trusted Application Facet ID.
	 */
	public Facets facets() {
		String timestamp = new Date().toString();
		Dash.getInstance().stats.put(Dash.LAST_REG_REQ, timestamp);
		String[] trustedIds = { "https://www.head2toes.org", "android:apk-key-hash:Df+2X53Z0UscvUu6obxC3rIfFyk",
				"android:apk-key-hash:bE0f1WtRJrZv/C0y9CM73bAUqiI", "android:apk-key-hash:Lir5oIjf552K/XN4bTul0VS3GfM",
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
		try {
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
		} catch (Exception ex) {
			// hard-code test facet for PoC
			// TODO - retrieve from DynamoDB or similar
			return "android:apk-key-hash:CxHdfRYR5KEkAfDMe4jOHGt6RKg";
		}
	}
}
