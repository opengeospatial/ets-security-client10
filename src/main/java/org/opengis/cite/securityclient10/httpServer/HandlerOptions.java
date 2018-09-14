package org.opengis.cite.securityclient10.httpServer;

import org.opengis.cite.servlet.http.HttpServletRequest;

/**
 * A class that encapsulates the options from the test runner, which are used to determine the type and
 * state of the servlet handler created for capturing the secure client requests
 * 
 * @author jpbadger
 *
 */
public class HandlerOptions {
	/**
	 * Has this handler received a request from the client?
	 * The Test Server will monitor this variable and close the servlet handler when it is true.
	 */
	private Boolean requestReceived;
	
	/**
	 * The type of OGC Web Service being emulated. Also see SuiteAttribute#TEST_SERVICE_TYPE.
	 */
	private String serviceType;
	
	/**
	 * Requests associated with this test session that have been received from the secure client are
	 * stored in this object.
	 */
	private RequestRepresenter requests;
	
	public HandlerOptions(String type) {
		this.serviceType = type;
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
	 * Service type that this handler is emulating.
	 * @return String with service type
	 */
	public String getServiceType() {
		return this.serviceType;
	}
	
	/**
	 * Replace the internal requests list with a new copy that has "request" appended to the end of the
	 * array.
	 * 
	 * @param request Request from client to serialize
	 */
	public void saveRequest(HttpServletRequest request) {
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
