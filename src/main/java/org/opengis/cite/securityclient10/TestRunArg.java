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
	 * The URL for the SAML2 metadata file, if SAML2 authentication is enabled
	 */
	Authentication_SAML2,
	
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
