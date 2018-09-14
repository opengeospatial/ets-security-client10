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
 * @author jpbadger
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
	 * @author jpbadger
	 *
	 */
	@SuppressWarnings("serial")
	public static class TestAsyncServlet extends HttpServlet {
		/**
		 * Override the reception of GET requests
		 */
		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            final AsyncContext ctxt = request.startAsync();
            // Remove the leading slash from the path to determine the nonce
            String path = request.getServletPath().substring(1);
            ctxt.start(new Runnable() {
                private EmulatedServer emulated;

				@Override
                public void run() {
                    System.out.println("Request received.");
                    
                    // Log request details
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
                    
                    HandlerOptions options = handlerBlocks.get(path);
                    
                    // Save request
                    options.saveRequest(request);
                    
                    // Return the proper document to the client
                    if (options.getServiceType().equals("wms111")) {
                    	emulated = new ServerWMS111();
                    }
                    
                    try {
						emulated.handleRequest(request, response);
					} catch (IOException e) {
						// When an IO Exception occurs trying to build a response
						e.printStackTrace();
					}
                    
                    // Mark path handler as no longer waiting for request
                    options.setReceived(true);
                    
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
	 * Retrieve the HTTP Servlet Request objects for a registered nonce
	 * 
	 * @param nonce String of the nonce to retrieve from the stored Handlers
	 * @return Zero or more HttpServletRequests
	 */
	public RequestRepresenter getRequests(String nonce) {
		HandlerOptions options = handlerBlocks.get(nonce);
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
	 * 
	 * @param path HTTP path to dynamically add to the embedded server
	 * @param serviceType String representing the type of OWS to emulate, will determine which capabilities
	 * 							 document will be presented to the client
	 */
	public void registerHandler(String path, String serviceType) {
		HandlerOptions options = new HandlerOptions(serviceType);
		handlerBlocks.put(path, options);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        ServletHolder asyncHolder = context.addServlet(TestAsyncServlet.class, "/" + path);
        asyncHolder.setAsyncSupported(true);
        serverHandlers.addHandler(context);
        
        try {
        	context.start();
        } catch (Exception e) {
        	System.err.println("Exception starting servlet context handler.");
        }
	}
	
	/**
	 * Shut down the embedded server. This will force close any open HTTP connections.
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
	 * @param nonce The unique code associated with the thread created to wait for the servlet activation
	 * @throws InterruptedException For any errors caused by the waiting thread being interrupted 
	 * @throws ExecutionException For any errors caused by the waiting thread having an exception
	 */
	public void waitForRequest(String nonce) throws InterruptedException, ExecutionException {
		HandlerOptions options = handlerBlocks.get(nonce);
		options.setReceived(false);
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<String> future = executor.submit(new WaitTask(nonce));
		
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
	 * @author jpbadger
	 *
	 * TODO: Return the request data to waitForRequest
	 */
	class WaitTask implements Callable<String> {
		private String nonce;
		
		/**
		 * @param nonce The unique code associated with this waiting thread task and the servlet
		 */
		public WaitTask(String nonce) {
			this.nonce = nonce;
		}

		@Override
		public String call() throws Exception {
			System.out.println("Waiting…");
			HandlerOptions options = handlerBlocks.get(nonce);
			while (!options.getReceived()) {
				Thread.sleep(1000);
			}
			return "";
		}
	}
}

