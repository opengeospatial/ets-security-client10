package org.opengis.cite.securityclient10.httpServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.opengis.cite.servlet.AsyncContext;
import org.opengis.cite.servlet.ServletException;
import org.opengis.cite.servlet.http.HttpServlet;
import org.opengis.cite.servlet.http.HttpServletRequest;
import org.opengis.cite.servlet.http.HttpServletResponse;

import org.opengis.cite.jetty.http.HttpVersion;
import org.opengis.cite.jetty.http.pathmap.MappedResource;
import org.opengis.cite.jetty.server.Connector;
import org.opengis.cite.jetty.server.Handler;
import org.opengis.cite.jetty.server.HttpConfiguration;
import org.opengis.cite.jetty.server.HttpConnectionFactory;
import org.opengis.cite.jetty.server.SecureRequestCustomizer;
import org.opengis.cite.jetty.server.Server;
import org.opengis.cite.jetty.server.ServerConnector;
import org.opengis.cite.jetty.server.handler.ContextHandlerCollection;
import org.opengis.cite.jetty.servlet.ServletContextHandler;
import org.opengis.cite.jetty.servlet.ServletHandler;
import org.opengis.cite.jetty.servlet.ServletHolder;
import org.opengis.cite.jetty.util.ssl.SslContextFactory;

/**
 * A wrapper class around the Jetty Server class. This adds functionality for the testing suite for
 * adding/removing servlets to capture test client requests.
 *
 */
public class TestServer {
	
	private int serverPort;
	private Server jettyServer;
	
	/**
	 * A ContextHandlerCollection is used to dynamically add and remove servlets from the running
	 * embedded server. (Otherwise the server would have to be stopped before new servlets could be
	 * added.)
	 */
	private ContextHandlerCollection serverHandlers;
	
	/**
	 * Use a HashMap to track which servlet handlers are waiting for test requests.
	 * When unfulfilled, the Boolean will be true. It is set to false in the TestAsyncServlet class.
	 */
	private static volatile HashMap<String, HandlerOptions> handlerBlocks;
	
	/**
	 * Use a servlet class to capture requests from the secure client. The path that is requested by the
	 * client corresponds to the test session created in TestNGController; in the parent class here
	 * (TestServer) the path is used to track if a request is fulfilled using the handlerBlocks HashMap.
	 *
	 */
	@SuppressWarnings("serial")
	public static class TestAsyncServlet extends HttpServlet {
		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response) throws 
			ServletException, IOException {
			handleRequest(request, response);
		}
		
		@Override
		protected void doPost(HttpServletRequest request, HttpServletResponse response) throws 
			ServletException, IOException {
			handleRequest(request, response);
		}
		
		@Override
		protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws 
			ServletException, IOException {
			handleRequest(request, response);
		}
		
