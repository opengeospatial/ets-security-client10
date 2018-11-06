package org.opengis.cite.securityclient10.httpServer;

/**
 * Class for options passed to emulated server types in TestServer.
 * Used to specify all Requirement Classes from the Web Services 
 * Security Standard that will apply on the test run.
 *
 */
public class ServerOptions {

	/**
	 * The authentication method to apply. Currently supported are
	 * "none" and "saml2".
	 */
	private String authentication;
	/**
	 * Whether W3C CORS will be included in the security 
	 * annotations.
	 */
	private Boolean cors;
	/**
	 * Whether HTTP Exception Handling will be included in the security 
	 * annotations.
	 */
	private Boolean httpExceptionHandling;
	/**
	 * Whether HTTP Methods will be included in the security 
	 * annotations.
	 */
	private Boolean httpMethods;
	/**
	 * Whether HTTP POST Content-Type will be included in the security 
	 * annotations.
	 */
	private Boolean httpPostContentType;
	/**
	 * The URL to the Identity Provider SSO resource URL. Optional.
	 */
	private String idpUrl;
	/**
	 * The type of service being emulated. See TestRunArgValidator
	 * for valid values.
	 */
	private String serviceType;

	public ServerOptions(String serviceType) {
		this.serviceType = serviceType;
		this.authentication = "none";
		this.cors = false;
		this.httpExceptionHandling = false;
		this.httpMethods = false;
		this.httpPostContentType = false;
	}
	
	/**
	 * Get the authentication type for the secure annotations.
	 * @return String
	 */
	public String getAuthentication() {
		return this.authentication;
	}
	
	/**
	 * Determine the number of client requests to expect for a
	 * given type of authentication scheme. For SAML2, the IdP URL
	 * must also be defined.
	 * 
	 * None  - 1 request
	 * Saml2 - 3 requests
	 * 
	 * @return int number of requests
	 */
	public int getExpectedRequestCount() {
		if (this.authentication.equals("none")) {
			return 1;
		} else if (this.authentication.equals("saml2") && this.idpUrl != null) {
			return 3;
		}
		
		return 1;
	}
	
	/**
	 * Get whether W3C CORS is enabled from the test
	 * run properties.
	 * @return Boolean
	 */
	public Boolean getCors() {
		return cors;
	}
	
	/**
	 * Get whether HTTP Exception Handling is enabled from the test
	 * run properties.
	 * @return Boolean
	 */
	public Boolean getHttpExceptionHandling() {
		return httpExceptionHandling;
	}
	
	/**
	 * Get whether HTTP POST Content-Type is enabled from the test
	 * run properties.
	 * @return Boolean
	 */
	public Boolean getHttpPostContentType() {
		return httpPostContentType;
	}

	/**
	 * Get whether HTTP Methods are enabled from the test
	 * run properties.
	 * @return Boolean
	 */
	public Boolean getHttpMethods() {
		return httpMethods;
	}
	
	/**
	 * Get the URL for the Identity Provider SSO resource.
	 * @return String
	 */
	public String getIdpUrl() {
		return this.idpUrl;
	}
	
	/**
	 * Get the type of server to be emulated.
	 * @return String
	 */
	public String getServiceType() {
		return this.serviceType;
	}
	
	/**
	 * Set the authentication method. If a blank string or null,
	 * then no authentication method will be applied. Currently
	 * valid values are "none" and "saml2".
	 * @param auth A string with a valid authentication code
	 */
	public void setAuthentication(String auth) {
		if (auth != null && !auth.isEmpty()) {
			this.authentication = auth;
		}
	}
	
	public void setCors(boolean enabled) {
		this.cors = enabled;
	}
	
	public void setHttpExceptionHandling(boolean enabled) {
		this.httpExceptionHandling = enabled;
	}
	
	public void setHttpMethods(boolean enabled) {
		this.httpMethods = enabled;
	}
	
	public void setHttpPostContentType(boolean enabled) {
		this.httpPostContentType = enabled;
	}
	
	/**
	 * Set the Identity Provider SSO resource URL. If a blank string, then
	 * null is used and the parameter is omitted from the emulated
	 * server.
	 * @param url A string with the URL to the Identity Provider SSO resource.
	 */
	public void setIdpUrl(String url) {
		if (url == null || url.isEmpty()) {
			this.idpUrl = null;
		} else {
			this.idpUrl = url;
		}
	}
	
}
