package org.opengis.cite.securityclient10;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.xerces.dom.DeferredDocumentImpl;
import org.opengis.cite.securityclient10.httpServer.RequestRepresenter;
import org.opengis.cite.securityclient10.httpServer.TestServer;
import org.opengis.cite.securityclient10.util.TestSuiteLogger;
import org.opengis.cite.servlet.ServletException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
        server.shutdown();
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
    	Map<String, String> args = validateTestRunArgs(testRunArgs);
    	
    	// Print out information on which conformance classes will be tested
    	System.out.println("ETS Security Client 1.0 Active Conformance Classes");
    	System.out.println("==================================================");
    	System.out.println("* Abstract Conformance Class Common Security");
    	
    	String serviceType = args.get(TestRunArg.Service_Type.toString());
    	
    	if (serviceType.equals("wms111")) {
    		System.out.println("* Conformance Class WMS 1.1.1");
    	} else if (serviceType.equals("wms130")) {
    		System.out.println("* Conformance Class WMS 1.3.0");
    	} else {
    		System.out.println("* Conformance Class OWS Common");
    	}
    	System.out.println("");
        
        TestServer server;
		try {
			server = getServer(args.get("address"), Integer.parseInt(args.get("port")),
					args.get("jks_path"), args.get("jks_password"));
		} catch (Exception e) {
			// If Test Server could not be started, skip to tests
			e.printStackTrace();
			return executor.execute(testRunArgs);
		}
    	
        String path;
        if (args.get("path") == "") {
        	// Generate nonce for this test session, which will be used as the unique servlet address
            path = this.getNonce();
        } else {
        	path = args.get("path");
        }
        
        // Register a servlet handler with the path and service type
        try {
			server.registerHandler(path, serviceType);
		} catch (Exception e) {
			// If handler could not be created, skip to tests
			e.printStackTrace();
			return executor.execute(testRunArgs);
		}
        
        // Print out the servlet test path for the test user
        System.out.println(String.format("Your test session endpoint is at https://%s:%s/%s", 
        		args.get("host"), args.get("port"), path));
    	
    	// Wait for TestServer to receive a request for this test run,
    	// or for the timeout to be reached.
    	try {
			server.waitForRequest(path);
		} catch (InterruptedException | ExecutionException e) {
			// If the waiting thread has any errors, skip to tests
			e.printStackTrace();
			return executor.execute(testRunArgs);
		}
    	
    	// Retrieve the request(s) from the secure client
    	RequestRepresenter requests = server.getRequests(path);
    	
    	// Save to file
    	String nonce = this.getNonce();
    	Path requestsFilePath = Paths.get(System.getProperty("java.io.tmpdir"), "requests-" + nonce + ".xml");
    	try {
			requests.saveToPath(requestsFilePath);
		} catch (FileNotFoundException | TransformerException e) {
			// If the requests could not be saved to the temporary file, skip to tests
			e.printStackTrace();
			return executor.execute(testRunArgs);
		}
    	
    	// Add argument for requests document path as IUT
    	args.put("iut", requestsFilePath.toAbsolutePath().toString());
    	
    	// Create new test run arguments document, as input document *may* be read only
    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document testRunProps = db.newDocument();
        
        Element rootElement = testRunProps.createElement("properties");
        testRunProps.appendChild(rootElement);
        
        for (String key : args.keySet()) {
        	// Omit jks_path and jks_password from output properties, to prevent
        	// them from leaking to test users
        	if (!key.equals("jks_path") && !key.equals("jks_password")) {
				Element entry = testRunProps.createElement("entry");
				entry.setAttribute("key", key);
				entry.setTextContent(args.get(key));
				rootElement.appendChild(entry);
        	}
		}
    	
    	// Release the servlet as the path is not needed anymore
    	try {
			server.unregisterHandler(path);
		} catch (ServletException e) {
			// If the handler could not be unregistered, skip to tests
			e.printStackTrace();
			return executor.execute(testRunProps);
		}
    	
        return executor.execute(testRunProps);
    }

	/**
     * Validates the test run arguments. The test run is aborted if any of these
     * checks fail.
     *
     * @param testRunArgs
     *            A DOM Document containing a set of XML properties (key-value
     *            pairs).
     * @return A Map of the test run properties
     * @throws IllegalArgumentException
     *             If any arguments are missing or invalid for some reason.
     */
    Map<String, String> validateTestRunArgs(Document testRunArgs) {
        if (null == testRunArgs || !testRunArgs.getDocumentElement().getNodeName().equals("properties")) {
            throw new IllegalArgumentException("Input is not an XML properties document.");
        }
        NodeList entries = testRunArgs.getDocumentElement().getElementsByTagName("entry");
        if (entries.getLength() == 0) {
            throw new IllegalArgumentException("No test run arguments found.");
        }
        Map<String, String> args = new HashMap<String, String>();
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            args.put(entry.getAttribute("key"), entry.getTextContent());
        }
        
        TestRunArgValidator.validateMap(args);
        
        return args;
    }
}
