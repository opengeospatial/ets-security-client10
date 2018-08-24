package org.opengis.cite.securityclient10.httpServer;

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
	
	public HandlerOptions(String type) {
		this.serviceType = type;
		this.requestReceived = false;
	}
	
	/**
	 * Value of the current request status
	 * @return
	 */
	public Boolean getReceived() {
		return this.requestReceived;
	}
	
	/**
	 * Service type that this handler is emulating
	 * @return
	 */
	public String getServiceType() {
		return this.serviceType;
	}
	
	/**
	 * Update the value of the `requestReceived` property
	 * @param isReceived
	 */
	public void setReceived(Boolean isReceived) {
		this.requestReceived = isReceived;
	}
}
