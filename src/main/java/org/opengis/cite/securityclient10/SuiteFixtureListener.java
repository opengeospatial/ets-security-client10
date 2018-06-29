package org.opengis.cite.securityclient10;

import java.util.Map;
import java.util.logging.Level;

import org.opengis.cite.securityclient10.util.ClientUtils;
import org.opengis.cite.securityclient10.util.TestSuiteLogger;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import com.sun.jersey.api.client.Client;

/**
 * A listener that performs various tasks before and after a test suite is run,
 * usually concerned with maintaining a shared test suite fixture. Since this
 * listener is loaded using the ServiceLoader mechanism, its methods will be
 * called before those of other suite listeners listed in the test suite
 * definition and before any annotated configuration methods.
 *
 * Attributes set on an ISuite instance are not inherited by constituent test
 * group contexts (ITestContext). However, suite attributes are still accessible
 * from lower contexts.
 *
 * @see org.testng.ISuite ISuite interface
 */
public class SuiteFixtureListener implements ISuiteListener {

    @Override
    public void onStart(ISuite suite) {
        processSuiteParameters(suite);
        registerClientComponent(suite);
    }

    @Override
    public void onFinish(ISuite suite) {}

    /**
     * Processes test suite arguments and sets suite attributes accordingly.
     * 
     * @param suite
     *            An ISuite object representing a TestNG test suite.
     */
    @SuppressWarnings("unlikely-arg-type")
	void processSuiteParameters(ISuite suite) {
        Map<String, String> params = suite.getXmlSuite().getParameters();
        TestSuiteLogger.log(Level.CONFIG, "Suite parameters\n" + params.toString());
        String serviceTypeParam = params.get(TestRunArg.Service_Type.toString());
        
        if ((null == serviceTypeParam) || serviceTypeParam.isEmpty()) {
            throw new IllegalArgumentException("Required test run parameter not found: " + TestRunArg.Service_Type.toString());
        }

        suite.setAttribute(SuiteAttribute.TEST_SERVICE_TYPE.getName(), serviceTypeParam);
        
        String hostParam = params.get(TestRunArg.Host.toString());
        
        if ((null == hostParam) || hostParam.isEmpty()) {
            throw new IllegalArgumentException("Required test run parameter not found: " + TestRunArg.Host.toString());
        }
        
        suite.setAttribute(SuiteAttribute.TEST_HOST.getName(), hostParam);
        
        Integer portParam = Integer.parseInt(params.get(TestRunArg.Port.toString()));
        suite.setAttribute(SuiteAttribute.TEST_PORT.getName(), portParam);
        
        String jksParam = params.get(TestRunArg.JKS_Path.toString());
        
        if ((null == jksParam) || jksParam.isEmpty()) {
            throw new IllegalArgumentException("Required test run parameter not found: " + TestRunArg.JKS_Path.toString());
        }
        
        suite.setAttribute(SuiteAttribute.TEST_JKS_PATH.getName(), jksParam);
    }

    /**
     * A client component is added to the suite fixture as the value of the
     * {@link SuiteAttribute#CLIENT} attribute; it may be subsequently accessed
     * via the {@link org.testng.ITestContext#getSuite()} method.
     *
     * @param suite
     *            The test suite instance.
     */
    void registerClientComponent(ISuite suite) {
        Client client = ClientUtils.buildClient();
        if (null != client) {
            suite.setAttribute(SuiteAttribute.CLIENT.getName(), client);
        }
    }
}
