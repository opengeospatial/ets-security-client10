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
	Host, 
	
	/**
	 * An integer for the port to bind the embedded web server
	 */
	Port,
	
	/**
	 * A path to a local file containing the Java KeyStore
	 */
	JKS_Path;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
