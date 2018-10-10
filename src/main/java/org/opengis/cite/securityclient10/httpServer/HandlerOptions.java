package org.opengis.cite.securityclient10.httpServer;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;

import org.opengis.cite.servlet.http.HttpServletRequest;

/**
 * A class that encapsulates the options from the test runner, which are used to determine the options and
 * state of the servlet handler created for capturing the secure client requests
 *
 */
public class HandlerOptions {
	/**
	 * Has this handler received a request from the client?
	 * The Test Server will monitor this variable and close the servlet handler when it is true.
	 */
	private Boolean requestReceived;
	
	/**
	 * The options for configuring the service type to emulate.
	 */
	private ServerOptions serverOptions;
	
	/**
	 * Requests associated with this test session that have been received from the secure client are
	 * stored in this object.
	 */
	private RequestRepresenter requests;
	
	/**
	 * The number of requests saved for this handler. The type of test will affect how many are saved,
	 * e.g. SAML2 will have 5 requests.
	 */
	private int requestCount;
	
	public HandlerOptions(ServerOptions options) throws TransformerConfigurationException, ParserConfigurationException {
		this.serverOptions = options;
		this.requestReceived = false;
		this.requests = new RequestRepresenter();
		this.requestCount = 0;
	}
	
	/**
	 * Value of the current request status
	 * @return Boolean if this handler has had a request
	 */
	public Boolean getReceived() {
		return this.requestReceived;
	}
	
	/**
	 * The number of requests saved for this handler.
	 * @return int
	 */
	public int getRequestCount() {
		return this.requestCount;
	}
	
	/**
	 * Requests that have been received for this handler. May be empty.
	 * @return RequestRepresenter for the zero or more requests in the Handler
	 */
	public RequestRepresenter getRequests() {
		return this.requests;
	}
	
	/**
	 * Options for the service type being emulated.
	 * @return ServerOptions object.
	 */
	public ServerOptions getServerOptions() {
		return this.serverOptions;
	}
	
	/**
	 * Replace the internal requests list with a new copy that has "request" appended to the end of the
	 * array. The internal request counter will be incremented by one.
	 * 
	 * @param request Request from client to serialize
	 * @throws IOException Exception serializing a request to a representer
	 */
	public void saveRequest(HttpServletRequest request) throws IOException {
		requests.serializeRequest(request);
		this.requestCount += 1;
	}
	
	/**
	 * Update the value of the `requestReceived` property
	 * @param isReceived Boolean with state to set this Handler
	 */
	public void setReceived(Boolean isReceived) {
		this.requestReceived = isReceived;
	}
}
