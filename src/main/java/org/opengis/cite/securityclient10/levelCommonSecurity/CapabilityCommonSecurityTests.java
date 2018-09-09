package org.opengis.cite.securityclient10.levelCommonSecurity;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.opengis.cite.securityclient10.CommonFixture;
import org.opengis.cite.securityclient10.SuiteAttribute;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Includes various tests of Common Security capability.
 */
public class CapabilityCommonSecurityTests extends CommonFixture {

	private Document testRequestSet;
    private String testServiceType;

    /**
     * Obtains the test service type from the ISuite context. The suite attribute
     * {@link org.opengis.cite.securityclient10.SuiteAttribute#TEST_SERVICE_TYPE} should
     * evaluate to a string representing an OGC Web Service type.
     * 
     * This method can be used to check if this set of tests should apply to the service type
     * as the service type implies a specific conformance class.
     * 
     * @param testContext
     *            The test (group) context.
     */
    @BeforeClass
    public void obtainTestServiceType(ITestContext testContext) {
        Object obj = testContext.getSuite().getAttribute(SuiteAttribute.TEST_SERVICE_TYPE.getName());
        if (null != obj) {
            this.testServiceType = String.class.cast(obj);
        }
    }
    
    /**
     * Obtain the Implementation Under Test (IUT) from the ISuite context. This is a path to a file
     * containing the serialized request(s) made by the secure client. See 
     * org.opengis.cite.securityclient10.httpServer.RequestRepresenter for XML structure details.
     * 
     * @param testContext The test (group) context.
     * @throws ParserConfigurationException Exception for failure to create a DocumentBuilder.
     * @throws IOException Exception when reading the IUT file from disk.
     * @throws SAXException Exception when parsing the IUT file.
     */
    @BeforeClass
    public void obtainIUT(ITestContext testContext) throws ParserConfigurationException, SAXException, IOException {
        Object obj = testContext.getSuite().getAttribute(SuiteAttribute.TEST_IUT.getName());
        if (null != obj) {
            String iutPath = String.class.cast(obj);
            File iutFile = new File(iutPath);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            this.testRequestSet = db.parse(iutFile);
        }
    }

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
}
