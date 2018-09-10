package org.opengis.cite.securityclient10.levelCommonSecurity;

import org.opengis.cite.securityclient10.CommonFixture;
import org.testng.Assert;
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
    @Test(description = "Connection(s) made using HTTPS")
    public void isHTTPS() {
        NodeList requests = this.testRequestSet.getElementsByTagName("Request");
        
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
    @Test(description = "Capabilities document with no Content section")
    public void noContentSectionCapabilities() {
    	// Print out Client Conformance Test Checklist for human operator
        System.out.println("\nClient Conformance Test **Manual** Checklist");
        System.out.println("=================================");
        System.out.println("1. Conformance Test \"HTTPS\"");
        System.out.println("Does the client parse the HTTPS response from the test suite?");
        System.out.println("YES - Pass");
        System.out.println("NO  - Failure");
        System.out.println("");
        System.out.println("2. Conformance Test \"Working on Capabilities with no Content section\"");
        System.out.println("Does the test suite Capabilities contain a <Content> section?");
        System.out.println("(Note that the \"Content\" section is represented as \"<Layer>\" in WMS,");
        System.out.println("see Section 8.2.2 of v1.0 of the specification for other services.)");
        System.out.println("YES - Skip Test");
        System.out.println("NO  - Continue");
        System.out.println("Does the test suite Capabilities contain an endpoint URL for the GetCapabilities operation?");
        System.out.println("YES - Continue");
        System.out.println("NO  - Failure");
        System.out.println("Does the GetCapabilities operation on that endpoint URL succeed?");
        System.out.println("YES - Continue");
        System.out.println("NO  - Failure");
        System.out.println("Does the Capabilities document contain a <Content> section?");
        System.out.println("YES - Pass");
        System.out.println("NO  - Failure");
        System.out.println("");
        System.out.println("If either Conformance Test went to a \"Failure\" option, then that Test should be marked as a \"Failure\".");
    }
}
