package org.opengis.cite.securityclient10;

import java.util.Map;
import java.util.logging.Level;

import org.opengis.cite.securityclient10.util.TestSuiteLogger;
import org.testng.ISuite;
import org.testng.ISuiteListener;

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
    }

    @Override
    public void onFinish(ISuite suite) {}

    /**
     * Processes test suite arguments and sets suite attributes accordingly.
     * 
     * @param suite
     *            An ISuite object representing a TestNG test suite.
     */
	void processSuiteParameters(ISuite suite) {
        Map<String, String> params = suite.getXmlSuite().getParameters();
        TestSuiteLogger.log(Level.CONFIG, "Suite parameters\n" + params.toString());
        
        suite.setAttribute(SuiteAttribute.TEST_SERVICE_TYPE.getName(), 
        		params.get(TestRunArg.Service_Type.toString()));
        
        suite.setAttribute(SuiteAttribute.TEST_ADDRESS.getName(), 
        		params.get(TestRunArg.Address.toString()));
        
        suite.setAttribute(SuiteAttribute.TEST_PORT.getName(), 
        		params.get(TestRunArg.Port.toString()));
        
        suite.setAttribute(SuiteAttribute.TEST_HOST.getName(), 
        		params.get(TestRunArg.Host.toString()));
        
        suite.setAttribute(SuiteAttribute.TEST_PATH.getName(), 
        		params.get(TestRunArg.Path.toString()));
        
        suite.setAttribute(SuiteAttribute.TEST_AUTHENTICATION.getName(), 
        		params.get(TestRunArg.Authentication.toString()));
        
        suite.setAttribute(SuiteAttribute.TEST_IDP_URL.getName(), 
        		params.get(TestRunArg.IDP_URL.toString()));
        
        suite.setAttribute(SuiteAttribute.TEST_HTTP_METHODS.getName(), 
        		params.get(TestRunArg.HTTP_METHODS.toString()));
        
        suite.setAttribute(SuiteAttribute.TEST_IUT.getName(), 
        		params.get(TestRunArg.IUT.toString()));
    }
}
