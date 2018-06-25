package org.opengis.cite.securityclient10.level1;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.testng.ISuite;
import org.testng.ITestContext;

/**
 * Verifies the behavior of the Capability1Tests test class. Test stubs replace
 * fixture constituents where appropriate.
 */
public class VerifyCapability1Tests {

    private static DocumentBuilder docBuilder;
    private static ITestContext testContext;
    private static ISuite suite;

    public VerifyCapability1Tests() {
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
        Capability1Tests iut = new Capability1Tests();
        iut.isEmpty();
    }

    @Test
    public void testTrim() {
        Capability1Tests iut = new Capability1Tests();
        iut.trim();
    }
}
