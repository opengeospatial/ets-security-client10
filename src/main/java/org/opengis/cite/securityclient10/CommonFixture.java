package org.opengis.cite.securityclient10;

import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

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
}
