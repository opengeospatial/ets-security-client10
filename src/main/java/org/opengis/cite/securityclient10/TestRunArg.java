package org.opengis.cite.securityclient10;

/**
 * An enumerated type defining all recognized test run arguments.
 */
public enum TestRunArg {

	/**
	 * A string for the type of OGC Web Service to emulate for client security 
	 * testing.
	 */
	ServiceType;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
