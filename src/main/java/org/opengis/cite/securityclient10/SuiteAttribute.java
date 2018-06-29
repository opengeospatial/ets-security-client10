package org.opengis.cite.securityclient10;

import com.sun.jersey.api.client.Client;

/**
 * An enumerated type defining ISuite attributes that may be set to constitute a
 * shared test fixture.
 */
@SuppressWarnings("rawtypes")
public enum SuiteAttribute {

    /**
     * A client component for interacting with HTTP endpoints.
     */
    CLIENT("httpClient", Client.class),
    /**
     * A String that identifies the type of OGC Web Service that will be emulated.
     */
    TEST_SERVICE_TYPE("testServiceType", String.class),
	/**
     * A String for the host address to bind the embedded server.
     */
    TEST_HOST("testHost", String.class),
    /**
     * An Integer for the port to bind the embedded server.
     */
    TEST_PORT("testPort", Integer.class),
    /**
     * A String for the filesystem path to the java keystore file.
     */
    TEST_JKS_PATH("testJKSPath", String.class);
	
    private final Class attrType;
    private final String attrName;

    private SuiteAttribute(String attrName, Class attrType) {
        this.attrName = attrName;
        this.attrType = attrType;
    }

    public Class getType() {
        return attrType;
    }

    public String getName() {
        return attrName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(attrName);
        sb.append('(').append(attrType.getName()).append(')');
        return sb.toString();
    }
}
