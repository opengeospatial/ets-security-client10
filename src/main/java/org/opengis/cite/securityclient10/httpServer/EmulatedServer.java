package org.opengis.cite.securityclient10.httpServer;

import java.io.IOException;

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
	
	public EmulatedServer() throws ParserConfigurationException {
		// Create factories and builders and re-use them
		this.documentFactory = DocumentBuilderFactory.newInstance();
		this.documentFactory.setNamespaceAware(true);
		this.documentBuilder = documentFactory.newDocumentBuilder();
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
