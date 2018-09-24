package org.opengis.cite.securityclient10.levelCommonSecurity;

import org.opengis.cite.securityclient10.CommonFixture;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Includes various tests of Common Security capability. See 
 * {@link org.opengis.cite.securityclient10.CommonFixture} for fixture loading methods.
 */
public class CapabilityCommonSecurityTests extends CommonFixture {
    /**
     * Verifies that the secure client connects over HTTPS.
     * Requirements Class: urn:ogc:def:security:1.0:rc:https
     */
    @Test(description = "Connection(s) made using HTTPS.\n"
    		+ "Requirements Class: urn:ogc:def:security:1.0:rc:https")
    public void isHTTPS() {
        NodeList requests = this.testRequestSet.getElementsByTagName("Request");
        
        Assert.assertNotNull(requests, "No secure client requests were collected by the test suite.");
        
        for (int i = 0; i < requests.getLength(); i++) {
        	Node request = requests.item(i);
        	
        	if (request.getNodeType() == Node.ELEMENT_NODE) {
        		Element requestElement = (Element) request;
        		Assert.assertEquals(requestElement.getAttribute("https"), "true", "A request was sent without HTTPS.");
        	}
        }
    }
    
    /**
     * Conformance Test: Working on Capabilities with no Content section
     * Requirements Class: https://www.opengis.net/def/security/1.0/cr/clientParsing/3
     * 
     * This is a manual test that must be verified by the test operator.
     */
    @Test(description = "Capabilities document with no Content section.\n"
    		+ "Requirements Class: https://www.opengis.net/def/security/1.0/cr/clientParsing/3")
    public void noContentSectionCapabilities() {
    	// Print out Client Conformance Test Checklist for human operator
    	throw new SkipException("Please see site documentation page for \"Test Suite\" and apply section "
    			+ "\"Manual Checklist: Capabilities with No Content Section\"");
    }
}
