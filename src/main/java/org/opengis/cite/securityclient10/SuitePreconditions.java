package org.opengis.cite.securityclient10;

import java.util.logging.Logger;

import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;

/**
 * Checks that various preconditions are satisfied before the test suite is run.
 * If any of these (BeforeSuite) methods fail, all tests will be skipped.
 */
public class SuitePreconditions {

    private static final Logger LOGR = Logger.getLogger(SuitePreconditions.class.getName());

    /**
     * Verifies that the referenced test subject exists and has the expected
     * type.
     *
     * @param testContext
     *            Information about the (pending) test run.
     */
    @BeforeSuite
    public void verifyTestSubject(ITestContext testContext) {}
}