		protected void handleRequest(HttpServletRequest request, HttpServletResponse response) throws 
			ServletException, IOException {
            final AsyncContext ctxt = request.startAsync();
            // Remove the leading slash from the path to determine the registered path
            String path = request.getServletPath().substring(1);
            
            ctxt.start(new Runnable() {
                private EmulatedServer emulated;

				@Override
                public void run() {
                    System.out.println("Request received.");
                    
                    // Log request details
                    System.out.printf("Path: %s\n", request.getPathInfo());
                    System.out.printf("HTTP Method: %s\n", request.getMethod());
                    System.out.printf("Is Secure: %s\n", request.isSecure() ? "true" : "false");
                    System.out.printf("Query String: %s\n", request.getQueryString());
                    System.out.printf("Auth Type: %s\n", request.getAuthType());
                    
                    System.out.println("Request Headers:");
                    for (Enumeration<String> headers = request.getHeaderNames(); headers.hasMoreElements();) {
                    	String headerName = headers.nextElement();
                    	String headerValue = request.getHeader(headerName);
                    	System.out.printf("%s: %s\n", headerName, headerValue);
                    }
                    
                    System.out.println();
                    
                    // Split out nonce from the path
                    String nonce = path.split("/")[0];
                    HandlerOptions options = handlerBlocks.get(nonce);
                    
                    // Save request
                    try {
						options.saveRequest(request);
					} catch (IOException e1) {
						// When the request could not be serialized, ignore it and leave the 
						// document empty.
						e1.printStackTrace();
					}
                    
                    // Return the proper document to the client
                    ServerOptions serverOptions = options.getServerOptions();
                    String serviceType = serverOptions.getServiceType();
                    try {
						if (serviceType.equals("wms111")) {
                    		emulated = new ServerWms111(serverOptions);
                        } else if (serviceType.equals("wms13")) {
                        	emulated = new ServerWms13(serverOptions);
                        } else if (serviceType.equals("wps20")) {
                        	emulated = new ServerWps20(serverOptions);
                        } else {
                        	System.err.println("Unknown service type for emulation: " + serviceType);
                        }
                    } catch (TransformerConfigurationException | ParserConfigurationException e) {
						emulated = null;
					}
                    
                    if (emulated != null) {
                    	try {
    						emulated.handleRequest(request, response);
    					} catch (IOException | TransformerException e) {
    						// When an IO Exception occurs trying to build a response
    						e.printStackTrace();
    					}
                    }
                    
                    // Ask ServerOptions for the number of expected requests for the authentication type
                    if (options.getRequestCount() == serverOptions.getExpectedRequestCount()) {
	                    // Mark path handler as no longer waiting for request
	                    options.setReceived(true);	                    
                    }
                    ctxt.complete();
                }
            });
        }
	}

	/**
	 * @param host String of host interface to bind
	 * @param port Integer of port to bind
	 * @param jks_path Path to the Java KeyStore
     * @param jks_password Password to unlock the KeyStore 
	 * @throws Exception for any errors starting the embedded Jetty server
	 */
	public TestServer(String host, int port, String jks_path, String jks_password) throws Exception {
		handlerBlocks = new HashMap<String, HandlerOptions>();
		serverPort = port;
		
		jettyServer = new Server();
		jettyServer.setStopAtShutdown(true);
		jettyServer.setStopTimeout(1);
		
		// Set up HTTPS
		// Check for Keystore
		File keystore = new File(jks_path);
		if (!keystore.exists()) {
			throw new FileNotFoundException("Missing keystore: " + keystore.getAbsolutePath());
		}
		
		SslContextFactory sslContextFactory = new SslContextFactory();
		sslContextFactory.setKeyStorePath(keystore.getAbsolutePath());
		sslContextFactory.setKeyStorePassword(jks_password);
		sslContextFactory.setTrustStorePath(keystore.getAbsolutePath());
		sslContextFactory.setTrustStorePassword(jks_password);
		
		HttpConfiguration httpsConfig = new HttpConfiguration();
		httpsConfig.setSecureScheme("https");
		httpsConfig.setSecurePort(port);
		httpsConfig.addCustomizer(new SecureRequestCustomizer());
		
		// Use a ServerConnector so we can force the host address and port 
		ServerConnector connector = new ServerConnector(jettyServer,
				new UnifiedSslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
				new HttpConnectionFactory(httpsConfig));
		connector.setPort(port);
		connector.setHost(host);
		jettyServer.setConnectors(new Connector[] { connector });
		
		// Use ContextHandlerCollection to add contexts/handlers *after* the server has been started
		serverHandlers = new ContextHandlerCollection();
		jettyServer.setHandler(serverHandlers);
		
		jettyServer.start();
	}
	
	/**
	 * Return the port the server is currently using.
	 * 
	 * @return int Current port being used by the embedded server
	 */
	public int getPort() {
		return serverPort;
	}
	
	/**
	 * Retrieve the HTTP Servlet Request objects for a registered path
	 * 
	 * @param path String of the path to retrieve from the stored Handlers
	 * @return Zero or more HttpServletRequests
	 */
	public RequestRepresenter getRequests(String path) {
		HandlerOptions options = handlerBlocks.get(path);
		if (options == null) {
			return null;
		} else {
			return options.getRequests();
		}
	}
	
	/**
	 * Create a new ServletContextHandler for the given `path`, and create a shared handler block boolean.
	 * The boolean will be used by the waitForRequest thread to delay until the request is fulfilled or a
	 * timeout is hit.
	 * The Handler will catch requests to `path`, and to `path/*` where `*` is a wildcard.
	 * 
	 * @param path HTTP path to dynamically add to the embedded server
	 * @param serverOptions ServerOptions Object with options for the type of OWS to emulate, will 
	 * determine which capabilities document will be presented to the client
	 * @throws Exception Exception if context could not be started
	 */
	public void registerHandler(String path, ServerOptions serverOptions) throws Exception {
		HandlerOptions options = new HandlerOptions(serverOptions);
		handlerBlocks.put(path, options);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        ServletHolder baseHolder = context.addServlet(TestAsyncServlet.class, "/" + path);
        baseHolder.setAsyncSupported(true);
        ServletHolder wildcardHolder = context.addServlet(TestAsyncServlet.class, "/" + path + "/*");
        wildcardHolder.setAsyncSupported(true);
        serverHandlers.addHandler(context);
        
        context.start();
	}
	
	/**
	 * Shut down the embedded server. This will force close any open HTTP connections.
	 * 
	 * @throws Exception for any errors shutting down the embedded server
	 */
	public void shutdown() throws Exception {
		jettyServer.stop();
	}
	
	/**
	 * Iterate through the registered ServletContextHandler instances on this server for the first one to
	 * have a mapping matching `path`, and if found then that ServletContextHandler is removed. If no match
	 * is found then nothing is done.
	 * 
	 * @param path HTTP path to dynamically remove from the embedded server
	 * @throws ServletException For any errors removing a servlet context handler
	 */
	public void unregisterHandler(String path) throws ServletException {
		Handler[] handlers = serverHandlers.getHandlers();
		
		for (Handler context : handlers) {
			if (context.getClass() == ServletContextHandler.class) {
				ServletHandler servletHandler = ((ServletContextHandler) context).getServletHandler();
				MappedResource<ServletHolder> a = servletHandler.getMappedServlet("/" + path);
				if (a != null) {
					serverHandlers.removeHandler(context);
					break;
				}
			}
		}
	}
	
	/**
	 * Block the thread until a request is received, or the
	 * timeout is hit.
	 * 
	 * @param path The unique code associated with the thread created to wait for the servlet activation
	 * @throws InterruptedException For any errors caused by the waiting thread being interrupted 
	 * @throws ExecutionException For any errors caused by the waiting thread having an exception
	 */
	public void waitForRequest(String path) throws InterruptedException, ExecutionException {
		HandlerOptions options = handlerBlocks.get(path);
		options.setReceived(false);
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<String> future = executor.submit(new WaitTask(path));
		
		try {
            System.out.println("Started wait.");
            System.out.println(future.get(300, TimeUnit.SECONDS));
            System.out.println("Finished!");
        } catch (TimeoutException e) {
            future.cancel(true);
            System.out.println("Terminated!");
        }

        executor.shutdownNow();
	}
	
	/**
	 * Thread class for delaying until a servlet request has been made
	 */
	class WaitTask implements Callable<String> {
		private String path;
		
		/**
		 * @param path The unique path associated with this waiting thread task and the servlet
		 */
		public WaitTask(String path) {
			this.path = path;
		}

		@Override
		public String call() throws Exception {
			System.out.println("Waiting…");
			HandlerOptions options = handlerBlocks.get(path);
			while (!options.getReceived()) {
				Thread.sleep(1000);
			}
			return "";
		}
	}
}

