package org.opengis.cite.securityclient10;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Matchers;
import org.testng.ISuite;
import org.testng.xml.XmlSuite;

public class VerifySuiteFixtureListener {

    private static XmlSuite xmlSuite;
    private static ISuite suite;

    public VerifySuiteFixtureListener() {
    }

    @BeforeClass
    public static void setUpClass() {
        xmlSuite = mock(XmlSuite.class);
        suite = mock(ISuite.class);
        when(suite.getXmlSuite()).thenReturn(xmlSuite);
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test(expected = IllegalArgumentException.class)
    public void noSuiteParameters() {
        Map<String, String> params = new HashMap<String, String>();
        when(xmlSuite.getParameters()).thenReturn(params);
        SuiteFixtureListener iut = new SuiteFixtureListener();
        iut.onStart(suite);
    }
    
    @Test
    public void processServiceTypeParameter() {
    	String serviceType = "wms111";
    	Map<String, String> params = new HashMap<String, String>();
    	params.put(TestRunArg.Service_Type.toString(), serviceType);
    	params.put(TestRunArg.Address.toString(), "127.0.0.1");
    	params.put(TestRunArg.Port.toString(), "10080");
    	params.put(TestRunArg.Host.toString(), "localhost");
    	params.put(TestRunArg.JKS_Path.toString(), "src/main/resources/security.jks");
    	params.put(TestRunArg.JKS_Password.toString(), "ets-security-client");
    	when(xmlSuite.getParameters()).thenReturn(params);
    	SuiteFixtureListener iut = new SuiteFixtureListener();
        iut.onStart(suite);
        verify(suite).setAttribute(
                Matchers.eq(SuiteAttribute.TEST_SERVICE_TYPE.getName()), 
                Matchers.isA(String.class));
    }

}
