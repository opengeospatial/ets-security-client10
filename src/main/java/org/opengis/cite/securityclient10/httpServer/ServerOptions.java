package org.opengis.cite.securityclient10.httpServer;

/**
 * Class for options passed to emulated server types in TestServer.
 * Used to specify all Requirement Classes from the Web Services 
 * Security Standard that will apply on the test run.
 *
 */
public class ServerOptions {

	/**
	 * The type of service being emulated. See TestRunArgValidator
	 * for valid values.
	 */
	private String serviceType;
	/**
	 * The URL to the SAML2 metadata file. Optional.
	 */
	private String saml2Url;

	public ServerOptions(String serviceType) {
		this.serviceType = serviceType;
	}
	
	/**
	 * Get the URL for the SAML2 metadata file.
	 * @return String
	 */
	public String getSaml2Url() {
		return this.saml2Url;
	}
	
	/**
	 * Get the type of server to be emulated.
	 * @return String
	 */
	public String getServiceType() {
		return this.serviceType;
	}
	
	/**
	 * Set the SAML2 metadata file URL. If a blank string, then
	 * null is used and the parameter is omitted from the emulated
	 * server.
	 * @param url A string with the URL to the SAML2 metadata file.
	 */
	public void setSaml2Url(String url) {
		if (url == null || url.isEmpty()) {
			this.saml2Url = null;
		} else {
			this.saml2Url = url;
		}
	}
}
