package org.opengis.cite.securityclient10;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * A supporting base class that sets up a common test fixture. These
 * configuration methods are invoked before those defined in a subclass.
 */
public class CommonFixture {

    /**
     * Root test suite package (absolute path).
     */
    public static final String ROOT_PKG_PATH = "/org/opengis/cite/securityclient10/";
    
    /**
     * DOM Document with secure client request data, see 
     * {@link org.opengis.cite.securityclient10.httpServer.RequestRepresenter} for more details. 
     */
    protected Document testRequestSet;
    
    /**
     * Service type string from test run properties
     */
    protected String testServiceType;

    /**
     * Initializes the common test fixture with a client component for 
     * interacting with HTTP endpoints.
     *
     * @param testContext The test context that contains all the information for
     * a test run, including suite attributes.
     */
    @BeforeClass
    public void initCommonFixture(ITestContext testContext) {
        Object obj = testContext.getSuite().getAttribute(SuiteAttribute.TEST_SERVICE_TYPE.getName());
        if (null == obj) {
            throw new SkipException("Test service type not found in ITestContext.");
        }
    }
    
    /**
     * Obtain the Implementation Under Test (IUT) from the ISuite context. This is a path to a file
     * containing the serialized request(s) made by the secure client. See 
     * {@link org.opengis.cite.securityclient10.httpServer.RequestRepresenter} for XML structure details.
     * 
     * @param testContext The test (group) context.
     * @throws ParserConfigurationException Could not create new document builder
     */
    @BeforeClass
    public void obtainIUT(ITestContext testContext) throws ParserConfigurationException {
        Object obj = testContext.getSuite().getAttribute(SuiteAttribute.TEST_IUT.getName());
        if (null != obj) {
            String iutPath = String.class.cast(obj);
            File iutFile = new File(iutPath);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            try {
				this.testRequestSet = db.parse(iutFile);
			} catch (SAXException | IOException e) {
				// If input file could not be parsed
				e.printStackTrace();
				this.testRequestSet = db.newDocument();
			}
        }
    }
    
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
}
