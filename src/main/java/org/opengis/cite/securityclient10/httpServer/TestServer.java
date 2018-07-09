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
import javax.servlet.Servlet;
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

public class TestServer {
	
	private int serverPort;
	private Server server;
	private ContextHandlerCollection serverHandlers;
	
	// Use a HashMap to track which servlet handlers are waiting for test requests
	private static volatile HashMap<String, Boolean> handlerBlocks;
	
	// Use a servlet class to catch test requests
	@SuppressWarnings("serial")
	public static class TestAsyncServlet extends HttpServlet {
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            final AsyncContext ctxt = req.startAsync();
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

	public TestServer(String host, int port) throws Exception {
		handlerBlocks = new HashMap<String, Boolean>();
		serverPort = port;
		
		server = new Server();
		server.setStopAtShutdown(true);
		server.setStopTimeout(1);
		
		// Use a ServerConnector so we can force the host address and port 
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		connector.setHost(host);
		server.setConnectors(new Connector[] { connector });
		
		// Use ContextHandlerCollection to add contexts/handlers *after* the server has been started
		serverHandlers = new ContextHandlerCollection();
		server.setHandler(serverHandlers);
		
		server.start();
	}
	
	public int getPort() {
		return serverPort;
	}
	
	public void registerHandler(String path) throws Exception {
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
	
	public void shutdown() throws Exception {
		server.stop();
	}
	
	/**
	 * Iterate through the registered ServerContextHandler instances on this server for the first one to
	 * have a mapping matching `path`, and if found then that ServerContextHandler is removed. If no match
	 * is found then nothing is done.
	 * 
	 * @param path
	 * @throws ServletException
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
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 */
	public void waitForRequest(String nonce) throws InterruptedException, ExecutionException, TimeoutException {
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
	
	// Thread class for delaying until a servlet request has been made.
	// TODO: Return the request data to waitForRequest
	class WaitTask implements Callable<String> {
		private String nonce;
		
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

