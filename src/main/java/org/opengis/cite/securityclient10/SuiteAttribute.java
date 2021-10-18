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
     * A String for the server url of the embedded server.
     */
    TEST_SERVER_URL("serverUrl", String.class),

    /**
     * A String for the filesystem path to the Java KeyStore file.
     */
    TEST_JKS_PATH("testJKSPath", String.class),
    /**
     * A String to unlock the Java KeyStore file.
     */
    TEST_JKS_PASSWORD("testJKSPassword", String.class),
    /**
     * A String specifying which authentication method to present in the capabilities.
     */
    TEST_AUTHENTICATION("testAuthentication", String.class),
    /**
     * A String for the Identity Provider SSO resource URL, if SAML2 is enabled.
     */
    TEST_IDP_URL("testIdpUrl", String.class),
    /**
     * A Boolean for whether Requirement Class "HTTP Methods" is enabled.
     */
    TEST_HTTP_METHODS("testHttpMethods", Boolean.class),
    /**
     * A Boolean for whether Requirement Class "HTTP Methods" is enabled.
     */
    TEST_W3C_CORS("testW3CCors", Boolean.class),
    /**
     * A Boolean for whether Requirement Class "HTTP Exception Handling" is enabled.
     */
    TEST_HTTP_EXCEPTION_HANDLING("testHttpExceptionHandling", Boolean.class),
    /**
     * A Boolean for whether Requirement Class "HTTP POST Content-Type" is enabled.
     */
    TEST_HTTP_POST_CONTENT_TYPE("testHttpPostContentType", Boolean.class),
    /**
     * A String for the filesystem path of the serialized client requests document.
     */
    TEST_IUT("testIUT", String.class);
	
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
