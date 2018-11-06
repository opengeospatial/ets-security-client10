package org.opengis.cite.securityclient10;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.opengis.cite.securityclient10.httpServer.RequestRepresenter;
import org.opengis.cite.securityclient10.httpServer.ServerOptions;
import org.opengis.cite.securityclient10.httpServer.TestServer;
import org.opengis.cite.securityclient10.util.PropertiesDocument;
import org.opengis.cite.securityclient10.util.TestSuiteLogger;
import org.opengis.cite.servlet.ServletException;
import org.w3c.dom.Document;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.occamlab.te.spi.executors.TestRunExecutor;
import com.occamlab.te.spi.executors.testng.TestNGExecutor;
import com.occamlab.te.spi.jaxrs.TestSuiteController;

/**
 * Main test run controller oversees execution of TestNG test suites.
 */
public class TestNGController implements TestSuiteController {

    private TestRunExecutor executor;
    private Properties etsProperties = new Properties();
    /**
     * A singleton to refer to the HTTP server process, which should be re-used between test sessions.
     * Re-use is only needed for TEAM Engine to run multiple test sessions simultaneously.
     */
    private static volatile TestServer httpServer;

    /**
     * A convenience method for running the test suite using a command-line
     * interface. This method is skipped by TEAM Engine. The default values 
     * of the test run arguments are as follows:
     * <ul>
     * <li>XML properties file: ${user.home}/test-run-props.xml</li>
     * <li>outputDir: ${user.home}</li>
     * <li>deleteSubjectOnFinish: false</li>
     * </ul>
     * <p>
     * <strong>Synopsis</strong>
     * </p>
     * 
     * <pre>
     * ets-*-aio.jar [-o|--outputDir $TMPDIR] [-d|--deleteSubjectOnFinish] [test-run-props.xml]
     * </pre>
     *
     * @param args
     *            Test run arguments (optional). The first argument must refer
	   *            to an XML properties file containing the expected set of test
	   *            run arguments. If no argument is supplied, the file located at
	   *            ${user.home}/test-run-props.xml will be used.
     * @throws Exception
     *             If the test run cannot be executed (usually due to
     *             unsatisfied pre-conditions).
     */
    public static void main(String[] args) throws Exception {
        CommandLineArguments testRunArgs = new CommandLineArguments();
        JCommander cmd = new JCommander(testRunArgs);
        try {
            cmd.parse(args);
        } catch (ParameterException px) {
            System.out.println(px.getMessage());
            cmd.usage();
        }
        if (testRunArgs.doDeleteSubjectOnFinish()) {
            System.setProperty("deleteSubjectOnFinish", "true");
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        File xmlArgs = testRunArgs.getPropertiesFile();
        Document testRunProps = db.parse(xmlArgs);
        TestNGController controller = new TestNGController(testRunArgs.getOutputDir());
        Source testResults = controller.doTestRun(testRunProps);
        System.out.println("Test results: " + testResults.getSystemId());
                
        // Shut down HTTP server
        TestServer server = getServer();
        if (server != null) {
        	server.shutdown();
        }
    }

    /**
     * Default constructor uses the location given by the "java.io.tmpdir"
     * system property as the root output directory.
     */
    public TestNGController() {
        this(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Construct a controller that writes results to the given output directory.
     * 
     * @param outputDir
     *            The location of the directory in which test results will be
     *            written (a file system path or a 'file' URI). It will be
     *            created if it does not exist.
     */
    public TestNGController(String outputDir) {
    	InputStream is = getClass().getResourceAsStream("ets.properties");
        try {
            this.etsProperties.load(is);
        } catch (IOException ex) {
            TestSuiteLogger.log(Level.WARNING, "Unable to load ets.properties. " + ex.getMessage());
        }
        URL tngSuite = TestNGController.class.getResource("testng.xml");
        File resultsDir;
        if (null == outputDir || outputDir.isEmpty()) {
            resultsDir = new File(System.getProperty("user.home"));
        } else if (outputDir.startsWith("file:")) {
            resultsDir = new File(URI.create(outputDir));
        } else {
            resultsDir = new File(outputDir);
        }
        TestSuiteLogger.log(Level.CONFIG, "Using TestNG config: " + tngSuite);
        TestSuiteLogger.log(Level.CONFIG, "Using outputDirPath: " + resultsDir.getAbsolutePath());
        // NOTE: setting third argument to 'true' enables the default listeners
        this.executor = new TestNGExecutor(tngSuite.toString(), resultsDir.getAbsolutePath(), false);
    }

    @Override
    public String getCode() {
        return etsProperties.getProperty("ets-code");
    }
    
    /**
     * Generate a random 16 character string
     * @return String, 16 characters
     */
    public String getNonce() {
        String symbols = "abcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        char[] bytes = new char[16];
        for (int i = 0; i < bytes.length; i++) {
			bytes[i] = symbols.charAt(random.nextInt(symbols.length()));
		}
        return String.valueOf(bytes);
    }
    
    /**
     * Return a reference to the HTTP Server instance. If it has not been initialized (i.e. null) then
     * a new instance is created.
     * 
     * @param address String representing the host interface on which to bind the Test Server
     * @param port Integer representing the port to bind the Test Server
     * @param jks_path Path to the Java KeyStore
     * @param jks_password Password to unlock the KeyStore
     * @return A TestServer instance that is the embedded Jetty web server.
     * @throws Exception Exception if Test Server could not be initialized and started
     */
    public static TestServer getServer(String address, int port, String jks_path, String jks_password) throws Exception {
    	// Use double-checked locking to prevent race condition.
    	if (null == httpServer) {
    		if (httpServer == null) {
    			httpServer = new TestServer(address, port, jks_path, jks_password);
    		}
    	}
    	
    	return httpServer;
    }
    
    public static TestServer getServer() {    	
    	return httpServer;
    }

    @Override
    public String getTitle() {
        return etsProperties.getProperty("ets-title");
    }
    
    @Override
    public String getVersion() {
        return etsProperties.getProperty("ets-version");
    }

    @Override
    public Source doTestRun(Document testRunArgs) throws ParserConfigurationException {
    	// Convert testRunArgs Document to PropertiesDocument
    	PropertiesDocument testRunProperties;
    	try {
			testRunProperties = new PropertiesDocument(testRunArgs);
		} catch (TransformerFactoryConfigurationError e1) {
			// If test run arguments could not be converted to a Properties Document, then
			// skip to tests with an exception.
			e1.printStackTrace();
			return executeWithException(e1);
		}
    	
    	validateTestRunArgs(testRunProperties);
    	
    	// Print out information on which conformance classes will be tested
    	System.out.println("ETS Security Client 1.0 Active Conformance Classes");
    	System.out.println("==================================================");
    	System.out.println("* Abstract Conformance Class Common Security");
    	
    	String serviceType = testRunProperties.getProperty(TestRunArg.Service_Type.toString());
    	
    	String httpMethods = testRunProperties.getProperty(TestRunArg.HTTP_METHODS.toString());
        Boolean hasHttpMethods = (httpMethods != null && httpMethods.equals("true"));
        
        String w3cCors = testRunProperties.getProperty(TestRunArg.W3C_CORS.toString());
        Boolean hasW3CCors = (w3cCors != null && w3cCors.equals("true"));
        
        String httpExceptionHandling = testRunProperties.getProperty(TestRunArg.HTTP_EXCEPTION_HANDLING.toString());
        Boolean hasExceptionHandling = (httpExceptionHandling != null && httpExceptionHandling.equals("true"));
        
        String httpPostContentType = testRunProperties.getProperty(TestRunArg.HTTP_POST_CONTENT_TYPE.toString());
        Boolean hasPostContentType = (httpPostContentType != null && httpPostContentType.equals("true"));
        
        String auth = testRunProperties.getProperty(TestRunArg.Authentication.toString());
        
        // Force-enable HTTP Methods if W3C CORS is enabled
        if (hasW3CCors) {
        	hasHttpMethods = true;
        }
    	
    	if (serviceType.equals("wms111")) {
    		System.out.println("* Conformance Class WMS 1.1.1");
    	} else if (serviceType.equals("wms13")) {
    		System.out.println("* Conformance Class WMS 1.3.0");
    	} else {
    		System.out.println("* Conformance Class OWS Common");
    	}
    	
    	if (hasHttpMethods) {
    		System.out.println("* HTTP Methods annotation enabled");
    	}
    	if (hasW3CCors) {
    		System.out.println("* W3C CORS annotation enabled");
    	}
    	if (hasExceptionHandling) {
    		System.out.println("* HTTP Exception Handling annotation enabled");
    	}
    	if (hasPostContentType) {
    		System.out.println("* HTTP POST Content-Type annotation enabled");
    	}
    	if (auth != null && auth.equals("saml2")) {
    		System.out.println("* SAML 2.0 Authentication Required");
    	}
    	System.out.println("");
        
        TestServer server;
		try {
			server = getServer(testRunProperties.getProperty(TestRunArg.Address.toString()), 
					Integer.parseInt(testRunProperties.getProperty(TestRunArg.Port.toString())),
					testRunProperties.getProperty(TestRunArg.JKS_Path.toString()), 
					testRunProperties.getProperty(TestRunArg.JKS_Password.toString()));
		} catch (Exception e) {
			// If Test Server could not be started, skip to tests
			e.printStackTrace();
			return executeWithException(e);
		}
    	
        String path;
        if (testRunProperties.getProperty(TestRunArg.Path.toString()) == "") {
        	// Generate nonce for this test session, which will be used as the unique servlet address
            path = this.getNonce();
        } else {
        	path = testRunProperties.getProperty(TestRunArg.Path.toString());
        }
        
        // Register a servlet handler with the path, service type, and requirement class options
        ServerOptions serverOptions = new ServerOptions(serviceType);
        serverOptions.setAuthentication(auth);
        serverOptions.setIdpUrl(testRunProperties.getProperty(TestRunArg.IDP_URL.toString()));
        serverOptions.setHttpMethods(hasHttpMethods);
        serverOptions.setCors(hasW3CCors);
        serverOptions.setHttpExceptionHandling(hasExceptionHandling);
        serverOptions.setHttpPostContentType(hasPostContentType);
        
        try {
			server.registerHandler(path, serverOptions);
		} catch (Exception e) {
			// If handler could not be created, skip to tests
			e.printStackTrace();
			return executeWithException(e);
		}
        
        // Print out the servlet test path for the test user
        System.out.println(String.format("Your test session endpoint is at https://%s:%s/%s", 
        		testRunProperties.getProperty(TestRunArg.Host.toString()), 
        		testRunProperties.getProperty(TestRunArg.Port.toString()), path));
    	
    	// Wait for TestServer to receive a request for this test run,
    	// or for the timeout to be reached.
    	try {
			server.waitForRequest(path);
		} catch (InterruptedException | ExecutionException e) {
			// If the waiting thread has any errors, skip to tests
			e.printStackTrace();
			return executeWithException(e);
		}
    	
    	// Retrieve the request(s) from the secure client
    	RequestRepresenter requests = server.getRequests(path);
    	
    	// Save to file
    	String nonce = this.getNonce();
    	Path requestsFilePath = Paths.get(System.getProperty("java.io.tmpdir"), 
    			"requests-" + nonce + ".xml");
    	try {
			requests.saveToPath(requestsFilePath);
		} catch (FileNotFoundException | TransformerException e) {
			// If the requests could not be saved to the temporary file, skip to tests
			e.printStackTrace();
			return executeWithException(e);
		}
    	
    	// Add argument for requests document path as IUT
    	testRunProperties.setProperty(TestRunArg.IUT.toString(), 
    			requestsFilePath.toAbsolutePath().toString());
    	
    	// Release the servlet as the path is not needed anymore
    	try {
			server.unregisterHandler(path);
		} catch (ServletException e) {
			// If the handler could not be unregistered, skip to tests
			e.printStackTrace();
			return executeWithException(e);
		}
    	
    	// Remove sensitive properties from test run properties, so they are not leaked into
    	// the test results.
    	// The JKS has a path that should not be shown.
    	testRunProperties.removeProperty(TestRunArg.JKS_Path.toString());
    	// The JKS password should not be shown.
    	testRunProperties.removeProperty(TestRunArg.JKS_Password.toString());
    	
        return executor.execute(testRunProperties.getDocument());
    }

    /**
     * Run the Test Suite even though an exception stopped full test preparation.
     * @param e Exception raised
     * @return Source object with XML results of test run
     */
	private Source executeWithException(Throwable e) {
		PropertiesDocument testRunProperties;
		testRunProperties = new PropertiesDocument();
		testRunProperties.setProperty(e.getClass().toString(), e.getMessage());
		return executor.execute(testRunProperties.getDocument());
	}

	/**
     * Validates the test run arguments. The test run is aborted if any of these
     * checks fail.
     *
     * @param testRunProperties PropertiesDocument with the test run properties
     * @throws IllegalArgumentException
     *             If any arguments are missing or invalid for some reason.
     */
    private void validateTestRunArgs(PropertiesDocument testRunProperties) {
    	TestRunArgValidator.validateProperties(testRunProperties.getProperties());
    }
}
