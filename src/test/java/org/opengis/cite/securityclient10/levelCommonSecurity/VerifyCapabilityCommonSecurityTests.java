package org.opengis.cite.securityclient10.levelCommonSecurity;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.securityclient10.levelCommonSecurity.CapabilityCommonSecurityTests;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testng.ISuite;
import org.testng.ITestContext;

/**
 * Verifies the behavior of the CapabilityCommonSecurityTests test class. Test stubs replace
 * fixture constituents where appropriate.
 */
public class VerifyCapabilityCommonSecurityTests {

    private static DocumentBuilder docBuilder;
    private static ITestContext testContext;
    private static ISuite suite;

    public VerifyCapabilityCommonSecurityTests() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        testContext = mock(ITestContext.class);
        suite = mock(ISuite.class);
        when(testContext.getSuite()).thenReturn(suite);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        docBuilder = dbf.newDocumentBuilder();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test(expected = AssertionError.class)
    public void testIsEmpty() {
        CapabilityCommonSecurityTests iut = new CapabilityCommonSecurityTests();
        iut.isEmpty();
    }

    @Test
    public void testTrim() {
        CapabilityCommonSecurityTests iut = new CapabilityCommonSecurityTests();
        iut.trim();
    }
}
