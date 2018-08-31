package org.opengis.cite.securityclient10.httpServer;

import javax.servlet.http.HttpServletRequest;

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
	 * Requests associated with this test session that have been received from the secure client.
	 */
	private HttpServletRequest[] requests;
	
	public HandlerOptions(String type) {
		this.serviceType = type;
		this.requestReceived = false;
		this.requests = new HttpServletRequest[0];
	}
	
	/**
	 * Value of the current request status
	 * @return
	 */
	public Boolean getReceived() {
		return this.requestReceived;
	}
	
	/**
	 * Requests that have been received for this handler. May be empty.
	 * @return
	 */
	public HttpServletRequest[] getRequests() {
		return this.requests;
	}
	
	/**
	 * Service type that this handler is emulating
	 * @return
	 */
	public String getServiceType() {
		return this.serviceType;
	}
	
	/**
	 * Replace the internal requests list with a new copy that has "request" appended to the end of the
	 * array.
	 * 
	 * @param request
	 */
	public void saveRequest(HttpServletRequest request) {
		int newLength = this.requests.length + 1;
		HttpServletRequest[] oldRequests = this.requests;
		this.requests = new HttpServletRequest[newLength];
		System.arraycopy(oldRequests, 0, this.requests, 0, oldRequests.length);
		this.requests[newLength - 1] = request;
	}
	
	/**
	 * Update the value of the `requestReceived` property
	 * @param isReceived
	 */
	public void setReceived(Boolean isReceived) {
		this.requestReceived = isReceived;
	}
}
