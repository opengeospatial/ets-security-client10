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
	 * Subclasses must override this.
	 * 
	 * @param request Request from client
	 * @param response Response to build to send back to client
	 * @throws IOException Exception raised when a response writer could not be created
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, TransformerException {
		// Override this method
	}
	
	/**
	 * Subclasses must override this.
	 * 
	 * @param reason String with reason for Service Exception.
	 * @param response Response to build to send back to client
	 */
	private void buildException(String string, HttpServletResponse response) {
		// Override this method
	}
	
	/**
	 * Generate a deflated and base64-encoded representation of a SAML authentication request document.
	 * This is meant to be used as a query parameter for clients to use when authenticating to a SAML 2
	 * Identity Provider.
	 * 
	 * The base path of the request handler is necessary to include the SAML callback URL that is sent to
	 * the Identity Provider.
	 * 
	 * @param href The base path of the request handler
	 * @return String of auth request
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	protected String buildSamlAuthRequest(String href) throws TransformerException {
		SamlAuthRequest request = new SamlAuthRequest(href);
		return request.toUrlParameterString();
	}
	
	/**
	 * Create a security context for the client, returning a response that sets a cookie.
	 * 
	 * @param request The request from the client
	 * @param response The response that will be modified
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	protected void buildSecurityContext(HttpServletRequest request, HttpServletResponse response) throws TransformerException {
		response.setHeader("Set-Cookie", "sessionToken=sample-token; Max-age=600; httpOnly");
	}

	/**
	 * Check if the request body has a valid SAML 2.0 Authentication Response document. For the test suite,
	 * no check is actually done and "true" can always be returned.
	 * 
	 * @param request The request from the client
	 * @param response The response to send to the client
	 * @return If the SAML Authentication response is valid
	 */
	protected boolean validateSamlAuthenticationResponse(HttpServletRequest request, HttpServletResponse response) {
		return true;
	}
	
	/**
	 * Validate a request to a secure resource has a valid Security Context.
	 * In this case, a security context is defined using an HTTP cookie. If the cookie is invalid or 
	 * missing, then respond with a redirect to the Identity Provider (if using SAML2) or Service Exception
	 * (if no authentication defined).
	 * If the cookie is valid, then simply return true, leaving the response alone.
	 * 
	 * @param request The request from the client
	 * @param response The response to send to the client
	 * @return If the request has a valid security context
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	protected boolean validateSecureRequest(HttpServletRequest request, HttpServletResponse response) throws TransformerException {
		String cookie = request.getHeader("Cookie");
		
		if (!this.getAuthenticationEnabled()) {
			buildException("Authentication undefined in test run properties", response);
			return false;
		} else if (cookie == null) {
			// If the cookie is missing, then redirect to IdP
			
			String idpUrl = this.options.getIdpUrl() + "?RelayState=" + this.relayState + "&SAMLRequest=" 
					+ this.buildSamlAuthRequest(getUri(request, true));
			
			response.setStatus(HttpServletResponse.SC_FOUND);
			response.setHeader("Location", idpUrl);
			return false;
		} else if (cookie.contains("sessionToken=")) {
			// Cookie is valid, do nothing
			return true;
		} else {
			// Cookie is malformed, return service exception
			buildException("Cookie is missing sessionToken", response);
			return false;
		}
	}

}
