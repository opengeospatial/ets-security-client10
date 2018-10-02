package org.opengis.cite.securityclient10.httpServer;

import java.io.IOException;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.opengis.cite.servlet.http.HttpServletRequest;
import org.opengis.cite.servlet.http.HttpServletResponse;
import org.w3c.dom.Document;

public class EmulatedServer {

	private Transformer transformer;

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
	
	/**
	 * Use a Transformer to convert the XML Document to a String.
	 * 
	 * @param document XML document to convert
	 * @return String containing the XML document
	 * @throws TransformerException Exception if transformer could not convert document to stream
	 */
	protected String documentToString(Document document) throws TransformerException {
		StringWriter stringWriter = new StringWriter();
		
		this.transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
		return stringWriter.toString();
	}

}
