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
	
	public HandlerOptions(ServerOptions options) throws TransformerConfigurationException, ParserConfigurationException {
		this.serverOptions = options;
		this.requestReceived = false;
		this.requests = new RequestRepresenter();
	}
	
	/**
	 * Value of the current request status
	 * @return Boolean if this handler has had a request
	 */
	public Boolean getReceived() {
		return this.requestReceived;
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
	 * array.
	 * 
	 * @param request Request from client to serialize
	 * @throws IOException Exception serializing a request to a representer
	 */
	public void saveRequest(HttpServletRequest request) throws IOException {
		requests.serializeRequest(request);
	}
	
	/**
	 * Update the value of the `requestReceived` property
	 * @param isReceived Boolean with state to set this Handler
	 */
	public void setReceived(Boolean isReceived) {
		this.requestReceived = isReceived;
	}
}
