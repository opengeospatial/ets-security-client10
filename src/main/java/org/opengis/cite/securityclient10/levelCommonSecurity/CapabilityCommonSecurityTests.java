package org.opengis.cite.securityclient10.levelCommonSecurity;

import org.opengis.cite.securityclient10.CommonFixture;
import org.opengis.cite.securityclient10.ErrorMessage;
import org.opengis.cite.securityclient10.ErrorMessageKeys;
import org.opengis.cite.securityclient10.SuiteAttribute;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Includes various tests of Common Security capability.
 */
public class CapabilityCommonSecurityTests extends CommonFixture {

    private String testServiceType;

    /**
     * Obtains the test service type from the ISuite context. The suite attribute
     * {@link org.opengis.cite.securityclient10.SuiteAttribute#TEST_SERVICE_TYPE} should
     * evaluate to a string representing an OGC Web Service type.
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
     * Sets the test service type. This method is intended to facilitate unit
     * testing.
     *
     * @param testServiceType A String representing an OGC Web Service type.
     */
    public void setTestServiceType(String testServiceType) {
        this.testServiceType = testServiceType;
    }

    /**
     * Verifies the string is empty.
     */
    @Test(description = "Implements ATC 1-1")
    public void isEmpty() {
        String str = "  foo   ";
        Assert.assertTrue(str.isEmpty(),
                ErrorMessage.get(ErrorMessageKeys.EMPTY_STRING));
    }

    /**
     * Checks the behavior of the trim function.
     */
    @Test(description = "Implements ATC 1-2")
    public void trim() {
        String str = "  foo   ";
        Assert.assertTrue("foo".equals(str.trim()));
    }
}
