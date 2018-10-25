package org.opengis.cite.securityclient10.httpServer;

import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.opengis.cite.servlet.http.HttpServletRequest;
import org.opengis.cite.servlet.http.HttpServletResponse;

public class EmulatedServer {

	protected DocumentBuilderFactory documentFactory;
	protected DocumentBuilder documentBuilder;
	
	/**
	 * Test Run Properties for this emulated server
	 */
	protected ServerOptions options;
	
	/**
	 * RelayState token for SAML2
	 */
	protected String relayState;
	
	public EmulatedServer() {
		// Create factories and builders and re-use them
		try {
			this.documentFactory = DocumentBuilderFactory.newInstance();
			this.documentFactory.setNamespaceAware(true);
			this.documentBuilder = documentFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// Exception with default configuration
			e.printStackTrace();
		}
		
		// RelayState is used for SAML2, we will hard-code something as we
		// are only testing.
		this.relayState = "token";
	}
	
	/**
	 * Enable Cross Origin Resource Sharing (CORS). This allows web-based
	 * clients to connect to this test server from their web browser
	 * security environment.
	 * @param response Reponse on which to enable CORS headers
	 */
	protected void enableCors(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
	}
	
	/**
	 * Return true if authentication has been set to SAML2 or another valid value in the test server 
	 * options. Returns false if no authentication method has been enabled.
	 * @return Boolean
	 */
	protected boolean getAuthenticationEnabled() {
		return this.options.getAuthentication() != null;
	}
	
	/**
	 * Get the writer for a HttpServletResponse, so we can send back a body
	 * @param response HttpServletResponse for the client
	 * @return PrintWriter for response body
	 */
	protected PrintWriter getWriterForResponse(HttpServletResponse response) {
		PrintWriter printWriter = null;
		try {
			printWriter = response.getWriter();
		} catch (IOException e) {
			// Exception if writer could not be created
			e.printStackTrace();
		}
		return printWriter;
	}
	
	/**
	 * Extract the uri from a request object.
	 * If contextOnly is true, then any path segments after the context path are excluded.
	 * 
	 * @param request The request to extract
	 * @param contextOnly Only include up to the context path
	 * @return The uri
	 */
	protected static String getUri(HttpServletRequest request, Boolean contextOnly) {
		String path;
		if (contextOnly) {
			path = "/" + request.getRequestURI().split("/")[1];
		} else {
			path = request.getRequestURI();
		}
		
		return String.format("%s://%s:%d%s",
				request.getScheme(),
				request.getServerName(),
				request.getServerPort(),
				path);
	}
	
	/**
	 * Subclasses must override this
	 * @param request Request from client
	 * @param response Response to build to send back to client
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, TransformerException {
		// TODO Auto-generated method stub
	}

}
