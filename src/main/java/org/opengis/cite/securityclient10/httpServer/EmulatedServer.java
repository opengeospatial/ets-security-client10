package org.opengis.cite.securityclient10.httpServer;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.opengis.cite.servlet.http.HttpServletRequest;
import org.opengis.cite.servlet.http.HttpServletResponse;

public class EmulatedServer {

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
