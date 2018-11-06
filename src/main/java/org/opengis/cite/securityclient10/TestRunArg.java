package org.opengis.cite.securityclient10;

/**
 * An enumerated type defining all recognized test run arguments.
 */
public enum TestRunArg {

	/**
	 * A string for the type of OGC Web Service to emulate for client security 
	 * testing.
	 */
	Service_Type,
	
	/**
	 * A string for the ip interface to bind the embedded web server
	 */
	Address, 
	
	/**
	 * An integer for the port to bind the embedded web server
	 */
	Port,
	
	/**
	 * A string of the domain hostname or ip address to advertise to clients
	 */
	Host,
	
	/**
	 * A string for the URL path fragment that will be the test endpoint 
	 */
	Path,
	
	/**
	 * A path to a local file containing the Java KeyStore
	 */
	JKS_Path,
	
	/**
	 * The password to access the Java KeyStore
	 */
	JKS_Password,
	
	/**
	 * The authentication method presented to secure clients
	 */
	Authentication,
	
	/**
	 * The URL for the Identity Provider SSO resource, if SAML2 authentication is enabled
	 */
	IDP_URL,
	
	/**
	 * Whether HTTP Methods will be added to the security annotations
	 */
	HTTP_METHODS,
	
	/**
	 * Whether W3C CORS support will be added to the security annotations
	 */
	W3C_CORS,
	
	/**
	 * Whether HTTP Exception Handling support will be added to the security annotations
	 */
	HTTP_EXCEPTION_HANDLING,
	
	/**
	 * Whether HTTP POST Content-Type support will be added to the security annotations
	 */
	HTTP_POST_CONTENT_TYPE,
	
	/**
	 * The request(s) from the secure client, containing the headers and body,
	 * that will be verified by the test suite.
	 */
	IUT;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
