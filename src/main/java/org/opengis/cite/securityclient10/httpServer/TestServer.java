package org.opengis.cite.securityclient10.httpServer;

import java.io.IOException;
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

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class TestServer {
	
	private static volatile Boolean blockingForRequest;
	private int serverPort;
	private Server server;
	
	@SuppressWarnings("serial")
	public static class TestAsyncServlet extends HttpServlet {
		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            final AsyncContext ctxt = req.startAsync();
            ctxt.start(new Runnable() {
                @Override
                public void run() {
                    System.err.println("Request received.");
                    blockingForRequest = false;
                    ctxt.complete();
                }
            });
        }
	}

	public TestServer(String host, int port) throws Exception {
		serverPort = port;
		server = new Server();
		server.setStopAtShutdown(true);
		server.setStopTimeout(1);
		
		// Use a ServerConnector so we can force the host address and port 
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		connector.setHost(host);
		server.setConnectors(new Connector[] { connector });
		
		ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        ServletHolder asyncHolder = context.addServlet(TestAsyncServlet.class, "/test");
        asyncHolder.setAsyncSupported(true);
        server.setHandler(context);
		server.start();
	}
	
	public int getPort() {
		return serverPort;
	}
	
	public void shutdown() throws Exception {
		server.stop();
	}
	
	/**
	 * Block the thread until a request is received, or the
	 * timeout is hit.
	 * @throws InterruptedException 
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 */
	public void waitForRequest() throws InterruptedException, ExecutionException, TimeoutException {
		blockingForRequest = true;
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<String> future = executor.submit(new WaitTask());
		
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
	
	class WaitTask implements Callable<String> {
		@Override
		public String call() throws Exception {
			System.err.println("Waiting…");
			while (blockingForRequest) {
				Thread.sleep(1000);
			}
			return "";
		}
	}
}

