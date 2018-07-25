package org.opengis.cite.securityclient10.httpServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.pathmap.MappedResource;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

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
	private static volatile HashMap<String, Boolean> handlerBlocks;
	
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
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            final AsyncContext ctxt = req.startAsync();
            // Remove the leading slash from the path to determine the nonce
            String path = req.getServletPath().substring(1);
            ctxt.start(new Runnable() {
                @Override
                public void run() {
                    System.err.println("Request received.");
                    handlerBlocks.put(path, false);
                    ctxt.complete();
                }
            });
        }
	}

	/**
	 * @param host String of host interface to bind
	 * @param port Integer of port to bind
	 * @throws Exception for any errors starting the embedded Jetty server
	 */
	public TestServer(String host, int port) throws Exception {
		handlerBlocks = new HashMap<String, Boolean>();
		serverPort = port;
		
		jettyServer = new Server();
		jettyServer.setStopAtShutdown(true);
		jettyServer.setStopTimeout(1);
		
		// Use a ServerConnector so we can force the host address and port 
		ServerConnector connector = new ServerConnector(jettyServer);
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
	 * Create a new ServletContextHandler for the given `path`, and create a shared handler block boolean.
	 * The boolean will be used by the waitForRequest thread to delay until the request is fulfilled or a
	 * timeout is hit.
	 * 
	 * @param path HTTP path to dynamically add to the embedded server
	 */
	public void registerHandler(String path) {
		handlerBlocks.put(path, true);

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
		handlerBlocks.put(nonce, true);
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<String> future = executor.submit(new WaitTask(nonce));
		
		try {
            System.out.println("Started wait.");
            System.out.println(future.get(120, TimeUnit.SECONDS));
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
			System.err.println("Waiting…");
			while (handlerBlocks.get(nonce)) {
				Thread.sleep(1000);
			}
			return "";
		}
	}
}

